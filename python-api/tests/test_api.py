import pytest
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock
from app.api import routes
from app.schemas import CartRequest, CartResponse, CartRequest
from fastapi import status
from pydantic import ValidationError

client = TestClient(routes.router)

client = TestClient(app, raise_server_exceptions=False)

def make_cart_request_payload(items=None, coupon_code=None, is_vip=False):
    if items is None:
        items = [{"product_id": 1, "quantity": 2, "price": 10.0}]
    payload = {
        "items": items,
        "coupon_code": coupon_code,
        "is_vip": is_vip,
    }
    # Remove keys with None values to simulate optional omission
    return {k: v for k, v in payload.items() if v is not None}


@patch("app.api.routes.cart_service")
def test_calculate_cart_returns_cart_response_with_correct_total(mock_cart_service):
    # Arrange
    items = [
        {"product_id": 1, "quantity": 2, "price": 10.0},
        {"product_id": 2, "quantity": 1, "price": 20.0},
    ]
    coupon_code = None
    is_vip = False
    expected_result = {
        "total": 40.0,
        "discount": 0.0,
        "final_total": 40.0,
        "items": items,
    }
    mock_cart_service.calculate_cart_total.return_value = expected_result

    payload = make_cart_request_payload(items=items, coupon_code=coupon_code, is_vip=is_vip)

    # Act
    response = client.post("/cart/calculate", json=payload)

    # Assert
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert data["total"] == expected_result["total"]
    assert data["discount"] == expected_result["discount"]
    assert data["final_total"] == expected_result["final_total"]
    assert data["items"] == expected_result["items"]
    mock_cart_service.calculate_cart_total.assert_called_once()
    # Validate that items passed to service are dicts (model_dump equivalent)
    called_args, called_kwargs = mock_cart_service.calculate_cart_total.call_args
    assert isinstance(called_kwargs["items"], list)
    for item in called_kwargs["items"]:
        assert isinstance(item, dict)
    assert called_kwargs["coupon_code"] == coupon_code
    assert called_kwargs["is_vip"] == is_vip


@patch("app.api.routes.cart_service")
def test_calculate_cart_applies_coupon_discount_correctly(mock_cart_service):
    # Arrange
    items = [{"product_id": 1, "quantity": 3, "price": 15.0}]
    coupon_code = "VALIDCOUPON"
    is_vip = False
    expected_result = {
        "total": 45.0,
        "discount": 5.0,
        "final_total": 40.0,
        "items": items,
    }
    mock_cart_service.calculate_cart_total.return_value = expected_result

    payload = make_cart_request_payload(items=items, coupon_code=coupon_code, is_vip=is_vip)

    # Act
    response = client.post("/cart/calculate", json=payload)

    # Assert
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert data["discount"] == expected_result["discount"]
    assert data["final_total"] == expected_result["final_total"]
    mock_cart_service.calculate_cart_total.assert_called_once_with(
        items=[{"product_id": 1, "quantity": 3, "price": 15.0}],
        coupon_code=coupon_code,
        is_vip=is_vip,
    )


@patch("app.api.routes.cart_service")
def test_calculate_cart_returns_400_on_value_error_from_service(mock_cart_service):
    # Arrange
    items = [{"product_id": 1, "quantity": 1, "price": 10.0}]
    coupon_code = "INVALID"
    is_vip = False
    error_message = "Cupom inválido"

    mock_cart_service.calculate_cart_total.side_effect = ValueError(error_message)

    payload = make_cart_request_payload(items=items, coupon_code=coupon_code, is_vip=is_vip)

    # Act
    response = client.post("/cart/calculate", json=payload)

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
    mock_cart_service.calculate_cart_total.return_value = expected_result

    payload = make_cart_request_payload(items=items, coupon_code=coupon_code, is_vip=is_vip)

    # Act
    response = client.post("/cart/calculate", json=payload)

    # Assert
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert data["discount"] == expected_result["discount"]
    assert data["final_total"] == expected_result["final_total"]
    mock_cart_service.calculate_cart_total.assert_called_once_with(
        items=[{"product_id": 1, "quantity": 2, "price": 50.0}],
        coupon_code=coupon_code,
        is_vip=is_vip,
    )


@patch("app.api.routes.cart_service")
def test_calculate_cart_with_empty_items_returns_zero_total(mock_cart_service):
    # Arrange
    items = []
    coupon_code = None
    is_vip = False
    expected_result = {
        "total": 0.0,
        "discount": 0.0,
        "final_total": 0.0,
        "items": items,
    }
    mock_cart_service.calculate_cart_total.return_value = expected_result

    payload = make_cart_request_payload(items=items, coupon_code=coupon_code, is_vip=is_vip)

    # Act
    response = client.post("/cart/calculate", json=payload)

    # Assert
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert data["total"] == 0.0
    assert data["final_total"] == 0.0
    mock_cart_service.calculate_cart_total.assert_called_once()


def test_calculate_cart_rejects_invalid_item_data():
    # Negative quantity and price should be rejected by Pydantic validation
    payload = {
        "items": [
            {"product_id": 1, "quantity": -1, "price": 10.0},
            {"product_id": 2, "quantity": 1, "price": -5.0},
        ],
        "coupon_code": None,
        "is_vip": False,
    }
    response = client.post("/cart/calculate", json=payload)
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY


def test_calculate_cart_rejects_payload_missing_required_fields():
    # Missing 'items' field should cause validation error 422
    payload = {
        "coupon_code": "ANY",
        "is_vip": False,
    }
    response = client.post("/cart/calculate", json=payload)
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY


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
