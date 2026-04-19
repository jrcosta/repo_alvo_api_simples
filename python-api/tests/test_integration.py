import pytest
from pathlib import Path
from fastapi.testclient import TestClient

from app.main import app

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
