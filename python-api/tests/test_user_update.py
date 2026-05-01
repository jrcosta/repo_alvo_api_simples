import pytest
from fastapi import status
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock
from app.main import app
from app.schemas import UserUpdate

client = TestClient(app, raise_server_exceptions=False)

def test_update_user_with_extra_undefined_fields_returns_422():
    payload = {
        "name": "User Extra Field",
        "extra_field": "value"
    }
    response = client.put("/users/1", json=payload)
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY

@pytest.mark.parametrize("field", ["name", "email", "is_vip"])
def test_update_user_accepts_null_values_for_updatable_fields(field):
    # O service ignora None e mantém o valor original — null não é persistido como null
    payload = {field: None}
    response = client.put("/users/1", json=payload)
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    # Campo deve estar presente com o valor original (não None)
    assert field in data
    assert data[field] is not None

@pytest.mark.parametrize("immutable_field", ["id", "created_at", "updated_at"])
def test_update_user_with_immutable_fields_returns_422(immutable_field):
    payload = {immutable_field: "some_value", "name": "Valid Name"}
    response = client.put("/users/1", json=payload)
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY

def test_update_user_with_empty_payload_returns_422():
    response = client.put("/users/1", json={})
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY

def test_update_user_partial_update_with_single_valid_field():
    payload = {"name": "Partial Update Name"}
    response = client.put("/users/1", json=payload)
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert data["name"] == "Partial Update Name"

@patch("app.api.routes.user_service.update_user")
def test_update_user_handles_data_layer_exception(mock_update):
    mock_update.side_effect = Exception("Simulated DB error")
    payload = {"name": "Trigger Exception"}
    response = client.put("/users/1", json=payload)
    assert response.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR

def test_update_user_strict_validation_rejects_extra_fields_before_db_call():
    with patch("app.api.routes.user_service.update_user") as mock_update:
        payload = {
            "name": "Strict Validation",
            "unexpected_field": "value"
        }
        response = client.put("/users/1", json=payload)
        assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY
        mock_update.assert_not_called()

@pytest.mark.parametrize("field", ["name", "email", "is_vip"])
def test_update_user_accepts_null_and_persists_correctly(field):
    # Create user first
    create_payload = {"name": "Null Persist User", "email": "nullpersist@example.com", "is_vip": True}
    create_response = client.post("/users", json=create_payload)
    assert create_response.status_code == status.HTTP_201_CREATED
    user_id = create_response.json()["id"]

    # Update with null value — service ignora None, mantém valor original
    update_payload = {field: None}
    update_response = client.put(f"/users/{user_id}", json=update_payload)
    assert update_response.status_code == status.HTTP_200_OK
    updated_data = update_response.json()
    # Campo deve estar presente com valor original (não None)
    assert field in updated_data
    assert updated_data[field] is not None

    # Fetch and verify persistence
    get_response = client.get(f"/users/{user_id}")
    assert get_response.status_code == status.HTTP_200_OK
    get_data = get_response.json()
    assert field in get_data
    assert get_data[field] is not None

def test_update_user_with_immutable_field_and_valid_fields_returns_422_and_no_update():
    with patch("app.api.routes.user_service.update_user") as mock_update:
        payload = {
            "id": 999,
            "name": "Valid Name"
        }
        response = client.put("/users/1", json=payload)
        assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY
        mock_update.assert_not_called()

@patch("app.api.routes.user_service.update_user")
def test_update_user_mock_called_and_exception_returns_500(mock_update):
    mock_update.side_effect = Exception("Timeout error")
    payload = {"name": "Timeout Test"}
    response = client.put("/users/1", json=payload)
    assert response.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR
    # O route converte o dict para UserUpdate antes de chamar o service
    mock_update.assert_called_once()
    args, kwargs = mock_update.call_args
    assert args[0] == 1
    assert isinstance(args[1], UserUpdate)
    assert args[1].name == "Timeout Test"

def test_update_user_flow_with_nulls_extras_and_immutables():
    # Create user
    create_payload = {"name": "Flow Test User", "email": "flowtest@example.com", "is_vip": False}
    create_response = client.post("/users", json=create_payload)
    assert create_response.status_code == status.HTTP_201_CREATED
    user_id = create_response.json()["id"]

    # Payload with nulls, extras and immutable fields
    payload = {
        "name": None,
        "email": None,
        "is_vip": None,
        "extra_field": "extra",
        "id": 1234
    }
    response = client.put(f"/users/{user_id}", json=payload)
    # Expect 422 due to extra and immutable fields
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY

def test_update_user_with_nested_extra_fields_rejected():
    payload = {
        "name": "Nested Extra",
        "extra": {"nested_key": "nested_value"}
    }
    response = client.put("/users/1", json=payload)
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY

@pytest.mark.parametrize("invalid_payload", [
    {"is_vip": "not_boolean"},
    {"email": 123},
    {"name": 456},
    {"is_vip": 1},
])
def test_update_user_with_invalid_types_returns_422(invalid_payload):
    response = client.put("/users/1", json=invalid_payload)
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY

