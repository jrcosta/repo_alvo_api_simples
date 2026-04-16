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
