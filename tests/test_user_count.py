import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_user_count_functionality() -> None:
    """Test the user count functionality."""
    # Create a user
    payload = {"name": "Count Test User", "email": "count_test@example.com"}
    client.post("/users", json=payload)

    # Check the count
    response = client.get("/users/count")
    assert response.status_code == 200
    assert response.json()["count"] == 1

    # Create another user
    payload2 = {"name": "Another Count Test User", "email": "another_count_test@example.com"}
    client.post("/users", json=payload2)

    # Check the count again
    response = client.get("/users/count")
    assert response.status_code == 200
    assert response.json()["count"] == 2

    # Attempt to create a user with a duplicate email
    duplicate_payload = {"name": "Duplicate Count User", "email": "count_test@example.com"}
    client.post("/users", json=duplicate_payload)

    # Check the count should still be 2
    response = client.get("/users/count")
    assert response.status_code == 200
    assert response.json()["count"] == 2