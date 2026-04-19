import pytest
from app.services.user_service import UserService
from app.schemas import UserCreate, UserResponse

@pytest.fixture
def user_service():
    service = UserService()
    return service

def test_user_service_reset(user_service) -> None:
    user_service.create_user(UserCreate(name="Test User", email="test@example.com"))
    assert len(user_service.list_users()) == 3  # Deve ter 3 usuários após a criação

    user_service.reset()
    users_after_reset = user_service.list_users()
    assert len(users_after_reset) == 2  # Deve ter 2 usuários após o reset

    # Validar conteúdo dos usuários remanescentes após reset
    expected_seed_users = [
        UserResponse(id=1, name="Ana Silva", email="ana@example.com"),
        UserResponse(id=2, name="Bruno Lima", email="bruno@example.com"),
    ]
    # Comparar usuários por id, name e email
    for expected_user in expected_seed_users:
        assert any(
            u.id == expected_user.id and u.name == expected_user.name and u.email == expected_user.email
            for u in users_after_reset
        ), f"Usuário seed esperado não encontrado: {expected_user}"

    assert user_service._next_id == 3  # O próximo ID deve ser 3

def test_user_service_create_user_after_reset(user_service) -> None:
    user_service.reset()
    new_user = user_service.create_user(UserCreate(name="New User", email="newuser@example.com"))
    assert new_user.id == 3  # O ID atribuído deve ser 3 após o reset
    assert len(user_service.list_users()) == 3  # Deve ter 3 usuários após a criação

def test_user_service_multiple_resets_do_not_alter_state(user_service) -> None:
    user_service.create_user(UserCreate(name="User1", email="user1@example.com"))
    user_service.reset()
    first_reset_users = user_service.list_users()
    first_next_id = user_service._next_id

    user_service.reset()
    second_reset_users = user_service.list_users()
    second_next_id = user_service._next_id

    assert first_reset_users == second_reset_users, "Usuários após múltiplos resets devem ser iguais"
    assert first_next_id == second_next_id == 3, "O _next_id deve ser 3 após múltiplos resets"

def test_user_service_create_multiple_users_after_reset_increments_id_correctly(user_service) -> None:
    user_service.reset()
    user1 = user_service.create_user(UserCreate(name="User One", email="user1@example.com"))
    user2 = user_service.create_user(UserCreate(name="User Two", email="user2@example.com"))
    user3 = user_service.create_user(UserCreate(name="User Three", email="user3@example.com"))

    assert user1.id == 3
    assert user2.id == 4
    assert user3.id == 5
    assert len(user_service.list_users()) == 5

def test_user_service_reset_on_empty_service(user_service) -> None:
    # Limpar todos os usuários manualmente para simular serviço vazio
    user_service._users.clear()
    user_service._next_id = 1

    assert len(user_service.list_users()) == 0
    assert user_service._next_id == 1

    user_service.reset()
    users_after_reset = user_service.list_users()
    assert len(users_after_reset) == 2  # Deve restaurar os usuários seed
    assert user_service._next_id == 3


def test_user_service_create_user_raises_error_after_reset_with_invalid_data(user_service) -> None:
    user_service.reset()
    with pytest.raises(Exception):
        # Criar usuário com dados inválidos (nome vazio)
        user_service.create_user(UserCreate(name="", email="invalid@example.com"))

    with pytest.raises(Exception):
        # Criar usuário com dados inválidos (email vazio)
        user_service.create_user(UserCreate(name="Invalid User", email=""))

def test_user_service_reset_id_increment_after_multiple_resets(user_service) -> None:
    user_service.reset()
    user_service.create_user(UserCreate(name="User A", email="a@example.com"))
    user_service.reset()
    user_service.create_user(UserCreate(name="User B", email="b@example.com"))
    user_service.reset()
    user_service.create_user(UserCreate(name="User C", email="c@example.com"))

    last_user = user_service.list_users()[-1]
    assert last_user.id == 3
    assert user_service._next_id == 4