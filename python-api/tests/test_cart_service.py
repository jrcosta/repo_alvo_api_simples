import pytest
from unittest.mock import Mock, create_autospec
from app.services.cart_service import CartItem, CartService
from app.services.discount_service import DiscountService

def test_cart_item_creation_with_negative_price_raises_value_error():
    with pytest.raises(ValueError, match="Preço não pode ser negativo"):
        CartItem(id="1", name="Produto Negativo", price=-10.0)

def test_calculate_cart_total_with_empty_items_returns_zeros():
    discount_service_mock = create_autospec(DiscountService)
    discount_service_mock.calculate_final_price.return_value = 0.0
    service = CartService(discount_service=discount_service_mock)

    result = service.calculate_cart_total(items=[], coupon_code=None, is_vip=False)

    assert result["subtotal"] == 0.0
    assert result["tax_amount"] == 0.0
    assert result["final_price"] == 0.0
    assert result["items_count"] == 0
    discount_service_mock.calculate_final_price.assert_called_once_with(
        base_price=0.0,
        discount_percentage=0.0,
        coupon_code=None,
        is_vip=False
    )

def test_calculate_cart_total_with_varied_items_and_default_quantity():
    discount_service_mock = create_autospec(DiscountService)
    # Simulate discount service returns total unchanged (no discount)
    discount_service_mock.calculate_final_price.side_effect = lambda base_price, **kwargs: base_price
    service = CartService(discount_service=discount_service_mock)

    items = [
        {"id": "1", "name": "Produto A", "price": 10.0, "quantity": 2},
        {"id": "2", "name": "Produto B", "price": 5.5},  # quantity default 1
        {"id": "3", "name": "Produto C", "price": 3.3333, "quantity": 3},
    ]

    result = service.calculate_cart_total(items=items, coupon_code=None, is_vip=False)

    expected_subtotal = (10.0 * 2) + (5.5 * 1) + (3.3333 * 3)
    expected_tax = expected_subtotal * 0.08
    expected_total_with_tax = expected_subtotal + expected_tax

    assert abs(result["subtotal"] - round(expected_subtotal, 2)) < 0.01
    assert abs(result["tax_amount"] - round(expected_tax, 2)) < 0.01
    # final_price should be equal to total_with_tax (no discount)
    assert abs(result["final_price"] - round(expected_total_with_tax, 2)) < 0.01
    assert result["items_count"] == 3

def test_calculate_cart_total_applies_discount_service_with_coupon_and_vip_flags():
    discount_service_mock = create_autospec(DiscountService)
    # Simulate discount service returns 90% of base_price if coupon is valid
    def discount_side_effect(base_price, discount_percentage, coupon_code, is_vip):
        if coupon_code == "VALID":
            return base_price * 0.9
        else:
            return base_price
    discount_service_mock.calculate_final_price.side_effect = discount_side_effect
    service = CartService(discount_service=discount_service_mock)

    items = [
        {"id": "1", "name": "Produto A", "price": 100.0, "quantity": 1},
    ]

    # Test with valid coupon, non VIP
    result_valid_coupon = service.calculate_cart_total(items=items, coupon_code="VALID", is_vip=False)
    subtotal = 100.0
    tax = subtotal * 0.08
    total_with_tax = subtotal + tax
    expected_final = total_with_tax * 0.9
    # subtotal <= 1000, so no extra 5% discount
    assert abs(result_valid_coupon["final_price"] - round(expected_final, 2)) < 0.01

    # Test with invalid coupon, non VIP
    result_invalid_coupon = service.calculate_cart_total(items=items, coupon_code="INVALID", is_vip=False)
    expected_final_invalid = total_with_tax
    assert abs(result_invalid_coupon["final_price"] - round(expected_final_invalid, 2)) < 0.01

    # Test with valid coupon, VIP user
    result_vip = service.calculate_cart_total(items=items, coupon_code="VALID", is_vip=True)
    expected_final_vip = total_with_tax * 0.9  # no extra 5% discount for VIP
    assert abs(result_vip["final_price"] - round(expected_final_vip, 2)) < 0.01

def test_calculate_cart_total_applies_additional_5_percent_discount_for_non_vip_over_1000():
    discount_service_mock = create_autospec(DiscountService)
    # Discount service returns base_price unchanged (no coupon discount)
    discount_service_mock.calculate_final_price.side_effect = lambda base_price, **kwargs: base_price
    service = CartService(discount_service=discount_service_mock)

    items = [
        {"id": "1", "name": "Produto Caro", "price": 1100.0, "quantity": 1},
    ]

    result = service.calculate_cart_total(items=items, coupon_code=None, is_vip=False)

    subtotal = 1100.0
    tax = subtotal * 0.08
    total_with_tax = subtotal + tax
    expected_final = total_with_tax * 0.95  # 5% discount applied

    assert abs(result["final_price"] - round(expected_final, 2)) < 0.01

def test_calculate_cart_total_does_not_apply_additional_discount_for_vip_over_1000():
    discount_service_mock = create_autospec(DiscountService)
    discount_service_mock.calculate_final_price.side_effect = lambda base_price, **kwargs: base_price
    service = CartService(discount_service=discount_service_mock)

    items = [
        {"id": "1", "name": "Produto Caro", "price": 1100.0, "quantity": 1},
    ]

    result = service.calculate_cart_total(items=items, coupon_code=None, is_vip=True)

    subtotal = 1100.0
    tax = subtotal * 0.08
    total_with_tax = subtotal + tax
    expected_final = total_with_tax  # no 5% discount for VIP

    assert abs(result["final_price"] - round(expected_final, 2)) < 0.01

def test_calculate_cart_total_raises_value_error_for_item_with_negative_price():
    discount_service_mock = create_autospec(DiscountService)
    service = CartService(discount_service=discount_service_mock)

    items = [
        {"id": "1", "name": "Produto Negativo", "price": -5.0, "quantity": 1},
    ]

    with pytest.raises(ValueError, match="Preço não pode ser negativo"):
        service.calculate_cart_total(items=items)

@pytest.mark.parametrize("quantity", [0, -1, -5])
def test_calculate_cart_total_accepts_zero_or_negative_quantity(quantity):
    # The code does not validate quantity, so it should process as is
    discount_service_mock = create_autospec(DiscountService)
    discount_service_mock.calculate_final_price.side_effect = lambda base_price, **kwargs: base_price
    service = CartService(discount_service=discount_service_mock)

    items = [
        {"id": "1", "name": "Produto", "price": 10.0, "quantity": quantity},
    ]

    result = service.calculate_cart_total(items=items)

    expected_subtotal = 10.0 * quantity
    expected_tax = expected_subtotal * 0.08
    total_with_tax = expected_subtotal + expected_tax
    expected_final = total_with_tax

    assert abs(result["subtotal"] - round(expected_subtotal, 2)) < 0.01
    assert abs(result["tax_amount"] - round(expected_tax, 2)) < 0.01
    assert abs(result["final_price"] - round(expected_final, 2)) < 0.01
    assert result["items_count"] == 1

def test_calculate_cart_total_correctly_rounds_values():
    discount_service_mock = create_autospec(DiscountService)
    discount_service_mock.calculate_final_price.side_effect = lambda base_price, **kwargs: base_price
    service = CartService(discount_service=discount_service_mock)

    items = [
        {"id": "1", "name": "Produto Preciso", "price": 10.12345, "quantity": 3},
    ]

    result = service.calculate_cart_total(items=items)

    expected_subtotal = 10.12345 * 3
    expected_tax = expected_subtotal * 0.08
    total_with_tax = expected_subtotal + expected_tax
    expected_final = total_with_tax

    assert result["subtotal"] == round(expected_subtotal, 2)
    assert result["tax_amount"] == round(expected_tax, 2)
    assert result["final_price"] == round(expected_final, 2)

def test_calculate_cart_total_items_count_matches_number_of_items():
    discount_service_mock = create_autospec(DiscountService)
    discount_service_mock.calculate_final_price.side_effect = lambda base_price, **kwargs: base_price
    service = CartService(discount_service=discount_service_mock)

    items = [
        {"id": "1", "name": "Produto 1", "price": 10.0, "quantity": 1},
        {"id": "2", "name": "Produto 2", "price": 20.0, "quantity": 2},
        {"id": "3", "name": "Produto 3", "price": 30.0, "quantity": 3},
    ]

    result = service.calculate_cart_total(items=items)

    assert result["items_count"] == 3

def test_calculate_cart_total_handles_discount_service_exception_gracefully():
    discount_service_mock = create_autospec(DiscountService)
    discount_service_mock.calculate_final_price.side_effect = Exception("Erro no serviço de desconto")
    service = CartService(discount_service=discount_service_mock)

    items = [
        {"id": "1", "name": "Produto", "price": 10.0, "quantity": 1},
    ]

    with pytest.raises(Exception, match="Erro no serviço de desconto"):
        service.calculate_cart_total(items=items)

def test_calculate_cart_total_rejects_items_missing_required_fields():
    discount_service_mock = create_autospec(DiscountService)
    discount_service_mock.calculate_final_price.side_effect = lambda base_price, **kwargs: base_price
    service = CartService(discount_service=discount_service_mock)

    # Missing 'price'
    items_missing_price = [
        {"id": "1", "name": "Produto", "quantity": 1},
    ]
    with pytest.raises(KeyError):
        service.calculate_cart_total(items=items_missing_price)

    # Missing 'id'
    items_missing_id = [
        {"name": "Produto", "price": 10.0, "quantity": 1},
    ]
    with pytest.raises(KeyError):
        service.calculate_cart_total(items=items_missing_id)

    # Missing 'name'
    items_missing_name = [
        {"id": "1", "price": 10.0, "quantity": 1},
    ]
    with pytest.raises(KeyError):
        service.calculate_cart_total(items=items_missing_name)

def test_calculate_cart_total_rejects_items_with_invalid_field_types():
    discount_service_mock = create_autospec(DiscountService)
    discount_service_mock.calculate_final_price.side_effect = lambda base_price, **kwargs: base_price
    service = CartService(discount_service=discount_service_mock)

    # price as string
    items_price_str = [
        {"id": "1", "name": "Produto", "price": "10.0", "quantity": 1},
    ]
    with pytest.raises(TypeError):
        service.calculate_cart_total(items=items_price_str)

    # quantity as float negative
    items_quantity_float = [
        {"id": "1", "name": "Produto", "price": 10.0, "quantity": -1.5},
    ]
    # The code does not validate quantity type, so it may accept float and calculate
    # But multiplication with float quantity is allowed, so no error expected
    result = service.calculate_cart_total(items=items_quantity_float)
    expected_subtotal = 10.0 * -1.5
    expected_tax = expected_subtotal * 0.08
    total_with_tax = expected_subtotal + expected_tax
    expected_final = total_with_tax
    assert abs(result["subtotal"] - round(expected_subtotal, 2)) < 0.01

def test_calculate_cart_total_with_subtotal_exactly_1000_no_additional_discount():
    discount_service_mock = create_autospec(DiscountService)
    discount_service_mock.calculate_final_price.side_effect = lambda base_price, **kwargs: base_price
    service = CartService(discount_service=discount_service_mock)

    items = [
        {"id": "1", "name": "Produto", "price": 1000.0, "quantity": 1},
    ]

    result = service.calculate_cart_total(items=items, is_vip=False)

    subtotal = 1000.0
    tax = subtotal * 0.08
    total_with_tax = subtotal + tax
    expected_final = total_with_tax  # no 5% discount because subtotal == 1000 (not > 1000)

    assert abs(result["final_price"] - round(expected_final, 2)) < 0.01

def test_calculate_cart_total_with_extremely_high_values_stability():
    discount_service_mock = create_autospec(DiscountService)
    discount_service_mock.calculate_final_price.side_effect = lambda base_price, **kwargs: base_price
    service = CartService(discount_service=discount_service_mock)

    high_price = 1e9
    items = [
        {"id": "1", "name": "Produto Muito Caro", "price": high_price, "quantity": 2},
    ]

    result = service.calculate_cart_total(items=items, is_vip=False)

    subtotal = high_price * 2
    tax = subtotal * 0.08
    total_with_tax = subtotal + tax
    expected_final = total_with_tax * 0.95  # 5% discount for non VIP and subtotal > 1000

    assert abs(result["final_price"] - round(expected_final, 2)) < 1.0  # allow small float diff

def test_calculate_cart_total_with_multiple_coupons_only_accepts_one(monkeypatch):
    # The current implementation accepts only one coupon_code parameter,
    # so test that passing multiple coupons is not supported and only one is used.
    discount_service_mock = create_autospec(DiscountService)

    def discount_side_effect(base_price, discount_percentage, coupon_code, is_vip):
        # Return different values depending on coupon_code
        if coupon_code == "COUPON1":
            return base_price * 0.9
        elif coupon_code == "COUPON2":
            return base_price * 0.8
        else:
            return base_price

    discount_service_mock.calculate_final_price.side_effect = discount_side_effect
    service = CartService(discount_service=discount_service_mock)

    items = [
        {"id": "1", "name": "Produto", "price": 100.0, "quantity": 1},
    ]

    # Simulate passing only one coupon_code (as per method signature)
    result = service.calculate_cart_total(items=items, coupon_code="COUPON1", is_vip=False)
    subtotal = 100.0
    tax = subtotal * 0.08
    total_with_tax = subtotal + tax
    expected_final = total_with_tax * 0.9

    assert abs(result["final_price"] - round(expected_final, 2)) < 0.01

    # No support for multiple coupons, so no test for multiple coupons sequence needed