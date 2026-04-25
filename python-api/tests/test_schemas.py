import pytest
from pydantic import ValidationError
from app.schemas import DiscountRequest, DiscountResponse


class TestDiscountRequestModel:

    def test_create_with_valid_values_should_succeed(self):
        req = DiscountRequest(
            base_price=100.0,
            discount_percentage=10.0,
            coupon_code="SAVE10",
            is_vip=True
        )
        assert req.base_price == 100.0
        assert req.discount_percentage == 10.0
        assert req.coupon_code == "SAVE10"
        assert req.is_vip is True

    @pytest.mark.parametrize("base_price", [-0.01, -100, -1e10])
    def test_create_with_negative_base_price_should_fail(self, base_price):
        with pytest.raises(ValidationError) as exc_info:
            DiscountRequest(base_price=base_price)
        errors = exc_info.value.errors()
        assert any(e['loc'] == ('base_price',) and e['type'] == 'value_error.number.not_ge' for e in errors)

    @pytest.mark.parametrize("discount_percentage", [-0.01, -1, 100.1, 150])
    def test_create_with_discount_percentage_out_of_bounds_should_fail(self, discount_percentage):
        with pytest.raises(ValidationError) as exc_info:
            DiscountRequest(base_price=10.0, discount_percentage=discount_percentage)
        errors = exc_info.value.errors()
        assert any(e['loc'] == ('discount_percentage',) and e['type'] == 'value_error.number.not_ge' or e['type'] == 'value_error.number.not_le' for e in errors)

    @pytest.mark.parametrize("coupon_code", [None, "", " ", "ABC123", "!@#$%^&*()", "A" * 1000])
    def test_coupon_code_accepts_various_values(self, coupon_code):
        req = DiscountRequest(base_price=10.0, coupon_code=coupon_code)
        assert req.coupon_code == coupon_code

    @pytest.mark.parametrize("is_vip", [True, False])
    def test_is_vip_accepts_true_and_false(self, is_vip):
        req = DiscountRequest(base_price=10.0, is_vip=is_vip)
        assert req.is_vip is is_vip

    def test_discount_percentage_defaults_to_zero_and_coupon_code_and_is_vip_defaults(self):
        req = DiscountRequest(base_price=50.0)
        assert req.discount_percentage == 0.0
        assert req.coupon_code is None
        assert req.is_vip is False

    def test_base_price_zero_is_accepted(self):
        req = DiscountRequest(base_price=0.0)
        assert req.base_price == 0.0


class TestDiscountResponseModel:

    def test_create_with_valid_final_price_should_succeed(self):
        resp = DiscountResponse(final_price=99.99)
        assert resp.final_price == 99.99

    def test_serialization_and_deserialization(self):
        resp = DiscountResponse(final_price=123.45)
        json_data = resp.json()
        resp2 = DiscountResponse.parse_raw(json_data)
        assert resp2.final_price == 123.45


class TestDiscountRequestSerialization:

    def test_serialization_and_deserialization_with_all_fields(self):
        req = DiscountRequest(
            base_price=200.0,
            discount_percentage=15.5,
            coupon_code="VIP2024",
            is_vip=True
        )
        json_data = req.json()
        req2 = DiscountRequest.parse_raw(json_data)
        assert req2.base_price == 200.0
        assert req2.discount_percentage == 15.5
        assert req2.coupon_code == "VIP2024"
        assert req2.is_vip is True

    def test_serialization_and_deserialization_with_defaults(self):
        req = DiscountRequest(base_price=100.0)
        json_data = req.json()
        req2 = DiscountRequest.parse_raw(json_data)
        assert req2.base_price == 100.0
        assert req2.discount_percentage == 0.0
        assert req2.coupon_code is None
        assert req2.is_vip is False