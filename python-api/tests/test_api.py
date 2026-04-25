import pytest
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock
from app.api.routes import router
from app.schemas import DiscountRequest, DiscountResponse
from fastapi import FastAPI, status

app = FastAPI()
app.include_router(router)

client = TestClient(app, raise_server_exceptions=False)


def make_payload(
    base_price=100.0,
    discount_percentage=10.0,
    coupon_code="SAVE10",
    is_vip=False,
):
    return {
        "base_price": base_price,
        "discount_percentage": discount_percentage,
        "coupon_code": coupon_code,
        "is_vip": is_vip,
    }


@patch("app.api.routes.discount_service")
def test_calculate_discount_returns_expected_final_price(mock_discount_service):
    # Arrange
    mock_discount_service.calculate_final_price.return_value = 80.0
    payload = make_payload()

    # Act
    response = client.post("/discounts/calculate", json=payload)

    # Assert
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert "final_price" in data
    assert data["final_price"] == 80.0
    mock_discount_service.calculate_final_price.assert_called_once_with(
        base_price=payload["base_price"],
        discount_percentage=payload["discount_percentage"],
        coupon_code=payload["coupon_code"],
        is_vip=payload["is_vip"],
    )


@patch("app.api.routes.discount_service")
def test_calculate_discount_raises_value_error_returns_400(mock_discount_service):
    # Arrange
    mock_discount_service.calculate_final_price.side_effect = ValueError("Desconto inválido")
    payload = make_payload()

    # Act
    response = client.post("/discounts/calculate", json=payload)

    # Assert
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json() == {"detail": "Desconto inválido"}


@pytest.mark.parametrize(
    "payload,missing_field",
    [
        ({"discount_percentage": 10.0, "coupon_code": "SAVE10", "is_vip": False}, "base_price"),
        # discount_percentage e is_vip têm defaults, então não são obrigatórios
    ],
)
def test_calculate_discount_missing_required_fields(payload, missing_field):
    # coupon_code is optional, so skip that case
    if missing_field == "coupon_code":
        # Should succeed because coupon_code is optional
        response = client.post("/discounts/calculate", json=payload)
        assert response.status_code == status.HTTP_200_OK
        return

    response = client.post("/discounts/calculate", json=payload)
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY
    assert missing_field in response.text


@pytest.mark.parametrize(
    "payload",
    [
        {"base_price": "one hundred", "discount_percentage": 10.0, "coupon_code": "SAVE10", "is_vip": False},
        {"base_price": 100.0, "discount_percentage": "ten", "coupon_code": "SAVE10", "is_vip": False},
        {"base_price": 100.0, "discount_percentage": 10.0, "coupon_code": "SAVE10", "is_vip": []},
        {"base_price": 100.0, "discount_percentage": 10.0, "coupon_code": [], "is_vip": False},
    ],
)
def test_calculate_discount_invalid_field_types(payload):
    response = client.post("/discounts/calculate", json=payload)
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY


@pytest.mark.parametrize(
    "payload",
    [
        {"base_price": 0, "discount_percentage": 0, "coupon_code": "", "is_vip": False},
        {"base_price": -10, "discount_percentage": 0, "coupon_code": None, "is_vip": True},
        {"base_price": 1000000, "discount_percentage": 100, "coupon_code": "VIP100", "is_vip": True},
        {"base_price": 100, "discount_percentage": 101, "coupon_code": "INVALID", "is_vip": False},
        {"base_price": 100, "discount_percentage": -1, "coupon_code": "INVALID", "is_vip": False},
    ],
)
@patch("app.api.routes.discount_service")
def test_calculate_discount_with_edge_values(mock_discount_service, payload):
    if payload["base_price"] < 0:
        # Pydantic valida base_price >= 0 e retorna 422
        response = client.post("/discounts/calculate", json=payload)
        assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY
    elif payload["discount_percentage"] > 100 or payload["discount_percentage"] < 0:
        # Pydantic valida discount_percentage entre 0 e 100 e retorna 422
        response = client.post("/discounts/calculate", json=payload)
        assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY
    else:
        # Return a dummy final price for valid inputs
        mock_discount_service.calculate_final_price.return_value = 50.0
        response = client.post("/discounts/calculate", json=payload)
        assert response.status_code == status.HTTP_200_OK
        data = response.json()
        assert "final_price" in data
        assert isinstance(data["final_price"], (int, float))


@patch("app.api.routes.discount_service")
def test_calculate_discount_without_coupon_code(mock_discount_service):
    mock_discount_service.calculate_final_price.return_value = 90.0
    payload = {
        "base_price": 100.0,
        "discount_percentage": 10.0,
        "is_vip": False,
    }
    response = client.post("/discounts/calculate", json=payload)
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert data["final_price"] == 90.0
    mock_discount_service.calculate_final_price.assert_called_once_with(
        base_price=payload["base_price"],
        discount_percentage=payload["discount_percentage"],
        coupon_code=None,
        is_vip=payload["is_vip"],
    )


def test_calculate_discount_rejects_extra_fields():
    payload = {
        "base_price": 100.0,
        "discount_percentage": 10.0,
        "coupon_code": "SAVE10",
        "is_vip": False,
        "extra_field": "not_allowed",
    }
    response = client.post("/discounts/calculate", json=payload)
    # By default Pydantic rejects extra fields unless configured otherwise
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY
    assert "extra_field" in response.text


@pytest.mark.parametrize("method", ["get", "put", "delete", "patch"])
def test_calculate_discount_method_not_allowed(method):
    func = getattr(client, method)
    response = func("/discounts/calculate")
    assert response.status_code == status.HTTP_405_METHOD_NOT_ALLOWED


@patch("app.api.routes.discount_service")
def test_calculate_discount_service_returns_none(mock_discount_service):
    mock_discount_service.calculate_final_price.return_value = None
    payload = make_payload()
    response = client.post("/discounts/calculate", json=payload)
    # If service returns None, Pydantic validation for DiscountResponse fails.
    # Since ValidationError is a ValueError, the controller catches it and returns 400.
    assert response.status_code == status.HTTP_400_BAD_REQUEST


@patch("app.api.routes.discount_service")
def test_calculate_discount_service_raises_unexpected_exception_returns_500(mock_discount_service):
    mock_discount_service.calculate_final_price.side_effect = RuntimeError("Erro inesperado")
    payload = make_payload()
    response = client.post("/discounts/calculate", json=payload)
    # Since only ValueError is caught, other exceptions cause 500 Internal Server Error
    assert response.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR


def test_discount_endpoint_documentation_contains_discount_route():
    response = client.get("/openapi.json")
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert "/discounts/calculate" in data["paths"]