import pytest
from pydantic import ValidationError
from app.schemas import UserCreate, UserResponse


class TestUserCreateSchema:

    def test_create_user_without_is_vip_should_default_to_false(self):
        user = UserCreate(name="Alice", email="alice@example.com")
        assert user.name == "Alice"
        assert user.email == "alice@example.com"
        assert user.is_vip is False

    def test_create_user_with_is_vip_true_should_set_true(self):
        user = UserCreate(name="Bob", email="bob@example.com", is_vip=True)
        assert user.is_vip is True

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
        assert any(e['loc'] == ('is_vip',) and e['type'].startswith('type_error') for e in errors)


class TestUserResponseSchema:

    def test_serialize_and_deserialize_user_response_with_is_vip_false(self):
        user_dict = {
            "id": 1,
            "name": "Eve",
            "email": "eve@example.com",
            "is_vip": False
        }
        user = UserResponse.model_validate(user_dict)
        assert user.id == 1
        assert user.name == "Eve"
        assert user.email == "eve@example.com"
        assert user.is_vip is False

        serialized = user.model_dump()
        assert serialized["is_vip"] is False

    def test_serialize_and_deserialize_user_response_with_is_vip_true(self):
        user_dict = {
            "id": 2,
            "name": "Frank",
            "email": "frank@example.com",
            "is_vip": True
        }
        user = UserResponse.model_validate(user_dict)
        assert user.is_vip is True

        serialized = user.model_dump()
        assert serialized["is_vip"] is True

    def test_user_response_without_is_vip_should_default_to_false(self):
        user_dict = {
            "id": 3,
            "name": "Grace",
            "email": "grace@example.com"
        }
        user = UserResponse.model_validate(user_dict)
        assert user.is_vip is False

    @pytest.mark.parametrize("invalid_value", ["yes", "no", 1, 0, None, "true", "false", [], {}])
    def test_user_response_with_invalid_is_vip_should_raise_validation_error(self, invalid_value):
        if isinstance(invalid_value, bool):
            pytest.skip("Boolean values are valid")
        user_dict = {
            "id": 4,
            "name": "Hank",
            "email": "hank@example.com",
            "is_vip": invalid_value
        }
        with pytest.raises(ValidationError) as exc_info:
            UserResponse.model_validate(user_dict)
        errors = exc_info.value.errors()
        assert any(e['loc'] == ('is_vip',) and e['type'].startswith('type_error') for e in errors)