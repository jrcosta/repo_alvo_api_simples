import pytest
from pydantic import ValidationError
from app.schemas import UserUpdate


class TestUserUpdateSchema:
    def test_partial_update_all_fields_valid(self):
        data = {
            "name": "Valid Name",
            "email": "valid@example.com",
            "is_vip": True,
        }
        user_update = UserUpdate(**data)
        assert user_update.name == "Valid Name"
        assert user_update.email == "valid@example.com"
        assert user_update.is_vip is True

    def test_partial_update_only_email(self):
        data = {
            "email": "onlyemail@example.com",
        }
        user_update = UserUpdate(**data)
        assert user_update.name is None
        assert user_update.email == "onlyemail@example.com"
        assert user_update.is_vip is None

    @pytest.mark.parametrize("invalid_name", ["", "   "])
    def test_name_blank_string_should_raise_value_error(self, invalid_name):
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(name=invalid_name)
        errors = exc_info.value.errors()
        assert any("must not be blank" in e["msg"] for e in errors)

    def test_name_less_than_min_length_should_raise_validation_error(self):
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(name="ab")
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("name",) and e["type"] == "value_error.any_str.min_length" for e in errors)

    def test_email_invalid_should_raise_validation_error(self):
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(email="invalid-email")
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("email",) and e["type"].startswith("value_error.email") for e in errors)

    @pytest.mark.parametrize("invalid_is_vip", ["true", 1, "yes", 0])
    def test_is_vip_invalid_type_should_raise_validation_error(self, invalid_is_vip):
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(is_vip=invalid_is_vip)
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("is_vip",) and e["type"] == "type_error.bool" for e in errors)

    def test_payload_empty_all_fields_none(self):
        user_update = UserUpdate()
        assert user_update.name is None
        assert user_update.email is None
        assert user_update.is_vip is None

    def test_create_instance_all_fields_valid(self):
        user_update = UserUpdate(
            name="Valid Name",
            email="valid@example.com",
            is_vip=False,
        )
        assert user_update.name == "Valid Name"
        assert user_update.email == "valid@example.com"
        assert user_update.is_vip is False

    def test_create_instance_name_none(self):
        user_update = UserUpdate(name=None)
        assert user_update.name is None

    def test_create_instance_name_empty_string_raises_value_error(self):
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(name="")
        errors = exc_info.value.errors()
        assert any("must not be blank" in e["msg"] for e in errors)

    def test_create_instance_name_less_than_min_length(self):
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(name="ab")
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("name",) and e["type"] == "value_error.any_str.min_length" for e in errors)

    def test_create_instance_email_invalid_raises_validation_error(self):
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(email="not-an-email")
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("email",) and e["type"].startswith("value_error.email") for e in errors)

    def test_create_instance_is_vip_invalid_raises_validation_error(self):
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(is_vip="notbool")
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("is_vip",) and e["type"] == "type_error.bool" for e in errors)

    def test_create_instance_payload_empty(self):
        user_update = UserUpdate()
        assert user_update.name is None
        assert user_update.email is None
        assert user_update.is_vip is None


class TestRejectBlankNameValidator:
    from app.schemas import UserUpdate

    def test_reject_blank_name_with_none_passes(self):
        # Should not raise
        assert UserUpdate.reject_blank_name(None) is None

    @pytest.mark.parametrize("blank_value", ["", "   "])
    def test_reject_blank_name_with_blank_string_raises_value_error(self, blank_value):
        with pytest.raises(ValueError) as exc_info:
            UserUpdate.reject_blank_name(blank_value)
        assert str(exc_info.value) == "must not be blank"

    def test_reject_blank_name_with_valid_string_passes(self):
        valid_name = "Valid Name"
        assert UserUpdate.reject_blank_name(valid_name) == valid_name