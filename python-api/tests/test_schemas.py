import pytest
from pydantic import ValidationError
from app.schemas import CartItemSchema, CartRequest, CartResponse, DiscountRequest, DiscountResponse


class TestUserCreateSchema:

    def test_create_user_without_is_vip_should_default_to_false(self):
        user = UserCreate(name="Alice", email="alice@example.com")
        assert user.name == "Alice"
        assert user.email == "alice@example.com"
        assert user.is_vip is False

    @pytest.mark.parametrize("price", [-0.01, -100, -1e10])
    def test_create_with_negative_price_should_fail(self, price):
        with pytest.raises(ValidationError) as exc_info:
            CartItemSchema(id="item1", name="Item One", price=price, quantity=1)
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("price",) and e["type"] == "greater_than_equal" for e in errors)

    @pytest.mark.parametrize("quantity", [0, -1, -100])
    def test_create_with_zero_or_negative_quantity_should_fail(self, quantity):
        with pytest.raises(ValidationError) as exc_info:
            CartItemSchema(id="item1", name="Item One", price=10.0, quantity=quantity)
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("quantity",) and e["type"] == "greater_than_equal" for e in errors)

    def test_create_user_with_is_vip_false_should_set_false(self):
        user = UserCreate(name="Carol", email="carol@example.com", is_vip=False)
        assert user.is_vip is False

    @pytest.mark.parametrize("invalid_value", ["yes", "no", 1, 0, None, "true", "false", [], {}])
    def test_create_user_with_invalid_is_vip_should_raise_validation_error(self, invalid_value):
        # Only bool is accepted, so strings and other types should fail
        if isinstance(invalid_value, bool):
            # bool is valid, skip
            pytest.skip("Boolean values are valid")
        with pytest.raises(ValidationError) as exc_info:
            UserCreate(name="Dave", email="dave@example.com", is_vip=invalid_value)
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("id",) for e in errors)

    @pytest.mark.parametrize("invalid_name", [None, "", "   "])
    def test_create_with_empty_or_null_name_should_fail(self, invalid_name):
        with pytest.raises(ValidationError) as exc_info:
            UserResponse.model_validate(user_dict)
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("name",) for e in errors)


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

    def test_serialization_and_deserialization(self):
        resp = DiscountResponse(final_price=123.45)
        json_data = resp.model_dump_json()
        resp2 = DiscountResponse.model_validate_json(json_data)
        assert resp2.final_price == 123.45


class TestDiscountRequestSerialization:

    def test_serialization_and_deserialization_with_all_fields(self):
        req = DiscountRequest(
            base_price=200.0,
            discount_percentage=15.5,
            coupon_code="VIP2024",
            is_vip=True
        )
        json_data = req.model_dump_json()
        req2 = DiscountRequest.model_validate_json(json_data)
        assert req2.base_price == 200.0
        assert req2.discount_percentage == 15.5
        assert req2.coupon_code == "VIP2024"
        assert req2.is_vip is True

    def test_serialization_and_deserialization_with_defaults(self):
        req = DiscountRequest(base_price=100.0)
        json_data = req.model_dump_json()
        req2 = DiscountRequest.model_validate_json(json_data)
        assert req2.base_price == 100.0
        assert req2.discount_percentage == 0.0
        assert req2.coupon_code is None
        assert req2.is_vip is False
