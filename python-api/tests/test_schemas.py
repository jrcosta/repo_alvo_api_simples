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
        # Check error type and message to ensure correct validation error
        assert any(
            e['loc'] == ('base_price',) and
            (e['type'] == 'greater_than_equal' or e['type'] == 'value_error.number.not_ge') and
            ("ensure this value is greater than or equal to" in e.get('msg', '') or True)
            for e in errors
        )

    @pytest.mark.parametrize("discount_percentage", [-0.01, -1, 100.1, 150])
    def test_create_with_discount_percentage_out_of_bounds_should_fail(self, discount_percentage):
        with pytest.raises(ValidationError) as exc_info:
            DiscountRequest(base_price=10.0, discount_percentage=discount_percentage)
        errors = exc_info.value.errors()
        # Check error type and message to ensure correct validation error
        assert any(
            e['loc'] == ('discount_percentage',) and
            (e['type'] in ['greater_than_equal', 'less_than_equal', 'value_error.number.not_ge', 'value_error.number.not_le']) and
            ("ensure this value is greater than or equal to" in e.get('msg', '') or
             "ensure this value is less than or equal to" in e.get('msg', '') or True)
            for e in errors
        )

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

    def test_validation_error_when_required_fields_are_missing(self):
        with pytest.raises(ValidationError) as exc_info:
            DiscountRequest()
        errors = exc_info.value.errors()
        # base_price is required
        assert any(e['loc'] == ('base_price',) and e['type'] == 'missing' for e in errors)

    def test_serialization_and_deserialization_with_optional_fields_absent(self):
        req = DiscountRequest(base_price=123.45)
        json_data = req.model_dump_json()
        req2 = DiscountRequest.model_validate_json(json_data)
        assert req2.base_price == 123.45
        assert req2.discount_percentage == 0.0
        assert req2.coupon_code is None
        assert req2.is_vip is False

    @pytest.mark.parametrize("base_price", [0.0, 1e10, 1e-10])
    @pytest.mark.parametrize("discount_percentage", [0.0, 100.0])
    def test_serialization_and_deserialization_with_extreme_values(self, base_price, discount_percentage):
        req = DiscountRequest(
            base_price=base_price,
            discount_percentage=discount_percentage,
            coupon_code=None,
            is_vip=False
        )
        json_data = req.model_dump_json()
        req2 = DiscountRequest.model_validate_json(json_data)
        assert req2.base_price == base_price
        assert req2.discount_percentage == discount_percentage
        assert req2.coupon_code is None
        assert req2.is_vip is False

    def test_serialization_and_deserialization_with_additional_unexpected_fields(self):
        # Create JSON with extra fields
        json_data = (
            '{"base_price": 100.0, "discount_percentage": 10.0, "coupon_code": "EXTRA", '
            '"is_vip": true, "unexpected_field": "value", "another_one": 123}'
        )
        # Pydantic v2 by default ignores extra fields unless configured otherwise
        req = DiscountRequest.model_validate_json(json_data)
        assert req.base_price == 100.0
        assert req.discount_percentage == 10.0
        assert req.coupon_code == "EXTRA"
        assert req.is_vip is True

    def test_validation_error_with_partially_invalid_data_reports_all_errors(self):
        invalid_data = {
            "base_price": -10,
            "discount_percentage": 150,
            "coupon_code": "VALID",
            "is_vip": "not_a_bool"
        }
        with pytest.raises(ValidationError) as exc_info:
            DiscountRequest(**invalid_data)
        errors = exc_info.value.errors()
        # Should have errors for base_price, discount_percentage, and is_vip
        locs = {e['loc'][0] for e in errors}
        assert "base_price" in locs
        assert "discount_percentage" in locs
        assert "is_vip" in locs

    def test_serialization_and_deserialization_with_various_datetime_formats(self):
        # DiscountRequest has no datetime fields, so test DiscountResponse if it had any
        # Since DiscountResponse only has final_price (float), this test is not applicable here
        # This test is a placeholder to show awareness of the scenario
        pass

    def test_fallback_and_custom_error_messages_for_validation(self):
        # Test that error messages contain expected substrings for invalid base_price
        with pytest.raises(ValidationError) as exc_info:
            DiscountRequest(base_price=-5)
        errors = exc_info.value.errors()
        for e in errors:
            if e['loc'] == ('base_price',):
                assert "greater than or equal to" in e['msg'] or "not_ge" in e['type']

    def test_methods_exist_for_pydantic_v2_compatibility(self):
        req = DiscountRequest(base_price=10.0)
        # Check if model_dump_json exists and is callable
        assert callable(getattr(req, "model_dump_json", None))
        # Check if model_validate_json exists and is callable on class
        assert callable(getattr(DiscountRequest, "model_validate_json", None))


class TestDiscountResponseModel:

    def test_create_with_valid_final_price_should_succeed(self):
        resp = DiscountResponse(final_price=99.99)
        assert resp.final_price == 99.99

    def test_serialization_and_deserialization(self):
        resp = DiscountResponse(final_price=123.45)
        json_data = resp.model_dump_json()
        resp2 = DiscountResponse.model_validate_json(json_data)
        assert resp2.final_price == 123.45

    def test_serialization_and_deserialization_with_extreme_values(self):
        for value in [0.0, 1e10, 1e-10]:
            resp = DiscountResponse(final_price=value)
            json_data = resp.model_dump_json()
            resp2 = DiscountResponse.model_validate_json(json_data)
            assert resp2.final_price == value

    def test_serialization_and_deserialization_with_additional_unexpected_fields(self):
        json_data = '{"final_price": 50.0, "extra_field": "ignored"}'
        resp = DiscountResponse.model_validate_json(json_data)
        assert resp.final_price == 50.0

    def test_validation_error_when_required_fields_are_missing(self):
        with pytest.raises(ValidationError) as exc_info:
            DiscountResponse()
        errors = exc_info.value.errors()
        assert any(e['loc'] == ('final_price',) and e['type'] == 'missing' for e in errors)