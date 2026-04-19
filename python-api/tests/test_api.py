from fastapi.testclient import TestClient
from app.main import app

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