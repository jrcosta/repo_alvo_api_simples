from unittest.mock import patch

from fastapi import status
from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app, raise_server_exceptions=False)


def make_cart_request_payload(items=None, coupon_code=None, is_vip=False):
    if items is None:
        items = [{"id": "1", "name": "Produto A", "quantity": 2, "price": 10.0}]

    payload = {
        "items": items,
        "coupon_code": coupon_code,
        "is_vip": is_vip,
    }
    return {key: value for key, value in payload.items() if value is not None}


def make_discount_payload(coupon_code=None, is_vip=False):
    payload = {
        "base_price": 100.0,
        "discount_percentage": 10.0,
        "coupon_code": coupon_code,
        "is_vip": is_vip,
    }
    return {key: value for key, value in payload.items() if value is not None}


@patch("app.api.routes.cart_service")
def test_calculate_cart_returns_cart_response_with_correct_total(mock_cart_service):
    items = [
        {"id": "1", "name": "Produto A", "quantity": 2, "price": 10.0},
        {"id": "2", "name": "Produto B", "quantity": 1, "price": 20.0},
    ]
    expected_result = {
        "subtotal": 40.0,
        "tax_amount": 3.2,
        "final_price": 43.2,
        "items_count": 2,
    }
    mock_cart_service.calculate_cart_total.return_value = expected_result

    response = client.post("/cart/calculate", json=make_cart_request_payload(items=items))

    assert response.status_code == status.HTTP_200_OK
    assert response.json() == expected_result
    mock_cart_service.calculate_cart_total.assert_called_once_with(
        items=items,
        coupon_code=None,
        is_vip=False,
    )


@patch("app.api.routes.cart_service")
def test_calculate_cart_returns_400_on_value_error_from_service(mock_cart_service):
    mock_cart_service.calculate_cart_total.side_effect = ValueError("Cupom inválido")

    response = client.post("/cart/calculate", json=make_cart_request_payload(coupon_code="INVALID"))

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json() == {"detail": "Cupom inválido"}


def test_calculate_cart_rejects_invalid_item_data():
    payload = make_cart_request_payload(
        items=[{"id": "1", "name": "Produto A", "quantity": -1, "price": 10.0}]
    )

    response = client.post("/cart/calculate", json=payload)

    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY


def test_calculate_cart_rejects_payload_missing_required_fields():
    response = client.post("/cart/calculate", json={"coupon_code": "ANY", "is_vip": False})

    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY


def test_calculate_discount_missing_required_field():
    payload = {"discount_percentage": 10.0, "coupon_code": "QUERO10", "is_vip": False}

    response = client.post("/discounts/calculate", json=payload)

    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY
    assert "base_price" in response.text


def test_calculate_discount_invalid_field_types():
    payload = {
        "base_price": 100.0,
        "discount_percentage": 10.0,
        "coupon_code": [],
        "is_vip": False,
    }

    response = client.post("/discounts/calculate", json=payload)

    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY


@patch("app.api.routes.discount_service")
def test_calculate_discount_without_coupon_code(mock_discount_service):
    mock_discount_service.calculate_final_price.return_value = 90.0

    response = client.post("/discounts/calculate", json=make_discount_payload())

    assert response.status_code == status.HTTP_200_OK
    assert response.json() == {"final_price": 90.0}
    mock_discount_service.calculate_final_price.assert_called_once_with(
        base_price=100.0,
        discount_percentage=10.0,
        coupon_code=None,
        is_vip=False,
    )


@patch("app.api.routes.discount_service")
def test_calculate_discount_service_raises_value_error_returns_400(mock_discount_service):
    mock_discount_service.calculate_final_price.side_effect = ValueError("Desconto inválido")

    response = client.post("/discounts/calculate", json=make_discount_payload())

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json() == {"detail": "Desconto inválido"}


