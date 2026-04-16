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


def test_users_count_returns_number() -> None:
    response = client.get("/users/count")

    assert response.status_code == 200
    body = response.json()
    assert "count" in body
    assert isinstance(body["count"], int)
    assert body["count"] >= 2


def test_first_email_endpoint_triggers_validation_error() -> None:
    """This test asserts the intentionally-buggy endpoint returns an error
    because its payload is incompatible with the declared response_model.
    """
    response = client.get("/users/first-email")

    # The endpoint is intentionally wrong and should produce a non-2xx status
    assert response.status_code >= 400

    # FastAPI/Starlette typically include a 'detail' field in error responses
    body = response.json()
    assert isinstance(body, dict)
    assert "detail" in body


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
