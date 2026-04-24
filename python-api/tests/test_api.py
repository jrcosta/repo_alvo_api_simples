import pytest
from fastapi.testclient import TestClient
from app.main import app
from unittest.mock import patch

client = TestClient(app)


def test_create_user_handles_empty_name_and_email() -> None:
    """Test that Pydantic validation rejects empty name and email with 422."""
    response = client.post("/users", json={"name": "", "email": ""})
    assert response.status_code == 422


def test_create_user_handles_invalid_email_format() -> None:
    """Test that Pydantic validation rejects an invalid email format with 422."""
    response = client.post("/users", json={"name": "Test User", "email": "invalid-email"})
    assert response.status_code == 422


def test_check_health_returns_correct_status() -> None:
    """Test that checkHealth returns the correct status and data."""
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_search_users_handles_empty_query() -> None:
    """Test that an empty search query returns all users (empty string matches any name)."""
    response = client.get("/users/search?q=")
    assert response.status_code == 200
    results = response.json()
    assert isinstance(results, list)
    assert len(results) > 0


def test_search_users_returns_matching_results() -> None:
    """Test that searchUsers returns matching results for a known query."""
    response = client.get("/users/search?q=Ana")
    assert response.status_code == 200
    results = response.json()
    assert isinstance(results, list)
    assert any(user["name"] == "Ana Silva" for user in results)  # Assuming "Ana Silva" is a seeded user


def test_create_user_duplicate_email_returns_409() -> None:
    """Test that creating a user with a duplicate email returns a 409 status."""
    # First user creation
    response = client.post("/users", json={"name": "User One", "email": "duplicate@example.com"})
    assert response.status_code == 201

    # Attempt to create a second user with the same email
    response = client.post("/users", json={"name": "User Two", "email": "duplicate@example.com"})
    assert response.status_code == 409


def test_create_user_unique_email_returns_201() -> None:
    """Test that creating a user with a unique email returns a 201 status."""
    response = client.post("/users", json={"name": "Unique User", "email": "unique@example.com"})
    assert response.status_code == 201


def test_get_user_by_email_success() -> None:
    """Test that GET /users/by-email returns the correct user when found."""
    # "ana@example.com" is a seeded user in UserService
    response = client.get("/users/by-email?email=ana@example.com")
    assert response.status_code == 200
    data = response.json()
    assert data["email"] == "ana@example.com"
    assert data["name"] == "Ana Silva"
    # Ensure no sensitive data like password is included
    assert "password" not in data


def test_get_user_by_email_not_found() -> None:
    """Test that GET /users/by-email returns 404 when the email is not found."""
    response = client.get("/users/by-email?email=nonexistent@example.com")
    assert response.status_code == 404
    assert response.json()["detail"] == "Usuário não encontrado"


def test_get_user_by_email_missing_email_param_returns_400() -> None:
    """Test that GET /users/by-email without email parameter returns 400 Bad Request."""
    response = client.get("/users/by-email")
    assert response.status_code == 400
    # Optionally check error message if defined
    json_data = response.json()
    assert "detail" in json_data


def test_get_user_by_email_empty_email_param_returns_404_or_error() -> None:
    """Test that GET /users/by-email with empty email parameter returns 404 or specific error."""
    response = client.get("/users/by-email?email=")
    # Accept either 404 or 422 depending on implementation
    assert response.status_code in (404, 422)
    json_data = response.json()
    assert "detail" in json_data


def test_get_user_by_email_invalid_email_format_returns_422() -> None:
    """Test that GET /users/by-email with invalid email format returns 422 Unprocessable Entity."""
    response = client.get("/users/by-email?email=invalid-email")
    assert response.status_code == 422
    json_data = response.json()
    assert "detail" in json_data


def test_get_user_by_email_does_not_return_sensitive_data() -> None:
    """Test that the response from /users/by-email does not include sensitive fields like password."""
    response = client.get("/users/by-email?email=ana@example.com")
    assert response.status_code == 200
    data = response.json()
    assert "password" not in data
    assert "email" in data
    assert "name" in data


@patch("app.services.user_service.UserService.get_by_email")
def test_get_user_by_email_service_mocked(mock_get_by_email) -> None:
    """Test /users/by-email endpoint with mocked UserService to isolate controller behavior."""
    mock_user = {"id": 123, "name": "Mock User", "email": "mock@example.com"}
    mock_get_by_email.return_value = mock_user

    response = client.get("/users/by-email?email=mock@example.com")
    assert response.status_code == 200
    data = response.json()
    assert data["email"] == "mock@example.com"
    assert data["name"] == "Mock User"
    assert "password" not in data
    mock_get_by_email.assert_called_once_with("mock@example.com")