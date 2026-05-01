import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.api import routes
from app.services.user_service import user_service

client = TestClient(app)


@pytest.fixture(autouse=True)
def reset_user_service():
    user_service.reset()
    yield
    user_service.reset()


def test_delete_existing_user_via_api_returns_204_and_user_removed():
    user_id = 1
    response = client.delete(f"/users/{user_id}")
    assert response.status_code in (200, 204)
    # Confirm user no longer exists
    get_response = client.get(f"/users/{user_id}")
    assert get_response.status_code == 404


def test_delete_nonexistent_user_via_api_returns_404_with_message():
    response = client.delete("/users/9999")
    assert response.status_code == 404
    assert response.json() == {"detail": "Usuário não encontrado"}


@pytest.mark.parametrize("invalid_id", ["abc", "-1", "0", "1.5", "", " "])
def test_delete_user_via_api_with_invalid_id_returns_422(invalid_id):
    response = client.delete(f"/users/{invalid_id}")
    assert response.status_code == 422


def test_multiple_sequential_deletions_via_api_maintain_consistent_state():
    # Delete user 1
    response1 = client.delete("/users/1")
    assert response1.status_code in (200, 204)
    # Delete user 2
    response2 = client.delete("/users/2")
    assert response2.status_code in (200, 204)
    # List users should be empty
    list_response = client.get("/users")
    assert list_response.status_code == 200
    assert list_response.json() == []


def test_list_users_after_deletion_via_api_reflects_correct_state():
    # Delete user 1
    client.delete("/users/1")
    # List users should not contain user 1
    response = client.get("/users")
    assert response.status_code == 200
    users = response.json()
    assert all(user["id"] != 1 for user in users)


def test_concurrent_deletion_requests_for_same_user_via_api():
    import concurrent.futures

    user_service.reset()
    user_id = 1

    def delete_request():
        return client.delete(f"/users/{user_id}")

    with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
        futures = [executor.submit(delete_request) for _ in range(5)]
        results = [f.result() for f in futures]

    # Exactly one should succeed with 200/204, others 404
    success_count = sum(1 for r in results if r.status_code in (200, 204))
    not_found_count = sum(1 for r in results if r.status_code == 404)
    assert success_count == 1
    assert not_found_count == 4


def test_delete_user_idempotency_via_api():
    user_service.reset()
    user_id = 2
    # First deletion
    response1 = client.delete(f"/users/{user_id}")
    assert response1.status_code in (200, 204)
    # Second deletion returns 404
    response2 = client.delete(f"/users/{user_id}")
    assert response2.status_code == 404


def test_delete_user_with_malicious_id_via_api_returns_422():
    malicious_ids = ["../1", "%2e%2e%2f1", "1; DROP TABLE users;", "<script>"]
    for mid in malicious_ids:
        response = client.delete(f"/users/{mid}")
        assert response.status_code == 422


def test_api_update_user_rejects_empty_payload_with_422_and_clear_error_message():
    user_id = 1
    payload = {}
    response = client.put(f"/users/{user_id}", json=payload)
    assert response.status_code == 422
    json_data = response.json()
    # Pydantic validation error detail should mention missing fields or no data provided
    assert "detail" in json_data
    # Check that error message indicates at least one field must be provided
    error_messages = [err.get("msg", "") for err in json_data["detail"]]
    assert any("at least one field" in msg.lower() or "none provided" in msg.lower() or "value_error" in msg.lower() for msg in error_messages)


def test_api_update_user_rejects_payload_with_all_null_fields_with_422():
    user_id = 1
    payload = {"name": None, "email": None, "is_vip": None}
    response = client.put(f"/users/{user_id}", json=payload)
    assert response.status_code == 422
    json_data = response.json()
    assert "detail" in json_data
    error_messages = [err.get("msg", "") for err in json_data["detail"]]
    assert any("none provided" in msg.lower() or "value_error" in msg.lower() for msg in error_messages)


def test_api_update_user_accepts_payload_with_at_least_one_valid_field():
    user_id = 1
    payload = {"name": "Valid Name", "email": None, "is_vip": None}
    response = client.put(f"/users/{user_id}", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["id"] == user_id
    assert data["name"] == "Valid Name"


def test_api_update_user_returns_404_for_nonexistent_user_with_valid_payload():
    user_id = 9999
    payload = {"name": "Nonexistent User"}
    response = client.put(f"/users/{user_id}", json=payload)
    assert response.status_code == 404
    assert response.json() == {"detail": "Usuário não encontrado"}


def test_api_update_user_returns_404_when_service_returns_none(monkeypatch):
    def fake_update_user(user_id, payload):
        return None

    monkeypatch.setattr(routes.user_service, "update_user", fake_update_user)
    user_id = 1
    payload = {"name": "Any Name"}
    response = client.put(f"/users/{user_id}", json=payload)
    assert response.status_code == 404
    assert response.json()["detail"] == "Usuário não encontrado"


def test_api_update_user_invalid_email_returns_422():
    user_id = 1
    payload = {"email": "invalid-email-format"}
    response = client.put(f"/users/{user_id}", json=payload)
    assert response.status_code == 422
    json_data = response.json()
    assert "detail" in json_data
    error_messages = [err.get("msg", "") for err in json_data["detail"]]
    assert any("value is not a valid email address" in msg.lower() for msg in error_messages)


def test_api_update_user_rejects_payload_with_extra_unexpected_fields():
    user_id = 1
    payload = {"name": "Extra Field User", "unknown_field": "should be ignored or rejected"}
    response = client.put(f"/users/{user_id}", json=payload)
    # Depending on Pydantic config, extra fields may be ignored or cause 422
    # We assert that either 200 or 422 is returned, but if 422, check error message
    assert response.status_code in (200, 422)
    if response.status_code == 422:
        json_data = response.json()
        assert "detail" in json_data
        error_messages = [err.get("msg", "") for err in json_data["detail"]]
        assert any("extra fields" in msg.lower() or "unexpected" in msg.lower() for msg in error_messages)
    else:
        data = response.json()
        assert data["name"] == "Extra Field User"


def test_api_update_user_rejects_payload_with_partially_invalid_fields():
    user_id = 1
    payload = {"name": "Valid Name", "email": "invalid-email-format"}
    response = client.put(f"/users/{user_id}", json=payload)
    assert response.status_code == 422
    json_data = response.json()
    assert "detail" in json_data
    error_messages = [err.get("msg", "") for err in json_data["detail"]]
    assert any("value is not a valid email address" in msg.lower() for msg in error_messages)


def test_api_update_user_returns_422_for_null_values_in_valid_fields():
    user_id = 1
    payload = {"name": None}
    response = client.put(f"/users/{user_id}", json=payload)
    assert response.status_code == 422
    json_data = response.json()
    assert "detail" in json_data


def test_api_update_user_monkeypatch_does_not_affect_other_tests(monkeypatch):
    original_update_user = routes.user_service.update_user

    def fake_update_user(user_id, payload):
        return None

    monkeypatch.setattr(routes.user_service, "update_user", fake_update_user)
    user_id = 1
    payload = {"name": "Test"}
    response = client.put(f"/users/{user_id}", json=payload)
    assert response.status_code == 404

    # Restore original method
    monkeypatch.setattr(routes.user_service, "update_user", original_update_user)

    # Now call again to ensure original method works as expected
    response2 = client.put(f"/users/{user_id}", json={"name": "Restored"})
    assert response2.status_code == 200
    data = response2.json()
    assert data["id"] == user_id
    assert data["name"] == "Restored"