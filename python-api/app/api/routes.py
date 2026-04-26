from fastapi import APIRouter, HTTPException, status, Query

from app.services.external_service import ExternalService
from app.services.user_service import UserService
from app.services.discount_service import DiscountService
from app.services.cart_service import CartService
from app.schemas import (
    HealthResponse,
    UserCreate,
    UserResponse,
    CountResponse,
    EmailResponse,
    AgeEstimateResponse,
    EmailDomainCountResponse,
    DiscountRequest,
    DiscountResponse,
    CartRequest,
    CartResponse,
)

router = APIRouter()
user_service = UserService()
external_service = ExternalService()
discount_service = DiscountService()
cart_service = CartService(discount_service=discount_service)


@router.get("/health", response_model=HealthResponse, tags=["health"])
def healthcheck() -> HealthResponse:
    return HealthResponse(status="ok")


@router.get("/users", response_model=list[UserResponse], tags=["users"])
def list_users(limit: int = Query(100, ge=1, description="Maximum number of users to return"),
               offset: int = Query(0, ge=0, description="Number of users to skip")) -> list[UserResponse]:
    """List users with simple pagination (limit/offset).

    This endpoint accepts `limit` and `offset` query parameters and returns a
    slice of the user list. It delegates slicing to the service layer.
    """
    return user_service.list_users(limit=limit, offset=offset)


@router.get("/users/count", response_model=CountResponse, tags=["users"])
def users_count() -> CountResponse:
    """Return the total number of seeded/created users."""
    return CountResponse(count=len(user_service.list_users()))



@router.post("/users", response_model=UserResponse, status_code=status.HTTP_201_CREATED, tags=["users"])
def create_user(payload: UserCreate) -> UserResponse:
    existing_user = user_service.find_by_email(payload.email)

    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="E-mail já cadastrado",
        )

    return user_service.create_user(payload)


@router.get("/users/count", response_model=CountResponse, tags=["users"])
def users_count() -> CountResponse:
    """Return the total number of seeded/created users."""
    return CountResponse(count=len(user_service.list_users()))


@router.get("/users/first-email", response_model=EmailResponse, tags=["users"])
def first_user_email() -> EmailResponse:
    """Retorna apenas o campo de email do primeiro usuário cadastrado."""
    users = user_service.list_users()
    if not users:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Nenhum usuário encontrado",
        )
    return EmailResponse(email=users[0].email)


@router.get("/users/broken", response_model=CountResponse, tags=["users"])
def users_broken() -> CountResponse:
    users = user_service.list_users()
    return {"total": len(users)}


@router.get("/users/{user_id}/email", response_model=EmailResponse, tags=["users"])
def get_user_email(user_id: int) -> EmailResponse:
    """Return only the email for a given user id."""
    user = user_service.get_user(user_id)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Usuário não encontrado",
        )

    return EmailResponse(email=user.email)


@router.get("/users/by-email", response_model=UserResponse, tags=["users"])
def get_user_by_email(email: str) -> UserResponse:
    """Find a user by their exact email address."""
    user = user_service.find_by_email(email)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Usuário não encontrado",
        )

    return user


@router.get("/users/search", response_model=list[UserResponse], tags=["users"])
def search_users(q: str) -> list[UserResponse]:
    results: list[UserResponse] = []
    vip_only = q.lower().startswith("vip:")
    term = q[4:] if vip_only else q

    for u in user_service.list_users():
        if vip_only and not u.is_vip:
            continue

        if term.lower() in u.name.lower():
            # Return the full UserResponse objects so the response_model is satisfied
            results.append(u)

    return results


@router.get("/users/duplicates", response_model=list[UserResponse], tags=["users"])
def find_duplicate_users() -> list[UserResponse]:
    """Return users whose email appears more than once in the system."""
    from collections import Counter

    all_users = user_service.list_users()
    email_counts = Counter(user.email for user in all_users)
    duplicated_emails = {email for email, count in email_counts.items() if count > 1}

    return [user for user in all_users if user.email in duplicated_emails]


@router.get("/users/email-domains", response_model=list[EmailDomainCountResponse], tags=["users"])
def users_email_domains() -> list[EmailDomainCountResponse]:
    """Return a summary of users grouped by email domain."""
    from collections import Counter

    domain_counts = Counter(user.email.split("@")[-1].lower() for user in user_service.list_users())
    return [
        EmailDomainCountResponse(domain=domain, count=count)
        for domain, count in sorted(domain_counts.items())
    ]


@router.get("/users/{user_id}/age-estimate", response_model=AgeEstimateResponse, tags=["external"])
def get_user_age_estimate(user_id: int) -> AgeEstimateResponse:
    user = user_service.get_user(user_id)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Usuário não encontrado",
        )

    # Use the public agify.io API to estimate age by name
    return external_service.estimate_age(user.name)



@router.get("/users/{user_id}", response_model=UserResponse, tags=["users"])
def get_user(user_id: int) -> UserResponse:
    user = user_service.get_user(user_id)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Usuário não encontrado",
        )

    return user


@router.post("/discounts/calculate", response_model=DiscountResponse, tags=["discounts"])
def calculate_discount(payload: DiscountRequest) -> DiscountResponse:
    """Calcula o desconto final para uma compra."""
    try:
        final_price = discount_service.calculate_final_price(
            base_price=payload.base_price,
            discount_percentage=payload.discount_percentage,
            coupon_code=payload.coupon_code,
            is_vip=payload.is_vip
        )
        return DiscountResponse(final_price=final_price)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )


@router.post("/cart/calculate", response_model=CartResponse, tags=["cart"])
def calculate_cart(payload: CartRequest) -> CartResponse:
    """Calcula o fechamento do carrinho de compras."""
    try:
        # Converte Pydantic models para dicts para o serviço
        items_dict = [item.model_dump() for item in payload.items]
        result = cart_service.calculate_cart_total(
            items=items_dict,
            coupon_code=payload.coupon_code,
            is_vip=payload.is_vip
        )
        return CartResponse(**result)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )
