from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_healthcheck_returns_ok() -> None:
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_list_users_returns_seeded_users() -> None:
    response = client.get("/users")

    assert response.status_code == 200
    assert len(response.json()) >= 2


def test_list_users_pagination_limit_offset() -> None:
    # Request only 1 user
    r1 = client.get("/users?limit=1&offset=0")
    assert r1.status_code == 200
    body1 = r1.json()
    assert len(body1) == 1

    # Request the second user using offset
    r2 = client.get("/users?limit=1&offset=1")
    assert r2.status_code == 200
    body2 = r2.json()
    assert len(body2) == 1
    assert body2[0]["id"] == 2


def test_users_count_returns_number() -> None:
    response = client.get("/users/count")

    assert response.status_code == 200
    body = response.json()
    assert "count" in body
    assert isinstance(body["count"], int)
    assert body["count"] >= 2


def test_users_count_route_not_captured_by_id() -> None:
    """Defensive test: ensure a static route '/users/count' is not matched by '/users/{user_id}'.

    This prevents regressions where route order causes 'count' to be parsed as an int
    and results in a 422 Unprocessable Entity.
    """
    response = client.get("/users/count")

    assert response.status_code == 200
    data = response.json()
    assert isinstance(data.get("count"), int)



def test_create_user_returns_201() -> None:
    payload = {
        "name": "Carlos Souza",
        "email": "carlos@example.com",
    }

    response = client.post("/users", json=payload)

    assert response.status_code == 201
    body = response.json()
    assert body["name"] == payload["name"]
    assert body["email"] == payload["email"]
    assert "id" in body


def test_create_user_duplicate_email_returns_409() -> None:
    payload = {
        "name": "Outra Ana",
        "email": "ana@example.com",
    }

    response = client.post("/users", json=payload)

    assert response.status_code == 409
    assert response.json()["detail"] == "E-mail já cadastrado"


def test_get_user_email_returns_email() -> None:
    response = client.get("/users/1/email")

    assert response.status_code == 200
    body = response.json()
    assert "email" in body
    assert isinstance(body["email"], str)
    assert body["email"] == "ana@example.com"
