from fastapi import APIRouter, HTTPException, status

from app.schemas import (
    HealthResponse,
    UserCreate,
    UserResponse,
    CountResponse,
    EmailResponse,
    AgeEstimateResponse,
)
from app.services.external_service import ExternalService
from app.services.user_service import UserService

router = APIRouter()
user_service = UserService()
external_service = ExternalService()


@router.get("/health", response_model=HealthResponse, tags=["health"])
def healthcheck() -> HealthResponse:
    return HealthResponse(status="ok")


@router.get("/users", response_model=list[UserResponse], tags=["users"])
def list_users() -> list[UserResponse]:
    return user_service.list_users()


@router.get("/users/{user_id}", response_model=UserResponse, tags=["users"])
def get_user(user_id: int) -> UserResponse:
    user = user_service.get_user(user_id)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Usuário não encontrado",
        )

    return user


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


@router.get("/users/first-email", response_model=UserResponse, tags=["users"])
def first_user_email() -> UserResponse:
    """Deliberately buggy endpoint: claims to return a full UserResponse but only
    returns a dict with a single wrongly-named email field. This will trigger
    Pydantic validation errors at runtime (was intentional for testing).
    This function now returns a proper UserResponse instance.
    """

    users = user_service.list_users()

    if not users:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Nenhum usuário encontrado",
        )

    # Return the actual UserResponse object (fixed)
    return users[0]


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


@router.get("/users/search", response_model=list[UserResponse], tags=["users"])
def search_users(q: str) -> list[UserResponse]:
    results = []
    for u in user_service.list_users():
        if q.lower() in u.name.lower():
            results.append(u.name)

    return results


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
