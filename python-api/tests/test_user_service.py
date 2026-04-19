import pytest
from app.services.user_service import UserService
from app.schemas import UserCreate, UserResponse


@pytest.fixture
def user_service():
    service = UserService()
    return service


def test_user_response_equality() -> None:
    user1 = UserResponse(id=1, name="Ana Silva", email="ana@example.com")
    user2 = UserResponse(id=1, name="Ana Silva", email="ana@example.com")
    user3 = UserResponse(id=2, name="Bruno Lima", email="bruno@example.com")
    user4 = UserResponse(id=1, name="Ana Silva", email="ana2@example.com")

    assert user1 == user2, "UserResponse instances with same data should be equal"
    assert user1 != user3, "UserResponse instances with different id should not be equal"
    assert user1 != user4, "UserResponse instances with different email should not be equal"


def test_user_service_create_user_with_duplicate_email_after_reset_raises_error(user_service) -> None:
    user_service.reset()
    # Create a user with a new email - should succeed
    user_service.create_user(UserCreate(name="Unique User", email="unique@example.com"))

    # Attempt to create another user with the same email - should raise ValueError
    with pytest.raises(ValueError):
        user_service.create_user(UserCreate(name="Duplicate User", email="unique@example.com"))

    # Attempt to create user with seed email - should raise ValueError
    with pytest.raises(ValueError):
        user_service.create_user(UserCreate(name="Ana Silva Clone", email="ana@example.com"))


def test_user_service_reset_restores_seed_after_partial_removal(user_service) -> None:
    user_service.reset()
    # Remove one seed user manually
    user_service._users = [u for u in user_service._users if u.email != "ana@example.com"]
    user_service._next_id = max(u.id for u in user_service._users) + 1 if user_service._users else 1

    # Confirm partial removal
    assert len(user_service._users) == 1
    assert all(u.email != "ana@example.com" for u in user_service._users)

    # Call reset and verify seed users restored exactly
    user_service.reset()
    users_after_reset = user_service.list_users()
    expected_seed_users = [
        UserResponse(id=1, name="Ana Silva", email="ana@example.com"),
        UserResponse(id=2, name="Bruno Lima", email="bruno@example.com"),
    ]
    assert len(users_after_reset) == 2
    for expected_user in expected_seed_users:
        assert any(
            u.id == expected_user.id and u.name == expected_user.name and u.email == expected_user.email
            for u in users_after_reset
        ), f"Usuário seed esperado não encontrado após reset parcial: {expected_user}"
    assert user_service._next_id == 3


def test_user_service_reset_does_not_alter_unrelated_attributes(user_service) -> None:
    # Setup: add a dummy attribute to simulate other internal state
    user_service._dummy_attr = "unchanged"
    user_service.reset()
    # Check that _users and _next_id are reset as expected
    assert len(user_service._users) == 2
    assert user_service._next_id == 3
    # Check that _dummy_attr remains unchanged
    assert hasattr(user_service, "_dummy_attr")
    assert user_service._dummy_attr == "unchanged"