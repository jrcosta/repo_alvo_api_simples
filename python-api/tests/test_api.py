from app.services.user_service import UserService
from app.schemas import UserCreateRequest
import pytest

@pytest.fixture
def user_service() -> UserService:
    # Create a fresh instance for each test to ensure isolation
    return UserService()

def test_create_user_with_unique_email_generates_id_and_stores_user(user_service: UserService) -> None:
    request = UserCreateRequest(name="Test User", email="unique@example.com")
    created_user = user_service.create(request)

    assert created_user.id > 0
    assert created_user.name == "Test User"
    assert created_user.email == "unique@example.com"

    # Verify user is stored and retrievable by ID
    retrieved = user_service.getById(created_user.id)
    assert retrieved is not None
    assert retrieved.id == created_user.id
    assert retrieved.email == created_user.email

def test_find_by_email_returns_correct_user_after_multiple_creations(user_service: UserService) -> None:
    users_data = [
        {"name": "User A", "email": "a@example.com"},
        {"name": "User B", "email": "b@example.com"},
        {"name": "User C", "email": "c@example.com"},
    ]
    created_users = []
    for data in users_data:
        created = user_service.create(UserCreateRequest(**data))
        created_users.append(created)

    for created in created_users:
        found = user_service.findByEmail(created.email)
        assert found is not None
        assert found.id == created.id
        assert found.email == created.email

def test_create_user_with_duplicate_email_raises_error_or_returns_none(user_service: UserService) -> None:
    request = UserCreateRequest(name="User One", email="dup@example.com")
    first_user = user_service.create(request)
    assert first_user is not None

    # Attempt to create another user with the same email
    with pytest.raises(Exception):
        user_service.create(request)

def test_service_state_isolation_between_tests() -> None:
    service1 = UserService()
    service2 = UserService()

    user1 = service1.create(UserCreateRequest(name="User1", email="user1@example.com"))
    user2 = service2.create(UserCreateRequest(name="User2", email="user2@example.com"))

    # Each service instance should have independent state
    assert service1.findByEmail("user1@example.com") is not None
    assert service1.findByEmail("user2@example.com") is None

    assert service2.findByEmail("user2@example.com") is not None
    assert service2.findByEmail("user1@example.com") is None