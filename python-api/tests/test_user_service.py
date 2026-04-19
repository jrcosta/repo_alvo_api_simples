import pytest
from app.services.user_service import UserService
from app.schemas import UserCreate, UserResponse

@pytest.fixture
def user_service():
    service = UserService()
    return service

def test_user_service_reset(user_service) -> None:
    user_service.create_user(UserCreate(name="Test User", email="test@example.com"))
    assert len(user_service.list_users()) == 3  # Deve ter 3 usuários após a criação

    user_service.reset()
    assert len(user_service.list_users()) == 2  # Deve ter 2 usuários após o reset
    assert user_service._next_id == 3  # O próximo ID deve ser 3

def test_user_service_create_user_after_reset(user_service) -> None:
    user_service.reset()
    new_user = user_service.create_user(UserCreate(name="New User", email="newuser@example.com"))
    assert new_user.id == 3  # O ID atribuído deve ser 3 após o reset
    assert len(user_service.list_users()) == 3  # Deve ter 3 usuários após a criação