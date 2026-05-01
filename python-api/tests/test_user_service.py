import pytest
import threading
from app.services.user_service import UserService
from app.schemas import UserCreate, UserUpdate, UserResponse


@pytest.fixture
def user_service():
    service = UserService()
    service.reset()
    return service


def test_delete_existing_user_removes_user_and_returns_true(user_service: UserService):
    user_id = 1
    assert user_service.get_user(user_id) is not None
    result = user_service.delete_user(user_id)
    assert result is True
    assert user_service.get_user(user_id) is None


def test_delete_nonexistent_user_returns_false_and_list_unchanged(user_service: UserService):
    initial_users = user_service.list_users()
    result = user_service.delete_user(9999)
    assert result is False
    assert user_service.list_users() == initial_users


def test_delete_user_with_empty_list_returns_false(user_service: UserService):
    user_service._users.clear()
    result = user_service.delete_user(1)
    assert result is False
    assert user_service._users == []


def test_multiple_sequential_deletions_maintain_list_integrity(user_service: UserService):
    user_service.reset()
    user_ids = [1, 2]
    for uid in user_ids:
        assert user_service.delete_user(uid) is True
    assert user_service.list_users() == []
    # Deleting again returns False
    for uid in user_ids:
        assert user_service.delete_user(uid) is False


@pytest.mark.parametrize("invalid_id", ["abc", -1, 0, 1.5, None, object()])
def test_delete_user_with_invalid_id_types_returns_false(user_service: UserService, invalid_id):
    # The method expects int, but no explicit validation, so test behavior
    # It should not raise, but return False because no user matches
    try:
        result = user_service.delete_user(invalid_id)  # type: ignore
        assert result is False
    except Exception:
        pytest.fail(f"delete_user raised exception with invalid id: {invalid_id}")


def test_reset_restores_initial_state_after_deletions(user_service: UserService):
    user_service.delete_user(1)
    user_service.delete_user(2)
    assert user_service.list_users() == []
    user_service.reset()
    users = user_service.list_users()
    assert len(users) == 2
    assert any(u.id == 1 for u in users)
    assert any(u.id == 2 for u in users)


def test_delete_user_with_duplicate_ids_removes_first_occurrence_only(user_service: UserService):
    # Add duplicate user with same id as existing user
    duplicate_user = UserResponse(
        id=1,
        name="Duplicate Ana",
        email="ana.dup@example.com",
        is_vip=False,
        status="inactive",
        role="user",
        phone_number="+55 11 90000-9999",
    )
    user_service._users.append(duplicate_user)
    initial_count = len(user_service._users)
    result = user_service.delete_user(1)
    assert result is True
    # Only one user with id=1 removed
    remaining_ids = [u.id for u in user_service._users]
    assert remaining_ids.count(1) == 1
    assert len(user_service._users) == initial_count - 1


def test_delete_user_does_not_alter_list_on_internal_exception(monkeypatch, user_service: UserService):
    # Simulate exception during pop by patching list.pop to raise
    original_pop = user_service._users.pop

    def raise_exception(index):
        raise RuntimeError("Simulated internal error")

    user_service._users.append(
        UserResponse(
            id=999,
            name="Test User",
            email="test@example.com",
            is_vip=False,
            status="active",
            role="user",
            phone_number="+55 11 90000-0000",
        )
    )
    monkeypatch.setattr(user_service._users, "pop", raise_exception)
    initial_users = user_service._users.copy()
    with pytest.raises(RuntimeError):
        user_service.delete_user(999)
    # List should remain unchanged
    assert user_service._users == initial_users
    # Restore original pop method
    monkeypatch.setattr(user_service._users, "pop", original_pop)


def test_delete_user_thread_safety_simulation(user_service: UserService):
    # This test simulates concurrent deletions to check for race conditions
    user_service.reset()
    user_id = 1
    results = []

    def delete_user():
        result = user_service.delete_user(user_id)
        results.append(result)

    threads = [threading.Thread(target=delete_user) for _ in range(10)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()

    # Exactly one thread should succeed, others should fail
    assert results.count(True) == 1
    assert results.count(False) == 9
    # User should no longer exist
    assert user_service.get_user(user_id) is None


def test_delete_user_integration_with_update_and_count(user_service: UserService):
    user_service.reset()
    initial_count = len(user_service.list_users())
    user_id = 1
    # Delete user
    assert user_service.delete_user(user_id) is True
    # Count should decrease by 1
    assert len(user_service.list_users()) == initial_count - 1
    # Update user that was deleted should return None
    update_payload = UserUpdate(name="New Name")
    assert user_service.update_user(user_id, update_payload) is None


def test_delete_user_idempotency(user_service: UserService):
    user_service.reset()
    user_id = 2
    # First deletion succeeds
    assert user_service.delete_user(user_id) is True
    # Second deletion returns False
    assert user_service.delete_user(user_id) is False


def test_delete_user_with_non_integer_id_does_not_raise(user_service: UserService):
    # Passing float, None, string, object should not raise but return False
    invalid_ids = [3.14, None, "string", object()]
    for invalid_id in invalid_ids:
        try:
            result = user_service.delete_user(invalid_id)  # type: ignore
            assert result is False
        except Exception:
            pytest.fail(f"delete_user raised exception with invalid id: {invalid_id}")