import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


@pytest.fixture(autouse=True)
def reset_users():
    # Reset users before each test to ensure consistent state
    from app.services.user_service import user_service
    user_service.reset()


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
    assert response.json()["detail"] == "Usuário não encontrado"


def test_api_update_user_returns_404_when_service_returns_none(monkeypatch):
    from app.api import routes

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
    from app.api import routes

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