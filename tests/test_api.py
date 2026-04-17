from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_create_user_handles_empty_name_and_email() -> None:
    """Test that createUser handles empty name and email inputs."""
    response = client.post("/users", json={"name": "", "email": ""})
    assert response.status_code == 400  # Assuming the API returns 400 for bad requests
    assert response.json() == {"error": "Preencha nome e email"}

def test_create_user_handles_invalid_email_format() -> None:
    """Test that createUser handles invalid email format."""
    response = client.post("/users", json={"name": "Test User", "email": "invalid-email"})
    assert response.status_code == 400  # Assuming the API returns 400 for bad requests
    assert response.json() == {"error": "Email inválido"}  # Assuming this is the expected error message

def test_check_health_returns_correct_status() -> None:
    """Test that checkHealth returns the correct status and data."""
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}

def test_search_users_handles_empty_query() -> None:
    """Test that searchUsers handles empty search queries."""
    response = client.get("/users/search?q=")
    assert response.status_code == 200  # Assuming it returns 200 with no results
    assert response.json() == []  # Assuming an empty list is returned for no results

def test_search_users_returns_matching_results() -> None:
    """Test that searchUsers returns matching results for a known query."""
    response = client.get("/users/search?q=Ana")
    assert response.status_code == 200
    results = response.json()
    assert isinstance(results, list)
    assert any(user["name"] == "Ana Silva" for user in results)  # Assuming "Ana Silva" is a seeded user