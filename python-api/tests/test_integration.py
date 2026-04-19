import pytest
from pathlib import Path
from fastapi.testclient import TestClient
from unittest.mock import patch

from app.main import app
from app.services import user_service

client = TestClient(app)

STATIC_DIR = Path(__file__).resolve().parent.parent / "static"


def test_root_endpoint_integration() -> None:
    response = client.get("/")
    assert response.status_code == 200
    assert "html" in response.headers["content-type"]

    with open(STATIC_DIR / "index.html", encoding="utf-8") as f:
        expected_content = f.read()
    assert expected_content.replace("\r\n", "\n") in response.text.replace("\r\n", "\n")


def test_access_static_file_integration() -> None:
    response = client.get("/static/index.html")
    assert response.status_code == 200
    assert "html" in response.headers["content-type"]


def test_access_nonexistent_static_file_returns_404_integration() -> None:
    response = client.get("/static/nonexistentfile.js")
    assert response.status_code == 404


def test_imports_work_correctly_in_python_api() -> None:
    """Test that modules inside python-api can be imported correctly."""
    try:
        import app.main  # noqa: F401
        import app.schemas  # noqa: F401
        import app.api.routes  # noqa: F401
        import app.services.user_service  # noqa: F401
        import app.services.external_service  # noqa: F401
    except ImportError as e:
        pytest.fail(f"ImportError raised: {e}")


def test_create_user_integration() -> None:
    """Test the full cycle of creating a user."""
    response = client.post("/users", json={"name": "New User", "email": "newuser@example.com"})
    assert response.status_code == 201
    created = response.json()
    assert created["name"] == "New User"
    assert created["email"] == "newuser@example.com"
    assert isinstance(created["id"], int)

    response = client.get("/users")
    assert response.status_code == 200
    users = response.json()
    assert any(user["email"] == "newuser@example.com" for user in users)


def test_create_user_duplicate_email_integration() -> None:
    """Test that creating a user with a duplicate email returns a 409 status."""
    response = client.post("/users", json={"name": "User One", "email": "duplicate@example.com"})
    assert response.status_code == 201

    response = client.post("/users", json={"name": "User Two", "email": "duplicate@example.com"})
    assert response.status_code == 409
    assert response.json().get("detail") == "E-mail já cadastrado"


def test_create_multiple_users_integration() -> None:
    """Test creating multiple users and verify they all appear in the list."""
    emails = ["user1@example.com", "user2@example.com", "user3@example.com"]
    for i, email in enumerate(emails, start=1):
        response = client.post("/users", json={"name": f"User {i}", "email": email})
        assert response.status_code == 201

    response = client.get("/users")
    assert response.status_code == 200
    users = response.json()
    user_emails = [user["email"] for user in users]
    assert all(email in user_emails for email in emails)


@pytest.mark.parametrize(
    "invalid_payload, expected_status, expected_detail",
    [
        ({"name": "", "email": "valid@example.com"}, 422, None),  # empty name
        ({"name": "Valid Name", "email": "invalid-email"}, 422, None),  # invalid email format
        ({"name": "Valid Name"}, 422, None),  # missing email
        ({"email": "valid@example.com"}, 422, None),  # missing name
        ({}, 422, None),  # missing both
    ],
)
def test_create_user_with_invalid_data_returns_422(invalid_payload, expected_status, expected_detail) -> None:
    response = client.post("/users", json=invalid_payload)
    assert response.status_code == expected_status
    if expected_detail:
        assert response.json().get("detail") == expected_detail


