import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_create_user_with_valid_data() -> None:
    """Test creating a user with valid data."""
    payload = {"name": "Valid User", "email": "valid@example.com"}
    response = client.post("/users", json=payload)
    assert response.status_code == 201
    created_user = response.json()
    assert created_user["name"] == payload["name"]
    assert created_user["email"] == payload["email"]

def test_create_user_with_duplicate_email() -> None:
    """Test creating a user with a duplicate email."""
    payload = {"name": "Duplicate User", "email": "duplicate@example.com"}
    client.post("/users", json=payload)  # Create the first user
    duplicate_payload = {"name": "Another User", "email": "duplicate@example.com"}
    response = client.post("/users", json=duplicate_payload)
    assert response.status_code == 409
    assert response.json()["detail"] == "E-mail já cadastrado"

def test_user_count_increases_after_creation() -> None:
    """Test that user count increases after creating a new user."""
    initial_count_response = client.get("/users/count")
    initial_count = initial_count_response.json()["count"]

    payload = {"name": "Count User", "email": "count@example.com"}
    client.post("/users", json=payload)

    updated_count_response = client.get("/users/count")
    updated_count = updated_count_response.json()["count"]
    assert updated_count == initial_count + 1

def test_user_count_does_not_increase_on_duplicate() -> None:
    """Test that user count does not increase when creating a duplicate user."""
    initial_count_response = client.get("/users/count")
    initial_count = initial_count_response.json()["count"]

    payload = {"name": "Unique User", "email": "unique@example.com"}
    client.post("/users", json=payload)  # Create the first user

    duplicate_payload = {"name": "Duplicate User", "email": "unique@example.com"}
    client.post("/users", json=duplicate_payload)  # Attempt to create duplicate

    updated_count_response = client.get("/users/count")
    updated_count = updated_count_response.json()["count"]
    assert updated_count == initial_count + 1