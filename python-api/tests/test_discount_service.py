import pytest
from app.services.discount_service import DiscountService


@pytest.fixture
def discount_service():
    return DiscountService()


class TestCalculateFinalPrice:
    def test_basic_percentage_discount(self, discount_service):
        # base_price=200.0, discount_percentage=20.0, no coupon, not VIP
        final_price = discount_service.calculate_final_price(200.0, 20.0)
        assert final_price == 160.00

    def test_discount_percentage_limited_to_70_percent(self, discount_service):
        # base_price=100.0, discount_percentage=80.0 (should limit to 70%)
        final_price = discount_service.calculate_final_price(100.0, 80.0)
        assert final_price == 30.00

    def test_discount_percentage_negative_treated_as_zero(self, discount_service):
        # base_price=100.0, discount_percentage=-10.0 (treated as 0)
        final_price = discount_service.calculate_final_price(100.0, -10.0)
        assert final_price == 100.00

    def test_vip_discount_applied_correctly(self, discount_service):
        # base_price=100.0, discount_percentage=10.0, is_vip=True
        # 100 - 10% = 90.0; VIP 5% on 90.0 = 85.5
        final_price = discount_service.calculate_final_price(100.0, 10.0, is_vip=True)
        assert final_price == 85.50

    def test_coupon_quero10_applied_correctly(self, discount_service):
        # base_price=50.0, discount_percentage=0, coupon_code="QUERO10"
        final_price = discount_service.calculate_final_price(50.0, 0, coupon_code="QUERO10")
        assert final_price == 40.00

    def test_coupon_quero20_not_applied_if_base_price_below_100(self, discount_service):
        # base_price=90.0, discount_percentage=0, coupon_code="QUERO20"
        final_price = discount_service.calculate_final_price(90.0, 0, coupon_code="QUERO20")
        assert final_price == 90.00

    def test_coupon_quero20_applied_if_base_price_above_100(self, discount_service):
        # base_price=150.0, discount_percentage=0, coupon_code="QUERO20"
        final_price = discount_service.calculate_final_price(150.0, 0, coupon_code="QUERO20")
        assert final_price == 130.00

    def test_base_price_negative_raises_value_error(self, discount_service):
        with pytest.raises(ValueError):
            discount_service.calculate_final_price(-10.0, 10.0)

    def test_final_price_never_negative(self, discount_service):
        # base_price=10.0, discount_percentage=70.0, coupon_code="QUERO10", is_vip=True
        # Calculation: 10 - 7 = 3; VIP 5% on 3 = 2.85; coupon -10 = -7.15 -> adjusted to 0.00
        final_price = discount_service.calculate_final_price(10.0, 70.0, coupon_code="QUERO10", is_vip=True)
        assert final_price == 0.00

    def test_combination_of_discounts(self, discount_service):
        # base_price=200.0, discount_percentage=50.0, coupon_code="QUERO10", is_vip=True
        # 200 - 50% = 100; VIP 5% on 100 = 95; coupon -10 = 85
        final_price = discount_service.calculate_final_price(200.0, 50.0, coupon_code="QUERO10", is_vip=True)
        assert final_price == 85.00

    def test_invalid_coupon_ignored(self, discount_service):
        # base_price=100.0, discount_percentage=10.0, coupon_code="INVALID"
        # Coupon ignored, only 10% discount applied
        final_price = discount_service.calculate_final_price(100.0, 10.0, coupon_code="INVALID")
        assert final_price == 90.00

    def test_coupon_code_case_sensitivity(self, discount_service):
        # coupon_code in lowercase should be ignored
        final_price = discount_service.calculate_final_price(50.0, 0, coupon_code="quero10")
        assert final_price == 50.00

    def test_discount_percentage_at_limit_70_percent(self, discount_service):
        # base_price=100.0, discount_percentage=70.0
        final_price = discount_service.calculate_final_price(100.0, 70.0)
        assert final_price == 30.00

    def test_discount_percentage_just_below_limit(self, discount_service):
        # base_price=100.0, discount_percentage=69.0, coupon_code="QUERO20"
        # 100 - 69% = 31; coupon applied -20 = 11
        final_price = discount_service.calculate_final_price(100.0, 69.0, coupon_code="QUERO20")
        # Since base_price=100.0 not > 100, coupon QUERO20 not applied, so final price = 31.0
        assert final_price == 31.00

        # Now with base_price=150.0, discount_percentage=69.0, coupon_code="QUERO20"
        # 150 - 69% = 46.5; coupon applied -20 = 26.5
        final_price2 = discount_service.calculate_final_price(150.0, 69.0, coupon_code="QUERO20")
        assert final_price2 == 26.50

    def test_vip_discount_not_applied_when_false_or_none(self, discount_service):
        # is_vip=False
        final_price = discount_service.calculate_final_price(100.0, 10.0, is_vip=False)
        assert final_price == 90.00

        # is_vip=None (should be treated as False)
        final_price_none = discount_service.calculate_final_price(100.0, 10.0, is_vip=None)
        assert final_price_none == 90.00

    def test_price_final_rounded_to_two_decimals(self, discount_service):
        # base_price=123.456, discount_percentage=12.345
        final_price = discount_service.calculate_final_price(123.456, 12.345)
        # Calculate expected manually:
        # discount = 123.456 * 0.12345 = 15.234
        # price after discount = 123.456 - 15.234 = 108.222
        # rounded to 2 decimals = 108.22
        assert final_price == 108.22

    def test_invalid_types_raise_error(self, discount_service):
        # Passing string instead of float should raise TypeError or ValueError
        with pytest.raises(TypeError):
            discount_service.calculate_final_price("100", 10.0)
        with pytest.raises(TypeError):
            discount_service.calculate_final_price(100.0, "10")
        with pytest.raises(TypeError):
            discount_service.calculate_final_price(100.0, 10.0, coupon_code=123)
        with pytest.raises(TypeError):
            discount_service.calculate_final_price(100.0, 10.0, is_vip="yes")


class TestApplyBulkDiscount:
    def test_bulk_discount_no_discount_for_less_than_5_items(self, discount_service):
        # items_count=0, total_value=100.0 -> factor=1.0
        price = discount_service.apply_bulk_discount(0, 100.0)
        assert price == 100.00

        # items_count=4, total_value=100.0 -> factor=1.0
        price = discount_service.apply_bulk_discount(4, 100.0)
        assert price == 100.00

    def test_bulk_discount_5_to_9_items(self, discount_service):
        # items_count=5, total_value=100.0 -> factor=0.95
        price = discount_service.apply_bulk_discount(5, 100.0)
        assert price == 95.00

        # items_count=9, total_value=200.0 -> factor=0.95
        price = discount_service.apply_bulk_discount(9, 200.0)
        assert price == 190.00

    def test_bulk_discount_10_to_19_items(self, discount_service):
        # items_count=10, total_value=100.0 -> factor=0.90
        price = discount_service.apply_bulk_discount(10, 100.0)
        assert price == 90.00

        # items_count=19, total_value=300.0 -> factor=0.90
        price = discount_service.apply_bulk_discount(19, 300.0)
        assert price == 270.00

    def test_bulk_discount_20_or_more_items(self, discount_service):
        # items_count=20, total_value=100.0 -> factor=0.85
        price = discount_service.apply_bulk_discount(20, 100.0)
        assert price == 85.00

        # items_count=100, total_value=1000.0 -> factor=0.85
        price = discount_service.apply_bulk_discount(100, 1000.0)
        assert price == 850.00

    def test_bulk_discount_negative_and_zero_values(self, discount_service):
        # items_count=0, total_value=0.0 -> factor=1.0, price=0.0
        price = discount_service.apply_bulk_discount(0, 0.0)
        assert price == 0.00

        # items_count=-5, total_value=100.0 -> factor=1.0 (per current implementation)
        price = discount_service.apply_bulk_discount(-5, 100.0)
        assert price == 100.00

        # items_count=5, total_value=0.0 -> factor=0.95, price=0.0
        price = discount_service.apply_bulk_discount(5, 0.0)
        assert price == 0.00

        # items_count=-10, total_value=-100.0 -> factor=1.0, price=-100.0 rounded to -100.00
        price = discount_service.apply_bulk_discount(-10, -100.0)
        assert price == -100.00

    def test_bulk_discount_rounding_to_two_decimals(self, discount_service):
        # items_count=5, total_value=123.456 -> factor=0.95
        price = discount_service.apply_bulk_discount(5, 123.456)
        # 123.456 * 0.95 = 117.2832 rounded to 117.28
        assert price == 117.28