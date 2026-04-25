import pytest
from pydantic import ValidationError
from app.schemas import CartItemSchema, CartRequest, CartResponse


class TestCartItemSchema:

    def test_create_with_valid_data_should_succeed(self):
        item = CartItemSchema(id="item1", name="Item One", price=10.5, quantity=3)
        assert item.id == "item1"
        assert item.name == "Item One"
        assert item.price == 10.5
        assert item.quantity == 3

    @pytest.mark.parametrize("price", [-0.01, -100, -1e10])
    def test_create_with_negative_price_should_fail(self, price):
        with pytest.raises(ValidationError) as exc_info:
            CartItemSchema(id="item1", name="Item One", price=price, quantity=1)
        errors = exc_info.value.errors()
        assert any(e['loc'] == ('price',) and e['type'] == 'value_error.number.not_ge' for e in errors)

    @pytest.mark.parametrize("quantity", [0, -1, -100])
    def test_create_with_zero_or_negative_quantity_should_fail(self, quantity):
        with pytest.raises(ValidationError) as exc_info:
            CartItemSchema(id="item1", name="Item One", price=10.0, quantity=quantity)
        errors = exc_info.value.errors()
        assert any(e['loc'] == ('quantity',) and e['type'] == 'value_error.number.not_ge' for e in errors)

    def test_create_with_quantity_omitted_should_default_to_one(self):
        item = CartItemSchema(id="item1", name="Item One", price=10.0)
        assert item.quantity == 1

    @pytest.mark.parametrize("invalid_id", [None, "", "   "])
    def test_create_with_empty_or_null_id_should_fail(self, invalid_id):
        with pytest.raises(ValidationError) as exc_info:
            CartItemSchema(id=invalid_id, name="Item One", price=10.0, quantity=1)
        errors = exc_info.value.errors()
        assert any(e['loc'] == ('id',) and e['type'] == 'value_error.missing' or e['type'] == 'value_error.str.min_length' or e['type'] == 'type_error.str' for e in errors)

    @pytest.mark.parametrize("invalid_name", [None, "", "   "])
    def test_create_with_empty_or_null_name_should_fail(self, invalid_name):
        with pytest.raises(ValidationError) as exc_info:
            CartItemSchema(id="item1", name=invalid_name, price=10.0, quantity=1)
        errors = exc_info.value.errors()
        assert any(e['loc'] == ('name',) and e['type'] == 'value_error.missing' or e['type'] == 'value_error.str.min_length' or e['type'] == 'type_error.str' for e in errors)


class TestCartRequestSchema:

    def test_create_with_multiple_valid_items_should_succeed(self):
        items = [
            CartItemSchema(id="item1", name="Item One", price=10.0, quantity=2),
            CartItemSchema(id="item2", name="Item Two", price=5.5, quantity=1),
        ]
        req = CartRequest(items=items, coupon_code="DISCOUNT10", is_vip=True)
        assert len(req.items) == 2
        assert req.coupon_code == "DISCOUNT10"
        assert req.is_vip is True

    def test_create_with_empty_items_list_should_succeed(self):
        # The model does not forbid empty list, so it should succeed
        req = CartRequest(items=[])
        assert req.items == []
        assert req.coupon_code is None
        assert req.is_vip is False

    @pytest.mark.parametrize("coupon_code", [None, "", "SAVE20"])
    def test_coupon_code_accepts_none_empty_and_valid_string(self, coupon_code):
        req = CartRequest(items=[CartItemSchema(id="item1", name="Item One", price=10.0)], coupon_code=coupon_code)
        assert req.coupon_code == coupon_code

    def test_is_vip_defaults_to_false_when_omitted(self):
        req = CartRequest(items=[CartItemSchema(id="item1", name="Item One", price=10.0)])
        assert req.is_vip is False

    def test_is_vip_accepts_true_explicitly(self):
        req = CartRequest(items=[CartItemSchema(id="item1", name="Item One", price=10.0)], is_vip=True)
        assert req.is_vip is True


class TestCartResponseSchema:

    def test_create_with_typical_values_should_succeed(self):
        resp = CartResponse(subtotal=100.0, tax_amount=10.0, final_price=110.0, items_count=3)
        assert resp.subtotal == 100.0
        assert resp.tax_amount == 10.0
        assert resp.final_price == 110.0
        assert resp.items_count == 3

    def test_serialization_to_json_and_back(self):
        resp = CartResponse(subtotal=50.5, tax_amount=5.05, final_price=55.55, items_count=2)
        json_data = resp.json()
        resp2 = CartResponse.parse_raw(json_data)
        assert resp2.subtotal == 50.5
        assert resp2.tax_amount == 5.05
        assert resp2.final_price == 55.55
        assert resp2.items_count == 2

    @pytest.mark.parametrize("items_count", [0, 1, 10, 100])
    def test_items_count_accepts_various_values(self, items_count):
        resp = CartResponse(subtotal=0.0, tax_amount=0.0, final_price=0.0, items_count=items_count)
        assert resp.items_count == items_count