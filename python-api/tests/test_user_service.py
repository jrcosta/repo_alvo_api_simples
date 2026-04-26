import pytest
from pydantic import ValidationError
from app.services.user_service import UserService
from app.schemas import UserCreate, UserResponse
import json


@pytest.fixture
def user_service():
    return UserService()


def test_create_user_without_is_vip_defaults_to_false():
    # Testa que ao criar UserCreate sem is_vip, o valor padrão é False
    payload = UserCreate(name="No VIP", email="novip@example.com")
    assert payload.is_vip is False


def test_create_user_with_is_vip_true_and_false_explicitly(user_service):
    # Criar usuário com is_vip True
    payload_vip = UserCreate(name="VIP User", email="vip@example.com", is_vip=True)
    user_vip = user_service.create_user(payload_vip)
    assert user_vip.is_vip is True
    assert user_vip.name == "VIP User"
    assert user_vip.email == "vip@example.com"

    # Criar usuário com is_vip False
    payload_non_vip = UserCreate(name="Non VIP User", email="nonvip@example.com", is_vip=False)
    user_non_vip = user_service.create_user(payload_non_vip)
    assert user_non_vip.is_vip is False
    assert user_non_vip.name == "Non VIP User"
    assert user_non_vip.email == "nonvip@example.com"


@pytest.mark.parametrize(
    "invalid_is_vip",
    [
        "true",  # string
        1,       # int
        None,    # None explicit
        0,       # int zero
        [],      # list
        {},      # dict
    ],
)
def test_create_user_with_invalid_is_vip_raises_validation_error(invalid_is_vip):
    # Testa que valores inválidos para is_vip no payload causam erro de validação
    with pytest.raises(ValidationError):
        UserCreate(name="Invalid VIP", email="invalidvip@example.com", is_vip=invalid_is_vip)


def test_user_create_serialization_and_deserialization_maintains_default_is_vip():
    # Criar UserCreate sem is_vip
    user_create = UserCreate(name="Serialize Test", email="serialize@example.com")
    # Serializar para JSON
    json_str = user_create.json()
    # Desserializar do JSON
    user_create_deserialized = UserCreate.parse_raw(json_str)
    # Verificar que is_vip é False após desserialização
    assert user_create_deserialized.is_vip is False


def test_create_user_with_is_vip_none_raises_validation_error():
    # Explicitamente passar is_vip=None deve gerar erro
    with pytest.raises(ValidationError):
        UserCreate(name="None VIP", email="nonevip@example.com", is_vip=None)


def test_create_user_with_is_vip_omitted_with_other_optional_fields():
    # Criar UserCreate omitindo is_vip e outros campos opcionais (se existirem)
    # Aqui só temos name e email obrigatórios, is_vip opcional
    user_create = UserCreate(name="Omit VIP", email="omitvip@example.com")
    assert user_create.is_vip is False


def test_create_user_without_is_vip_defaults_to_false_explicit_check():
    # Reforçar que ausência do campo is_vip não gera erro e atribui False
    user_data = {"name": "Test User", "email": "testuser@example.com"}
    user_create = UserCreate(**user_data)
    assert user_create.is_vip is False