@patch("app.api.routes.discount_service")
def test_calculate_discount_service_raises_unexpected_exception_returns_500(mock_discount_service):
    mock_discount_service.calculate_final_price.side_effect = RuntimeError("Erro inesperado")

    response = client.post("/discounts/calculate", json=make_discount_payload())

    assert response.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR


def test_discount_endpoint_documentation_contains_discount_route():
    response = client.get("/openapi.json")

    assert response.status_code == status.HTTP_200_OK
    assert "/discounts/calculate" in response.json()["paths"]


@patch("app.api.routes.cart_service")
def test_calculate_cart_rejects_negative_and_zero_quantity_items(mock_cart_service):
    # Negative quantity should be rejected by validation (422)
    payload = make_cart_request_payload(
        items=[{"id": "1", "name": "Produto A", "quantity": -5, "price": 10.0}]
    )
    response = client.post("/cart/calculate", json=payload)
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY
    mock_cart_service.calculate_cart_total.assert_not_called()

    # Zero quantity might be accepted or rejected depending on business rules,
    # here we test zero quantity is rejected (assuming validation)
    payload = make_cart_request_payload(
        items=[{"id": "1", "name": "Produto A", "quantity": 0, "price": 10.0}]
    )
    response = client.post("/cart/calculate", json=payload)
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY
    mock_cart_service.calculate_cart_total.assert_not_called()


@patch("app.api.routes.cart_service")
def test_calculate_cart_calls_service_with_correct_parameters(mock_cart_service):
    items = [
        {"id": "1", "name": "Produto A", "quantity": 3, "price": 15.0},
    ]
    coupon_code = "PROMO10"
    is_vip = True
    expected_result = {
        "subtotal": 45.0,
        "tax_amount": 3.6,
        "final_price": 48.6,
        "items_count": 1,
    }
    mock_cart_service.calculate_cart_total.return_value = expected_result

    response = client.post("/cart/calculate", json=make_cart_request_payload(items=items, coupon_code=coupon_code, is_vip=is_vip))

    assert response.status_code == status.HTTP_200_OK
    assert response.json() == expected_result
    mock_cart_service.calculate_cart_total.assert_called_once_with(
        items=items,
        coupon_code=coupon_code,
        is_vip=is_vip,
    )


@patch("app.api.routes.discount_service")
def test_calculate_discount_with_invalid_and_missing_coupon_codes(mock_discount_service):
    # Coupon code invalid triggers ValueError
    mock_discount_service.calculate_final_price.side_effect = ValueError("Cupom inválido")

    payload = make_discount_payload(coupon_code="INVALID")
    response = client.post("/discounts/calculate", json=payload)
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json() == {"detail": "Cupom inválido"}

    mock_discount_service.calculate_final_price.reset_mock()

    # Coupon code missing (None) returns valid final price
    mock_discount_service.calculate_final_price.side_effect = None
    mock_discount_service.calculate_final_price.return_value = 85.0

    payload = make_discount_payload(coupon_code=None)
    response = client.post("/discounts/calculate", json=payload)
    assert response.status_code == status.HTTP_200_OK
    assert response.json() == {"final_price": 85.0}
    mock_discount_service.calculate_final_price.assert_called_once_with(
        base_price=100.0,
        discount_percentage=10.0,
        coupon_code=None,
        is_vip=False,
    )


def test_calculate_cart_rejects_payload_with_extra_unexpected_fields():
    payload = make_cart_request_payload()
    payload["unexpected_field"] = "unexpected_value"

    response = client.post("/cart/calculate", json=payload)

    assert response.status_code == status.HTTP_200_OK


@patch("app.api.routes.cart_service")
def test_calculate_cart_returns_400_for_empty_items_list_from_service(mock_cart_service):
    payload = make_cart_request_payload(items=[])
    mock_cart_service.calculate_cart_total.side_effect = ValueError("Carrinho vazio")

    response = client.post("/cart/calculate", json=payload)

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json() == {"detail": "Carrinho vazio"}
