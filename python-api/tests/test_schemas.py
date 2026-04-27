import pytest
from pydantic import ValidationError
from app.schemas import UserUpdate

class TestUserUpdateSchema:
    def test_update_all_fields_valid(self):
        # Testar atualização com todos os campos válidos
        data = {
            "name": "Ana Atualizada",
            "email": "ana@ex.com",
            "is_vip": False,
        }
        user_update = UserUpdate(**data)
        assert user_update.name == "Ana Atualizada"
        assert user_update.email == "ana@ex.com"
        assert user_update.is_vip is False

    def test_update_partial_fields_valid(self):
        # Testar atualização parcial com apenas um campo
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
        assert any("blank" in e["msg"].lower() for e in errors)

    def test_name_less_than_min_length_should_raise_validation_error(self):
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(name="ab")
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("name",) and "too_short" in e["type"] for e in errors)

    def test_email_invalid_should_raise_validation_error(self):
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(email="invalid-email")
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("email",) for e in errors)

    @pytest.mark.parametrize("invalid_is_vip", ["true", 1, "yes", 0])
    def test_is_vip_invalid_type_should_raise_validation_error(self, invalid_is_vip):
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(is_vip=invalid_is_vip)
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("is_vip",) and "bool" in e["type"] for e in errors)

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
        assert any("blank" in e["msg"].lower() for e in errors)

    def test_create_instance_name_less_than_min_length(self):
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(name="ab")
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("name",) and "too_short" in e["type"] for e in errors)

    def test_create_instance_email_invalid_raises_validation_error(self):
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(email="not-an-email")
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("email",) for e in errors)

    def test_create_instance_is_vip_invalid_raises_validation_error(self):
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(is_vip="notbool")
        errors = exc_info.value.errors()
        assert any(e["loc"] == ("is_vip",) and "bool" in e["type"] for e in errors)

    def test_create_instance_payload_empty(self):
        user_update = UserUpdate()
        assert user_update.name is None
        assert user_update.email is None
        assert user_update.is_vip is None