def test_user_crud_full_cycle_integration() -> None:
    # Create user
    create_resp = client.post("/users", json={"name": "CRUD User", "email": "cruduser@example.com"})
    assert create_resp.status_code == 201
    created = create_resp.json()
    user_id = created["id"]
    assert isinstance(user_id, int)
    assert created["name"] == "CRUD User"
    assert created["email"] == "cruduser@example.com"

    # Read user by id
    get_resp = client.get(f"/users/{user_id}")
    assert get_resp.status_code == 200
    user = get_resp.json()
    assert user["id"] == user_id
    assert user["name"] == "CRUD User"
    assert user["email"] == "cruduser@example.com"

    # Read user email by id
    email_resp = client.get(f"/users/{user_id}/email")
    assert email_resp.status_code == 200
    assert email_resp.json() == {"email": "cruduser@example.com"}

    # Search user by name
    search_resp = client.get("/users/search", params={"q": "CRUD"})
    assert search_resp.status_code == 200
    search_results = search_resp.json()
    assert any(u["id"] == user_id for u in search_results)

    # Update user
    update_resp = client.put(f"/users/{user_id}", json={"name": "Updated User", "email": "updated@example.com"})
    assert update_resp.status_code == 200
    updated = update_resp.json()
    assert updated["id"] == user_id
    assert updated["name"] == "Updated User"
    assert updated["email"] == "updated@example.com"

    # Confirm update via get
    get_updated_resp = client.get(f"/users/{user_id}")
    assert get_updated_resp.status_code == 200
    user_updated = get_updated_resp.json()
    assert user_updated["name"] == "Updated User"
    assert user_updated["email"] == "updated@example.com"

    # Delete user
    delete_resp = client.delete(f"/users/{user_id}")
    assert delete_resp.status_code == 204

    # Confirm deletion
    get_deleted_resp = client.get(f"/users/{user_id}")
    assert get_deleted_resp.status_code == 404


def test_concurrent_creation_of_users_with_same_email_returns_conflict() -> None:
    # This test simulates concurrent creation by sequential calls but checks for consistent 409 on duplicates
    email = "concurrent@example.com"
    response1 = client.post("/users", json={"name": "User A", "email": email})
    assert response1.status_code == 201

    response2 = client.post("/users", json={"name": "User B", "email": email})
    assert response2.status_code == 409
    assert response2.json().get("detail") == "E-mail já cadastrado"

    response3 = client.post("/users", json={"name": "User C", "email": email})
    assert response3.status_code == 409
    assert response3.json().get("detail") == "E-mail já cadastrado"


def test_list_users_returns_all_created_users_including_new_ones() -> None:
    # Create new users
    new_users = [
        {"name": "List User 1", "email": "listuser1@example.com"},
        {"name": "List User 2", "email": "listuser2@example.com"},
    ]
    for user in new_users:
        resp = client.post("/users", json=user)
        assert resp.status_code == 201

    # List users and check presence
    list_resp = client.get("/users")
    assert list_resp.status_code == 200
    users = list_resp.json()
    emails = [u["email"] for u in users]
    for user in new_users:
        assert user["email"] in emails


# Unit test for user_service.create_user to validate returned object structure
def test_user_service_create_user_returns_valid_user_object() -> None:
    # Clear user_service state to avoid interference
    user_service._users.clear()
    user_service._next_id = 1

    user_data = {"name": "Unit Test User", "email": "unittest@example.com"}
    created_user = user_service.create_user(user_data)

    assert isinstance(created_user, dict) or hasattr(created_user, "__dict__")
    # Support dict or Pydantic model with attribute access
    if isinstance(created_user, dict):
        user_obj = created_user
    else:
        user_obj = created_user.__dict__

    assert "id" in user_obj
    assert isinstance(user_obj["id"], int)
    assert user_obj["id"] > 0

    assert "name" in user_obj
    assert isinstance(user_obj["name"], str)
    assert user_obj["name"] == user_data["name"]
    assert user_obj["name"].strip() != ""

    assert "email" in user_obj
    assert isinstance(user_obj["email"], str)
    assert user_obj["email"] == user_data["email"]
    assert user_obj["email"].strip() != ""


# Unit test for the function that returns the duplicate email error message
def test_user_service_duplicate_email_error_message_consistency() -> None:
    # Assuming user_service has a function or constant for duplicate email message
    # If not, we test the actual error message returned by create_user on duplicate

    # Clear state
    user_service._users.clear()
    user_service._next_id = 1

    user_data = {"name": "User One", "email": "duplicate@example.com"}
    user_service.create_user(user_data)

    with pytest.raises(Exception) as exc_info:
        user_service.create_user(user_data)

    # The exception message or error detail should contain the expected message
    # If user_service raises a specific exception or returns error, adapt accordingly
    # Here we check if the message contains the expected string
    assert "E-mail já cadastrado" in str(exc_info.value)