from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_full_user_lifecycle() -> None:
    """Integration test: exercises the full user lifecycle across multiple endpoints.

    Flow: create user → get by id → get email → search by name → verify count increased.
    """
    # 1. Capture initial count
    r_count_before = client.get("/users/count")
    assert r_count_before.status_code == 200
    count_before = r_count_before.json()["count"]

    # 2. Create a new user
    payload = {"name": "Integration Teste", "email": "integration@example.com"}
    r_create = client.post("/users", json=payload)
    assert r_create.status_code == 201
    created = r_create.json()
    user_id = created["id"]
    assert created["name"] == payload["name"]
    assert created["email"] == payload["email"]

    # 3. Retrieve the user by id
    r_get = client.get(f"/users/{user_id}")
    assert r_get.status_code == 200
    fetched = r_get.json()
    assert fetched["id"] == user_id
    assert fetched["name"] == payload["name"]
    assert fetched["email"] == payload["email"]

    # 4. Retrieve the user's email via dedicated endpoint
    r_email = client.get(f"/users/{user_id}/email")
    assert r_email.status_code == 200
    assert r_email.json()["email"] == payload["email"]

    # 5. Search for the user by name substring
    r_search = client.get("/users/search?q=Integration")
    assert r_search.status_code == 200
    results = r_search.json()
    assert any(u["id"] == user_id for u in results)

    # 6. Verify the count increased by 1
    r_count_after = client.get("/users/count")
    assert r_count_after.status_code == 200
    count_after = r_count_after.json()["count"]
    assert count_after == count_before + 1

    # 7. Verify the user appears in the full list (with pagination)
    r_list = client.get(f"/users?limit=100&offset=0")
    assert r_list.status_code == 200
    all_ids = [u["id"] for u in r_list.json()]
    assert user_id in all_ids

    # 8. Verify duplicate detection does NOT flag this user (unique email)
    r_dups = client.get("/users/duplicates")
    assert r_dups.status_code == 200
    dup_emails = [u["email"] for u in r_dups.json()]
    assert payload["email"] not in dup_emails


def test_duplicate_email_rejection_flow() -> None:
    """Integration test: ensures the full duplicate-email rejection flow works.

    Flow: create user → attempt duplicate → verify 409 → confirm count unchanged.
    """
    # 1. Capture count
    r_count = client.get("/users/count")
    count_before = r_count.json()["count"]

    # 2. Create a user
    payload = {"name": "Unico User", "email": "unico_integ@example.com"}
    r_create = client.post("/users", json=payload)
    assert r_create.status_code == 201

    # 3. Attempt to create another user with the same email
    duplicate_payload = {"name": "Outro Nome", "email": "unico_integ@example.com"}
    r_dup = client.post("/users", json=duplicate_payload)
    assert r_dup.status_code == 409
    assert r_dup.json()["detail"] == "E-mail já cadastrado"

    # 4. Count should have increased by exactly 1 (not 2)
    r_count_after = client.get("/users/count")
    assert r_count_after.json()["count"] == count_before + 1
