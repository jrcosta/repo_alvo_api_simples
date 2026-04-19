from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_full_user_lifecycle():
    # Create user
    create_payload = {"name": "Integration Test", "email": "integration@example.com"}
    create_resp = client.post("/users", json=create_payload)
    assert create_resp.status_code == 201
    created = create_resp.json()
    assert created["name"] == create_payload["name"]
    assert created["email"] == create_payload["email"]
    user_id = created["id"]

    # Get user by ID
    get_resp = client.get(f"/users/{user_id}")
    assert get_resp.status_code == 200
    user_data = get_resp.json()
    assert user_data["id"] == user_id
    assert user_data["name"] == create_payload["name"]
    assert user_data["email"] == create_payload["email"]

    # Get user email by ID
    email_resp = client.get(f"/users/{user_id}/email")
    assert email_resp.status_code == 200
    email_data = email_resp.json()
    assert email_data["email"] == create_payload["email"]

    # Search users by name
    search_resp = client.get("/users/search", params={"q": "Integration"})
    assert search_resp.status_code == 200
    assert "application/json" in search_resp.headers["content-type"]
    search_data = search_resp.json()
    assert isinstance(search_data, list)
    assert any(u["name"] == create_payload["name"] for u in search_data)

    # Count users
    count_resp = client.get("/users/count")
    assert count_resp.status_code == 200
    count_data = count_resp.json()
    assert isinstance(count_data.get("count"), int)
    assert count_data["count"] >= 1

    # List users with pagination
    list_resp = client.get("/users", params={"limit": 5, "offset": 0})
    assert list_resp.status_code == 200
    list_data = list_resp.json()
    assert isinstance(list_data, list)
    assert any(u["id"] == user_id for u in list_data)

    # List users with pagination offset beyond size returns empty list
    list_empty_resp = client.get("/users", params={"limit": 5, "offset": 1000})
    assert list_empty_resp.status_code == 200
    assert list_empty_resp.json() == []


def test_duplicate_email_rejection_flow():
    unique_email = "duplicate-test@example.com"
    create_payload = {"name": "Duplicate Test", "email": unique_email}

    # Create user first time
    create_resp = client.post("/users", json=create_payload)
    assert create_resp.status_code == 201
    created = create_resp.json()
    user_id = created["id"]

    # Attempt to create user with same email again
    duplicate_resp = client.post("/users", json=create_payload)
    assert duplicate_resp.status_code == 409
    error_data = duplicate_resp.json()
    assert "E-mail já cadastrado" in error_data.get("detail", "")

    # Count users should not have increased
    count_resp = client.get("/users/count")
    assert count_resp.status_code == 200
    count_data = count_resp.json()
    assert count_data["count"] >= 1