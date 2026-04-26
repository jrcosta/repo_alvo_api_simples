import pytest
from pydantic import ValidationError
from app.schemas import (
    CartItemSchema,
    CartRequest,
    CartResponse,
    DiscountRequest,
    DiscountResponse,
    UserCreate,
    UserResponse,
)


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
        with pytest.raises(ValidationError) as exc_info:
            UserCreate(name="Dave", email="dave@example.com", is_vip=invalid_value)
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("is_vip",) for e in errors)

    @pytest.mark.parametrize("invalid_name", [None, "", "   "])
    def test_create_with_empty_or_null_name_should_fail(self, invalid_name):
        user_dict = {
            "id": 1,
            "name": invalid_name,
            "email": "alice@example.com",
            "is_vip": False,
        }
        with pytest.raises(ValidationError) as exc_info:
            UserResponse.model_validate(user_dict)
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("name",) for e in errors)

    @pytest.mark.parametrize("invalid_email", [None, "", "   ", "invalid-email", "user@com", "user@.com"])
    def test_create_user_with_invalid_email_should_fail(self, invalid_email):
        user_dict = {
            "id": 1,
            "name": "Valid Name",
            "email": invalid_email,
            "is_vip": False,
        }
        with pytest.raises(ValidationError) as exc_info:
            UserResponse.model_validate(user_dict)
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("email",) for e in errors)

    @pytest.mark.parametrize("missing_field", ["name", "email"])
    def test_create_user_response_missing_required_fields_should_fail(self, missing_field):
        user_dict = {
            "id": 1,
            "name": "Valid Name",
            "email": "valid@example.com",
            "is_vip": False,
        }
        user_dict.pop(missing_field)
        with pytest.raises(ValidationError) as exc_info:
            UserResponse.model_validate(user_dict)
        errors = exc_info.value.errors()
        assert any(e["loc"] == (missing_field,) for e in errors)

    @pytest.mark.parametrize("invalid_is_vip", ["yes", "no", 1, 0, None, "true", "false", [], {}])
    def test_user_response_with_invalid_is_vip_should_raise_validation_error(self, invalid_is_vip):
        user_dict = {
            "id": 1,
            "name": "Valid Name",
            "email": "valid@example.com",
            "is_vip": invalid_is_vip,
        }
        with pytest.raises(ValidationError) as exc_info:
            UserResponse.model_validate(user_dict)
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("is_vip",) for e in errors)

    def test_user_response_serialization_and_deserialization_with_vip_field(self):
        user = UserResponse(id=1, name="Alice", email="alice@example.com", is_vip=True)
        json_data = user.model_dump_json()
        user2 = UserResponse.model_validate_json(json_data)
        assert user2.id == 1
        assert user2.name == "Alice"
        assert user2.email == "alice@example.com"
        assert user2.is_vip is True


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

    @pytest.mark.parametrize("invalid_is_vip", [None, [], {}])
    def test_cart_request_with_invalid_is_vip_should_raise_validation_error(self, invalid_is_vip):
        items = [CartItemSchema(id="item1", name="Item One", price=10.0, quantity=1)]
        with pytest.raises(ValidationError) as exc_info:
            CartRequest(items=items, is_vip=invalid_is_vip)
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("is_vip",) for e in errors)

    @pytest.mark.parametrize(
        ("coerced_value", "expected"),
        [
            ("yes", True),
            ("no", False),
            (1, True),
            (0, False),
            ("true", True),
            ("false", False),
        ],
    )
    def test_cart_request_coerces_bool_values_for_is_vip(self, coerced_value, expected):
        items = [CartItemSchema(id="item1", name="Item One", price=10.0, quantity=1)]

        request = CartRequest(items=items, is_vip=coerced_value)

        assert request.is_vip is expected

    @pytest.mark.parametrize("extra_field", [{"extra": "value"}, {"unexpected": 123}])
    def test_cart_request_ignores_extra_fields(self, extra_field):
        base = {"items": [CartItemSchema(id="item1", name="Item One", price=10.0, quantity=1)]}
        base.update(extra_field)

        request = CartRequest.model_validate(base)

        assert not hasattr(request, next(iter(extra_field)))

    @pytest.mark.parametrize("invalid_coupon_code", [123, True, False, [], {}])
    def test_cart_request_with_invalid_coupon_code_type_should_raise_validation_error(self, invalid_coupon_code):
        items = [CartItemSchema(id="item1", name="Item One", price=10.0, quantity=1)]
        with pytest.raises(ValidationError) as exc_info:
            CartRequest(items=items, coupon_code=invalid_coupon_code)
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("coupon_code",) for e in errors)

    @pytest.mark.parametrize("invalid_items", [None, "", 123, {}, [None], [123]])
    def test_cart_request_with_invalid_items_should_raise_validation_error(self, invalid_items):
        with pytest.raises(ValidationError) as exc_info:
            CartRequest(items=invalid_items)
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("items",) or e["loc"][:1] == ("items",) for e in errors)

    @pytest.mark.parametrize("invalid_item", [
        {"id": None, "name": "Item", "price": 10.0, "quantity": 1},
        {"id": "item1", "name": "", "price": 10.0, "quantity": 1},
        {"id": "item1", "name": "Item", "price": -10.0, "quantity": 1},
        {"id": "item1", "name": "Item", "price": 10.0, "quantity": 0},
    ])
    def test_cart_request_with_invalid_cart_item_should_raise_validation_error(self, invalid_item):
        with pytest.raises(ValidationError) as exc_info:
            CartRequest(items=[invalid_item])
        errors = exc_info.value.errors()
        assert any("items" in e["loc"] for e in errors)

    @pytest.mark.parametrize("long_string", ["a" * 1001, "b" * 5000])
    def test_cart_item_name_with_excessively_long_string_should_succeed(self, long_string):
        item = CartItemSchema(id="item1", name=long_string, price=10.0, quantity=1)
        assert item.name == long_string

    @pytest.mark.parametrize("extreme_price", [1e10, 1e15, 1e20])
    def test_cart_item_price_with_extreme_high_values_should_succeed(self, extreme_price):
        item = CartItemSchema(id="item1", name="Item", price=extreme_price, quantity=1)
        assert item.price == extreme_price

    @pytest.mark.parametrize("extreme_quantity", [1, 1000, 1000000])
    def test_cart_item_quantity_with_large_values_should_succeed(self, extreme_quantity):
        item = CartItemSchema(id="item1", name="Item", price=10.0, quantity=extreme_quantity)
        assert item.quantity == extreme_quantity


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

    def test_cart_response_serialization_and_deserialization(self):
        resp = CartResponse(subtotal=50.0, tax_amount=5.0, final_price=55.0, items_count=2)
        json_data = resp.model_dump_json()
        resp2 = CartResponse.model_validate_json(json_data)
        assert resp2.subtotal == 50.0
        assert resp2.tax_amount == 5.0
        assert resp2.final_price == 55.0
        assert resp2.items_count == 2

    def test_cart_response_accepts_negative_values_without_schema_constraints(self):
        data = {
            "subtotal": -10.0,
            "tax_amount": -1.0,
            "final_price": -11.0,
            "items_count": -1,
        }

        response = CartResponse.model_validate(data)

        assert response.subtotal == -10.0
        assert response.tax_amount == -1.0
        assert response.final_price == -11.0
        assert response.items_count == -1

    def test_cart_response_with_missing_fields_should_fail(self):
        incomplete_data = {
            "subtotal": 10.0,
            "tax_amount": 1.0,
        }
        with pytest.raises(ValidationError) as exc_info:
            CartResponse.model_validate(incomplete_data)
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("final_price",) for e in errors)
        assert any(e["loc"] == ("items_count",) for e in errors)


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

    def test_discount_request_with_invalid_boolean_should_fail(self):
        with pytest.raises(ValidationError) as exc_info:
            DiscountRequest(base_price=100.0, is_vip="notabool")
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("is_vip",) for e in errors)

    def test_discount_request_with_invalid_discount_percentage_should_fail(self):
        with pytest.raises(ValidationError) as exc_info:
            DiscountRequest(base_price=100.0, discount_percentage=-5)
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("discount_percentage",) for e in errors)

    def test_discount_request_with_invalid_base_price_should_fail(self):
        with pytest.raises(ValidationError) as exc_info:
            DiscountRequest(base_price=-1)
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("base_price",) for e in errors)

    def test_discount_request_with_extra_fields_should_fail(self):
        data = {
            "base_price": 100.0,
            "discount_percentage": 10.0,
            "coupon_code": "SAVE10",
            "is_vip": False,
            "extra_field": "not allowed"
        }
        with pytest.raises(ValidationError) as exc_info:
            DiscountRequest.model_validate(data)
        errors = exc_info.value.errors()
        assert any(e["type"] == "extra_forbidden" for e in errors)

    def test_discount_request_serialization_with_partial_data(self):
        req = DiscountRequest(base_price=50.0)
        json_data = req.model_dump_json()
        req2 = DiscountRequest.model_validate_json(json_data)
        assert req2.base_price == 50.0
        assert req2.discount_percentage == 0.0
        assert req2.coupon_code is None
        assert req2.is_vip is False

    def test_discount_request_deserialization_with_corrupted_json_should_fail(self):
        corrupted_json = '{"base_price": 100.0, "discount_percentage": "fifteen"}'
        with pytest.raises(ValidationError):
            DiscountRequest.model_validate_json(corrupted_json)

    def test_discount_request_deserialization_with_incomplete_json_should_fail(self):
        incomplete_json = '{"discount_percentage": 10.0}'
        with pytest.raises(ValidationError):
            DiscountRequest.model_validate_json(incomplete_json)
