import pytest
from app.services.discount_service import DiscountService


@pytest.fixture
def discount_service():
    return DiscountService()


class TestCalculateFinalPriceTypeValidation:
    @pytest.mark.parametrize("invalid_base_price", ["string", [], None, {}, object()])
    def test_calculate_final_price_raises_type_error_for_invalid_base_price(self, discount_service, invalid_base_price):
        with pytest.raises(TypeError) as excinfo:
            discount_service.calculate_final_price(invalid_base_price, 10.0)
        assert "base_price deve ser um número" in str(excinfo.value)

    @pytest.mark.parametrize("invalid_discount_percentage", ["string", [], None, {}, object()])
    def test_calculate_final_price_raises_type_error_for_invalid_discount_percentage(self, discount_service, invalid_discount_percentage):
        with pytest.raises(TypeError) as excinfo:
            discount_service.calculate_final_price(100.0, invalid_discount_percentage)
        assert "discount_percentage deve ser um número" in str(excinfo.value)

    @pytest.mark.parametrize("invalid_coupon_code", [123, 45.6, [], {}, object()])
    def test_calculate_final_price_raises_type_error_for_invalid_coupon_code(self, discount_service, invalid_coupon_code):
        with pytest.raises(TypeError) as excinfo:
            discount_service.calculate_final_price(100.0, 10.0, coupon_code=invalid_coupon_code)
        assert "coupon_code deve ser uma string" in str(excinfo.value)

    @pytest.mark.parametrize("invalid_is_vip", ["string", 1, 0, [], {}, object()])
    def test_calculate_final_price_raises_type_error_for_invalid_is_vip(self, discount_service, invalid_is_vip):
        with pytest.raises(TypeError) as excinfo:
            discount_service.calculate_final_price(100.0, 10.0, is_vip=invalid_is_vip)
        assert "is_vip deve ser um booleano" in str(excinfo.value)

    def test_calculate_final_price_accepts_is_vip_none_without_error(self, discount_service):
        # According to code, is_vip=None is accepted without error
        # Should behave as if is_vip=False (no VIP discount)
        price_with_none = discount_service.calculate_final_price(100.0, 10.0, is_vip=None)
        price_with_false = discount_service.calculate_final_price(100.0, 10.0, is_vip=False)
        assert price_with_none == price_with_false

    def test_calculate_final_price_accepts_valid_inputs_and_calculates_correctly(self, discount_service):
        # Repeating some valid tests to ensure no regression
        # base_price=200.0, discount_percentage=50.0, coupon_code="QUERO10", is_vip=True
        final_price = discount_service.calculate_final_price(200.0, 50.0, coupon_code="QUERO10", is_vip=True)
        # Calculation: 200 - 50% = 100; VIP 5% on 100 = 95; coupon -10 = 85
        assert final_price == 85.00

    def test_calculate_final_price_rejects_empty_string_coupon_code(self, discount_service):
        # Empty string coupon_code is a string, so accepted, but no discount applied
        final_price = discount_service.calculate_final_price(100.0, 10.0, coupon_code="")
        # 100 - 10% = 90, no coupon discount
        assert final_price == 90.00

    def test_calculate_final_price_rejects_special_characters_coupon_code(self, discount_service):
        # Coupon code with special characters is string, accepted, no discount applied
        final_price = discount_service.calculate_final_price(100.0, 10.0, coupon_code="!@#$%")
        # 100 - 10% = 90, no coupon discount
        assert final_price == 90.00

    @pytest.mark.parametrize("invalid_obj", [{}, object()])
    def test_calculate_final_price_rejects_unexpected_object_types(self, discount_service, invalid_obj):
        with pytest.raises(TypeError):
            discount_service.calculate_final_price(invalid_obj, 10.0)
        with pytest.raises(TypeError):
            discount_service.calculate_final_price(100.0, invalid_obj)
        with pytest.raises(TypeError):
            discount_service.calculate_final_price(100.0, 10.0, coupon_code=invalid_obj)
        with pytest.raises(TypeError):
            discount_service.calculate_final_price(100.0, 10.0, is_vip=invalid_obj)


class TestApplyBulkDiscountTypeValidation:
    @pytest.mark.parametrize("invalid_items_count", [1.5, "string", None, [], {}, object()])
    def test_apply_bulk_discount_raises_type_error_for_invalid_items_count(self, discount_service, invalid_items_count):
        with pytest.raises(TypeError) as excinfo:
            discount_service.apply_bulk_discount(invalid_items_count, 100.0)
        assert "items_count deve ser um inteiro" in str(excinfo.value)

    @pytest.mark.parametrize("invalid_total_value", ["string", None, [], {}, object()])
    def test_apply_bulk_discount_raises_type_error_for_invalid_total_value(self, discount_service, invalid_total_value):
        with pytest.raises(TypeError) as excinfo:
            discount_service.apply_bulk_discount(5, invalid_total_value)
        assert "total_value deve ser um número" in str(excinfo.value)

    def test_apply_bulk_discount_applies_correct_discount_for_valid_inputs(self, discount_service):
        # Test various items_count and total_value combinations
        # 0 items: no discount
        assert discount_service.apply_bulk_discount(0, 100.0) == 100.00
        # 4 items: no discount
        assert discount_service.apply_bulk_discount(4, 100.0) == 100.00
        # 5 items: 5% discount
        assert discount_service.apply_bulk_discount(5, 100.0) == 95.00
        # 10 items: 10% discount
        assert discount_service.apply_bulk_discount(10, 100.0) == 90.00
        # 20 items: 15% discount
        assert discount_service.apply_bulk_discount(20, 100.0) == 85.00

    def test_apply_bulk_discount_handles_large_items_count(self, discount_service):
        # Very large items_count should apply max discount (15%)
        assert discount_service.apply_bulk_discount(1000, 200.0) == 170.00

    @pytest.mark.parametrize("invalid_obj", [{}, object()])
    def test_apply_bulk_discount_rejects_unexpected_object_types(self, discount_service, invalid_obj):
        with pytest.raises(TypeError):
            discount_service.apply_bulk_discount(invalid_obj, 100.0)
        with pytest.raises(TypeError):
            discount_service.apply_bulk_discount(5, invalid_obj)


class TestCalculateFinalPriceDiscountPercentageLimits:
    @pytest.mark.parametrize(
        "discount_percentage, expected_final",
        [
            (0, 100.00),
            (100, 30.00),  # limited to 70%, so 100% treated as 70%
            (-10, 100.00),  # negative treated as 0
            (70, 30.00),
            (71, 30.00),  # limited to 70%
        ],
    )
    def test_discount_percentage_limits(self, discount_service, discount_percentage, expected_final):
        final_price = discount_service.calculate_final_price(100.0, discount_percentage)
        assert final_price == expected_final


class TestCalculateFinalPriceNegativeBasePrice:
    def test_negative_base_price_raises_value_error(self, discount_service):
        with pytest.raises(ValueError) as excinfo:
            discount_service.calculate_final_price(-1.0, 10.0)
        assert "Preço base não pode ser negativo" in str(excinfo.value)