import pytest
from pydantic import ValidationError
from app.schemas import UserUpdate, reject_blank_name


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
        # Assert mais específico para evitar falsos positivos
        assert any(
            e["loc"] == ("name",) and "blank" in e["msg"].lower() and e["type"] == "value_error"
            for e in errors
        )

    def test_name_less_than_min_length_should_raise_validation_error(self):
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(name="ab")
        errors = exc_info.value.errors()
        assert any(
            e["loc"] == ("name",) and "too_short" in e["type"]
            for e in errors
        )

    def test_email_invalid_should_raise_validation_error(self):
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(email="invalid-email")
        errors = exc_info.value.errors()
        assert any(
            e["loc"] == ("email",) and e["type"].startswith("value_error")
            for e in errors
        )

    @pytest.mark.parametrize("invalid_is_vip", ["true", 1, "yes", 0])
    def test_is_vip_invalid_type_should_raise_validation_error(self, invalid_is_vip):
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(is_vip=invalid_is_vip)
        errors = exc_info.value.errors()
        assert any(
            e["loc"] == ("is_vip",) and "bool" in e["type"]
            for e in errors
        )

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
        assert any(
            e["loc"] == ("name",) and "blank" in e["msg"].lower() and e["type"] == "value_error"
            for e in errors
        )

    def test_create_instance_name_less_than_min_length(self):
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(name="ab")
        errors = exc_info.value.errors()
        assert any(
            e["loc"] == ("name",) and "too_short" in e["type"]
            for e in errors
        )

    def test_create_instance_email_invalid_raises_validation_error(self):
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(email="not-an-email")
        errors = exc_info.value.errors()
        assert any(
            e["loc"] == ("email",) and e["type"].startswith("value_error")
            for e in errors
        )

    def test_create_instance_is_vip_invalid_raises_validation_error(self):
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(is_vip="notbool")
        errors = exc_info.value.errors()
        assert any(
            e["loc"] == ("is_vip",) and "bool" in e["type"]
            for e in errors
        )

    def test_create_instance_payload_empty(self):
        user_update = UserUpdate()
        assert user_update.name is None
        assert user_update.email is None
        assert user_update.is_vip is None

    def test_create_instance_with_extra_fields_should_raise_validation_error(self):
        # Testar que campos extras não são aceitos
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(name="Valid Name", email="valid@example.com", is_vip=True, extra_field="not_allowed")
        errors = exc_info.value.errors()
        # Pydantic v2 usa "extra_forbidden" em vez de "value_error.extra"
        assert any(
            e["loc"] == ("extra_field",) and e["type"] == "extra_forbidden"
            for e in errors
        )

    def test_create_instance_with_null_fields(self):
        # Testar que campos explicitamente nulos são aceitos (None)
        user_update = UserUpdate(name=None, email=None, is_vip=None)
        assert user_update.name is None
        assert user_update.email is None
        assert user_update.is_vip is None

    def test_name_length_boundaries(self):
        # Testar nome com exatamente 3 caracteres (limite mínimo)
        user_update = UserUpdate(name="abc")
        assert user_update.name == "abc"

        # Testar nome muito longo (ex: 256 caracteres)
        long_name = "a" * 256
        user_update = UserUpdate(name=long_name)
        assert user_update.name == long_name

    def test_email_length_boundaries(self):
        # RFC 5321: local part max 64 chars, total max 254 chars
        # Testar email com local part no limite (64 chars)
        local_part = "a" * 64
        email = f"{local_part}@example.com"
        user_update = UserUpdate(email=email)
        assert user_update.email == email

        # Testar email com caracteres especiais válidos
        special_email = "user.name+tag+sorting@example.com"
        user_update = UserUpdate(email=special_email)
        assert user_update.email == special_email

    @pytest.mark.parametrize("blank_variant", ["", " ", "\t", "\n", "\u00A0"])  # includes non-breaking space
    def test_reject_blank_name_validator_rejects_blank_and_unicode_spaces(self, blank_variant):
        with pytest.raises(ValueError) as exc_info:
            reject_blank_name(blank_variant)
        assert "must not be blank" in str(exc_info.value).lower()

    def test_reject_blank_name_validator_accepts_none_and_valid_strings(self):
        # None should be accepted (no error)
        assert reject_blank_name(None) is None
        # Valid string with internal spaces accepted
        assert reject_blank_name("John Doe") == "John Doe"
        # Valid string with leading/trailing spaces trimmed or accepted as is
        assert reject_blank_name("  John  ") == "  John  "

    def test_update_partial_various_combinations(self):
        # Testar combinações variadas de campos presentes e ausentes
        data_sets = [
            {"name": "Alice"},
            {"email": "alice@example.com"},
            {"is_vip": True},
            {"name": "Bob", "email": "bob@example.com"},
            {"email": "carol@example.com", "is_vip": False},
            {"name": "Dave", "is_vip": True},
            {},
        ]
        for data in data_sets:
            user_update = UserUpdate(**data)
            for field in ["name", "email", "is_vip"]:
                if field in data:
                    assert getattr(user_update, field) == data[field]
                else:
                    assert getattr(user_update, field) is None

    def test_error_messages_contain_expected_keywords(self):
        # Testar que mensagens de erro contêm palavras-chave esperadas para evitar regressões
        with pytest.raises(ValidationError) as exc_info:
            UserUpdate(name="", email="invalid-email", is_vip="notbool")
        errors = exc_info.value.errors()

        # Deve conter erro de nome em branco
        assert any(
            e["loc"] == ("name",) and "blank" in e["msg"].lower() and e["type"] == "value_error"
            for e in errors
        )
        # Deve conter erro de email inválido
        assert any(
            e["loc"] == ("email",) and e["type"].startswith("value_error")
            for e in errors
        )
        # Deve conter erro de tipo inválido para is_vip
        assert any(
            e["loc"] == ("is_vip",) and "bool" in e["type"]
            for e in errors
        )