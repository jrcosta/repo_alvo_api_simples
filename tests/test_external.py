from fastapi.testclient import TestClient

from app.main import app
from app.schemas import AgeEstimateResponse

client = TestClient(app)


def test_get_user_age_estimate_monkeypatched(monkeypatch) -> None:
    # Prepare a fake response from the external service
    fake = AgeEstimateResponse(name="Ana Silva", age=30, count=100)

    # Patch the external_service.estimate_age used by the route
    monkeypatch.setattr(
        "app.api.routes.external_service.estimate_age",
        lambda name: fake,
    )

    response = client.get("/users/1/age-estimate")

    assert response.status_code == 200
    body = response.json()
    assert body["name"] == fake.name
    assert body["age"] == fake.age
    assert body["count"] == fake.count


def test_get_user_age_estimate_handles_null_age(monkeypatch) -> None:
    # Simulate external service returning null/unknown age
    fake = AgeEstimateResponse(name="Ana Silva", age=None, count=None)

    monkeypatch.setattr(
        "app.api.routes.external_service.estimate_age",
        lambda name: fake,
    )

    response = client.get("/users/1/age-estimate")

    assert response.status_code == 200
    body = response.json()
    assert body["name"] == fake.name
    assert body["age"] is None
    assert body["count"] is None
