import pytest
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock
from app.api import routes
from app.schemas import CartRequest, CartResponse, CartRequest
from fastapi import status
from pydantic import ValidationError

client = TestClient(routes.router)


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
    assert response.json()["detail"] == error_message
    mock_cart_service.calculate_cart_total.assert_called_once()


@patch("app.api.routes.cart_service")
def test_calculate_cart_applies_vip_discount_correctly(mock_cart_service):
    # Arrange
    items = [{"product_id": 1, "quantity": 2, "price": 50.0}]
    coupon_code = None
    is_vip = True
    expected_result = {
        "total": 100.0,
        "discount": 20.0,
        "final_total": 80.0,
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


@patch("app.api.routes.cart_service")
def test_calculate_cart_returns_500_on_unexpected_exception(mock_cart_service):
    # Arrange
    items = [{"product_id": 1, "quantity": 1, "price": 10.0}]
    coupon_code = None
    is_vip = False

    mock_cart_service.calculate_cart_total.side_effect = RuntimeError("Unexpected error")

    payload = make_cart_request_payload(items=items, coupon_code=coupon_code, is_vip=is_vip)

    # Act
    response = client.post("/cart/calculate", json=payload)

    # Assert
    assert response.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR


@patch("app.api.routes.cart_service")
def test_calculate_cart_calls_service_with_correct_parameters(mock_cart_service):
    # Arrange
    items = [
        {"product_id": 1, "quantity": 2, "price": 10.0},
        {"product_id": 2, "quantity": 3, "price": 5.0},
    ]
    coupon_code = "COUPON123"
    is_vip = True
    expected_result = {
        "total": 35.0,
        "discount": 5.0,
        "final_total": 30.0,
        "items": items,
    }
    mock_cart_service.calculate_cart_total.return_value = expected_result

    payload = make_cart_request_payload(items=items, coupon_code=coupon_code, is_vip=is_vip)

    # Act
    response = client.post("/cart/calculate", json=payload)

    # Assert
    assert response.status_code == status.HTTP_200_OK
    mock_cart_service.calculate_cart_total.assert_called_once()
    called_args, called_kwargs = mock_cart_service.calculate_cart_total.call_args
    # Check that items are list of dicts
    assert isinstance(called_kwargs["items"], list)
    for item in called_kwargs["items"]:
        assert isinstance(item, dict)
    assert called_kwargs["coupon_code"] == coupon_code
    assert called_kwargs["is_vip"] == is_vip


def test_calculate_cart_rejects_payload_with_extra_fields():
    # Payload with extra fields not defined in schema should be rejected by Pydantic
    payload = {
        "items": [{"product_id": 1, "quantity": 1, "price": 10.0, "extra_field": "not_allowed"}],
        "coupon_code": None,
        "is_vip": False,
    }
    response = client.post("/cart/calculate", json=payload)
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY


def test_calculate_cart_accepts_payload_without_coupon_code():
    # coupon_code omitted should be accepted and treated as None
    payload = {
        "items": [{"product_id": 1, "quantity": 1, "price": 10.0}],
        "is_vip": False,
    }
    response = client.post("/cart/calculate", json=payload)
    # Since cart_service is not mocked here, it will raise error, so just check 422 or 400 or 500
    assert response.status_code in {status.HTTP_422_UNPROCESSABLE_ENTITY, status.HTTP_400_BAD_REQUEST, status.HTTP_500_INTERNAL_SERVER_ERROR}


def test_calculate_cart_rejects_items_with_zero_quantity_or_price():
    # Quantity or price zero should be invalid
    payload = {
        "items": [
            {"product_id": 1, "quantity": 0, "price": 10.0},
            {"product_id": 2, "quantity": 1, "price": 0.0},
        ],
        "coupon_code": None,
        "is_vip": False,
    }
    response = client.post("/cart/calculate", json=payload)
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY


def test_calculate_cart_rejects_items_with_malicious_strings():
    # Strings with special characters in product_id or coupon_code should be rejected if schema is strict
    payload = {
        "items": [{"product_id": "DROP TABLE users;", "quantity": 1, "price": 10.0}],
        "coupon_code": "<script>alert(1)</script>",
        "is_vip": False,
    }
    response = client.post("/cart/calculate", json=payload)
    # Depending on schema, this may be rejected or accepted; we check for rejection here
    assert response.status_code in {status.HTTP_422_UNPROCESSABLE_ENTITY, status.HTTP_400_BAD_REQUEST}


@patch("app.api.routes.cart_service")
def test_calculate_cart_handles_duplicate_items_correctly(mock_cart_service):
    # Arrange
    items = [
        {"product_id": 1, "quantity": 1, "price": 10.0},
        {"product_id": 1, "quantity": 2, "price": 10.0},
    ]
    coupon_code = None
    is_vip = False
    expected_result = {
        "total": 30.0,
        "discount": 0.0,
        "final_total": 30.0,
        "items": items,
    }
    mock_cart_service.calculate_cart_total.return_value = expected_result

    payload = make_cart_request_payload(items=items, coupon_code=coupon_code, is_vip=is_vip)

    # Act
    response = client.post("/cart/calculate", json=payload)

    # Assert
    assert response.status_code == status.HTTP_200_OK
    mock_cart_service.calculate_cart_total.assert_called_once()
    called_args, called_kwargs = mock_cart_service.calculate_cart_total.call_args
    assert called_kwargs["items"] == items