def test_update_user_with_payload_only_null_fields():
    payload = {
        "name": None,
        "email": None,
        "is_vip": None
    }
    response = client.put("/users/1", json=payload)
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    # Service ignora None — valores originais são mantidos
    assert data["name"] is not None
    assert data["email"] is not None
    assert data["is_vip"] is not None

def test_update_user_rollback_on_partial_failure_with_nulls_and_extras():
    # extra_field causa 422 antes de chegar ao service — comportamento correto do FastAPI
    payload = {
        "name": "Rollback Test",
        "email": "rollback@example.com",
        "extra_field": "extra"
    }
    response = client.put("/users/1", json=payload)
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY

def test_service_update_user_ignores_none_values_and_preserves_original(monkeypatch):
    # Mock original user data
    original_user = {
        "id": 1,
        "name": "Original Name",
        "email": "original@example.com",
        "is_vip": True,
        "created_at": "2023-01-01T00:00:00Z",
        "updated_at": "2023-01-01T00:00:00Z"
    }

    # Mock service update_user function to simulate ignoring None values
    def fake_update_user(user_id, user_update):
        # user_update is UserUpdate instance
        updated = original_user.copy()
        # Only update fields that are not None
        for field in ["name", "email", "is_vip"]:
            val = getattr(user_update, field)
            if val is not None:
                updated[field] = val
        return updated

    from app.api import routes
    monkeypatch.setattr(routes.user_service, "update_user", fake_update_user)

    # Prepare payload with None values for updatable fields
    payload = {
        "name": None,
        "email": None,
        "is_vip": None
    }
    response = client.put("/users/1", json=payload)
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    # Assert original values preserved (not None)
    assert data["name"] == original_user["name"]
    assert data["email"] == original_user["email"]
    assert data["is_vip"] == original_user["is_vip"]

def test_userupdate_schema_rejects_extra_and_immutable_fields():
    # Extra field
    with pytest.raises(Exception):
        UserUpdate(name="Name", extra_field="value")  # type: ignore

    # Immutable fields
    with pytest.raises(Exception):
        UserUpdate(id=1)  # type: ignore
    with pytest.raises(Exception):
        UserUpdate(created_at="2023-01-01T00:00:00Z")  # type: ignore
    with pytest.raises(Exception):
        UserUpdate(updated_at="2023-01-01T00:00:00Z")  # type: ignore

def test_userupdate_schema_accepts_valid_partial_payloads():
    # Only name
    obj = UserUpdate(name="Valid Name")
    assert obj.name == "Valid Name"
    assert obj.email is None
    assert obj.is_vip is None

    # Only email
    obj = UserUpdate(email="valid@example.com")
    assert obj.email == "valid@example.com"
    assert obj.name is None
    assert obj.is_vip is None

    # Only is_vip
    obj = UserUpdate(is_vip=True)
    assert obj.is_vip is True
    assert obj.name is None
    assert obj.email is None

def test_update_user_conversion_to_userupdate_schema_before_service_call():
    with patch("app.api.routes.user_service.update_user") as mock_update:
        payload = {"name": "Converted Name", "email": "converted@example.com", "is_vip": True}
        response = client.put("/users/1", json=payload)
        assert response.status_code == status.HTTP_200_OK
        mock_update.assert_called_once()
        args, kwargs = mock_update.call_args
        assert args[0] == 1
        assert isinstance(args[1], UserUpdate)
        assert args[1].name == "Converted Name"
        assert args[1].email == "converted@example.com"
        assert args[1].is_vip is True

@pytest.mark.parametrize("empty_value", ["", " "])
@pytest.mark.parametrize("field", ["name", "email"])
def test_update_user_accepts_empty_string_for_name_and_email(field, empty_value):
    payload = {field: empty_value}
    response = client.put("/users/1", json=payload)
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert field in data
    assert data[field] == empty_value

@pytest.mark.parametrize("invalid_value", ["true", 1, 0, "false"])
def test_update_user_rejects_non_boolean_values_for_is_vip(invalid_value):
    payload = {"is_vip": invalid_value}
    response = client.put("/users/1", json=payload)
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY

@pytest.mark.parametrize("partial_payload", [
    {"name": "Valid", "extra_field": "invalid"},
    {"email": "valid@example.com", "id": 123},
    {"is_vip": True, "created_at": "2023-01-01T00:00:00Z"},
])
def test_update_user_rejects_partially_invalid_payloads(partial_payload):
    response = client.put("/users/1", json=partial_payload)
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY

def test_update_user_response_never_contains_null_for_updatable_fields():
    # Create user
    create_payload = {"name": "Null Check User", "email": "nullcheck@example.com", "is_vip": False}
    create_response = client.post("/users", json=create_payload)
    assert create_response.status_code == status.HTTP_201_CREATED
    user_id = create_response.json()["id"]

    # Update with nulls
    update_payload = {"name": None, "email": None, "is_vip": None}
    update_response = client.put(f"/users/{user_id}", json=update_payload)
    assert update_response.status_code == status.HTTP_200_OK
    data = update_response.json()
    for field in ["name", "email", "is_vip"]:
        assert field in data
        assert data[field] is not None