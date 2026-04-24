import pytest
from pydantic import ValidationError
from fastapi.testclient import TestClient
from app.main import app
from app.schemas import EmailDomainCountResponse

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


def test_email_domain_count_response_creation_with_valid_data():
    response = EmailDomainCountResponse(domain="example.com", count=10)
    assert response.domain == "example.com"
    assert response.count == 10


@pytest.mark.parametrize(
    "invalid_domain",
    [123, 45.6, True, None, [], {}],
)
def test_email_domain_count_response_invalid_domain_type_raises_validation_error(invalid_domain):
    with pytest.raises(ValidationError) as exc_info:
        EmailDomainCountResponse(domain=invalid_domain, count=5)
    errors = exc_info.value.errors()
    assert any(error["loc"] == ("domain",) for error in errors)


@pytest.mark.parametrize(
    "invalid_count",
    # Note: bool (True/False) is a subclass of int in Python and is accepted by
    # Pydantic v2 for int fields (coerced to 0/1), so it is excluded here.
    ["string", 12.34, None, [], {}],
)
def test_email_domain_count_response_invalid_count_type_raises_validation_error(invalid_count):
    with pytest.raises(ValidationError) as exc_info:
        EmailDomainCountResponse(domain="example.com", count=invalid_count)
    errors = exc_info.value.errors()
    assert any(error["loc"] == ("count",) for error in errors)


def test_email_domain_count_response_serialization_and_deserialization():
    original = EmailDomainCountResponse(domain="example.com", count=42)
    json_str = original.model_dump_json()
    # Deserialize back
    deserialized = EmailDomainCountResponse.model_validate_json(json_str)
    assert deserialized == original
    assert deserialized.domain == "example.com"
    assert deserialized.count == 42


def test_get_user_by_email_success() -> None:
    """Test that GET /users/by-email returns the correct user when found."""
    # "ana@example.com" is a seeded user in UserService
    response = client.get("/users/by-email?email=ana@example.com")
    assert response.status_code == 200
    data = response.json()
    assert data["email"] == "ana@example.com"
    assert data["name"] == "Ana Silva"


def test_get_user_by_email_not_found() -> None:
    """Test that GET /users/by-email returns 404 when the email is not found."""
    response = client.get("/users/by-email?email=nonexistent@example.com")
    assert response.status_code == 404
    assert response.json()["detail"] == "Usuário não encontrado"