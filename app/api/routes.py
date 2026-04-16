from fastapi import APIRouter, HTTPException, status

from app.schemas import HealthResponse, UserCreate, UserResponse, CountResponse
from app.services.user_service import UserService

router = APIRouter()
user_service = UserService()


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
