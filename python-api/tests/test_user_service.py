import pytest
from pydantic import ValidationError
from app.services.user_service import UserService
from app.schemas import UserCreate, UserResponse


@pytest.fixture
def user_service():
    return UserService()


def test_create_user_sets_is_vip_correctly(user_service):
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


def test_reset_initializes_seed_users_with_correct_is_vip(user_service):
    user_service.reset()
    users = user_service.list_users()
    assert len(users) == 2

    expected_seed_users = [
        UserResponse(id=1, name="Ana Silva", email="ana@example.com", is_vip=True),
        UserResponse(id=2, name="Bruno Lima", email="bruno@example.com", is_vip=False),
    ]

    for expected_user in expected_seed_users:
        matched_users = [
            u for u in users
            if u.id == expected_user.id and u.name == expected_user.name and u.email == expected_user.email and u.is_vip == expected_user.is_vip
        ]
        assert len(matched_users) == 1, f"Seed user with id {expected_user.id} and is_vip={expected_user.is_vip} not found"


def test_list_users_contains_is_vip_for_all_users(user_service):
    user_service.reset()
    # Criar usuários adicionais para garantir lista maior
    user_service.create_user(UserCreate(name="User1", email="user1@example.com", is_vip=True))
    user_service.create_user(UserCreate(name="User2", email="user2@example.com", is_vip=False))

    users = user_service.list_users()
    assert len(users) >= 4
    for user in users:
        assert hasattr(user, "is_vip")
        assert isinstance(user.is_vip, bool)


def test_create_user_without_is_vip_raises_validation_error():
    # Tentativa de criar UserCreate sem is_vip deve falhar (campo obrigatório presumido)
    with pytest.raises(ValidationError):
        UserCreate(name="No VIP", email="novip@example.com")  # is_vip ausente


def test_get_user_and_find_by_email_return_user_with_correct_is_vip(user_service):
    user_service.reset()
    # Buscar usuário seed por id
    user1 = user_service.get_user(1)
    assert user1 is not None
    assert user1.is_vip is True

    user2 = user_service.get_user(2)
    assert user2 is not None
    assert user2.is_vip is False

    # Buscar usuário seed por email
    user_email_1 = user_service.find_by_email("ana@example.com")
    assert user_email_1 is not None
    assert user_email_1.is_vip is True

    user_email_2 = user_service.find_by_email("bruno@example.com")
    assert user_email_2 is not None
    assert user_email_2.is_vip is False


@pytest.mark.parametrize(
    "invalid_is_vip",
    [
        "true",  # string
        1,       # int
        None,    # null
        0,       # int zero
        [],      # list
        {},      # dict
    ],
)
def test_create_user_with_invalid_is_vip_raises_validation_error(invalid_is_vip):
    # Testa que valores inválidos para is_vip no payload causam erro de validação
    with pytest.raises(ValidationError):
        UserCreate(name="Invalid VIP", email="invalidvip@example.com", is_vip=invalid_is_vip)