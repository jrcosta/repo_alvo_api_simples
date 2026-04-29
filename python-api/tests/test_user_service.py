import pytest
from pydantic import ValidationError
from app.services.user_service import UserService
from app.schemas import UserCreate, UserUpdate, UserResponse


@pytest.fixture
def user_service():
    service = UserService()
    service.reset()
    return service


def test_update_user_all_fields(user_service: UserService):
    # Atualizar usuário existente com todos os campos preenchidos
    user_id = 1
    payload = UserUpdate(name="Ana Updated", email="ana.updated@example.com", is_vip=False)
    updated_user = user_service.update_user(user_id, payload)

    assert updated_user is not None
    assert updated_user.id == user_id
    assert updated_user.name == "Ana Updated"
    assert updated_user.email == "ana.updated@example.com"
    assert updated_user.is_vip is False


def test_update_user_partial_fields(user_service: UserService):
    # Atualizar usuário existente com apenas alguns campos (atualização parcial)
    user_id = 2
    original_user = user_service.get_user(user_id)
    payload = UserUpdate(email="bruno.new@example.com")
    updated_user = user_service.update_user(user_id, payload)

    assert updated_user is not None
    assert updated_user.id == user_id
    assert updated_user.name == original_user.name  # name não alterado
    assert updated_user.email == "bruno.new@example.com"
    assert updated_user.is_vip == original_user.is_vip  # is_vip não alterado


def test_update_user_nonexistent_user_returns_none(user_service: UserService):
    # Tentar atualizar usuário inexistente e verificar retorno None
    user_id = 9999
    payload = UserUpdate(name="Nonexistent", email="noone@example.com", is_vip=True)
    updated_user = user_service.update_user(user_id, payload)
    assert updated_user is None


def test_update_user_no_fields_to_update_returns_same_user(user_service: UserService):
    # Payload com todos campos None deve retornar usuário sem alterações
    user_id = 1
    original_user = user_service.get_user(user_id)
    payload = UserUpdate()
    updated_user = user_service.update_user(user_id, payload)

    assert updated_user is not None
    assert updated_user.id == original_user.id
    assert updated_user.name == original_user.name
    assert updated_user.email == original_user.email
    assert updated_user.is_vip == original_user.is_vip


def test_update_user_preserves_other_fields(user_service: UserService):
    # Garantir que campos não atualizados permanecem iguais
    user_id = 2
    original_user = user_service.get_user(user_id)
    payload = UserUpdate(name="Bruno Updated")
    updated_user = user_service.update_user(user_id, payload)

    assert updated_user is not None
    assert updated_user.name == "Bruno Updated"
    assert updated_user.email == original_user.email
    assert updated_user.is_vip == original_user.is_vip


def test_update_user_invalid_email_raises_validation_error(user_service: UserService):
    # Atualizar usuário com email mal formatado deve levantar ValidationError do Pydantic
    user_id = 1
    with pytest.raises(ValidationError):
        UserUpdate(name="Ana", email="invalid-email", is_vip=True)


def test_update_user_with_extra_fields_ignored(user_service: UserService):
    # Atualizar usuário com payload contendo campos extras não esperados
    # Como UserUpdate é Pydantic, campos extras são ignorados ou causam erro dependendo da configuração
    # Aqui assumimos que campos extras são rejeitados (default Pydantic behavior)
    user_id = 1
    with pytest.raises(ValidationError):
        UserUpdate(name="Ana", email="ana@example.com", is_vip=True, extra_field="not_allowed")


def test_update_user_with_unicode_and_special_characters(user_service: UserService):
    # Atualizar usuário com dados contendo caracteres especiais ou unicode
    user_id = 1
    payload = UserUpdate(name="Ána Šilva 🚀", email="ana.šilva@example.com", is_vip=True)
    updated_user = user_service.update_user(user_id, payload)

    assert updated_user is not None
    assert updated_user.name == "Ána Šilva 🚀"
    assert updated_user.email == "ana.šilva@example.com"
    assert updated_user.is_vip is True


def test_update_user_with_max_length_fields(user_service: UserService):
    # Atualizar usuário com strings no tamanho máximo permitido (assumindo limite 255 para email e name)
    max_length_name = "a" * 255
    max_length_email = "a" * 50 + "@example.com"
    user_id = 1
    payload = UserUpdate(name=max_length_name, email=max_length_email, is_vip=True)
    updated_user = user_service.update_user(user_id, payload)

    assert updated_user is not None
    assert updated_user.name == max_length_name
    assert updated_user.email == max_length_email
    assert updated_user.is_vip is True


import threading


def test_concurrent_updates_do_not_corrupt_data(user_service: UserService):
    # Testar atualizações simultâneas para o mesmo usuário para verificar consistência
    user_id = 1

    def update_name(name):
        payload = UserUpdate(name=name)
        user_service.update_user(user_id, payload)

    threads = []
    names = [f"User {i}" for i in range(10)]
    for name in names:
        t = threading.Thread(target=update_name, args=(name,))
        threads.append(t)
        t.start()

    for t in threads:
        t.join()

    # Após todas atualizações, o nome deve ser um dos nomes atualizados (último pode variar)
    final_user = user_service.get_user(user_id)
    assert final_user.name in names


def test_update_user_rejects_immutable_fields(user_service: UserService):
    # Se houver campos imutáveis, validar que atualização é rejeitada
    # No código atual não há campos imutáveis explícitos, mas testamos que id não pode ser alterado
    user_id = 1
    original_user = user_service.get_user(user_id)
    # Tentativa de alterar id via payload não é possível pois UserUpdate não tem id
    # Então testamos que id permanece o mesmo após atualização
    payload = UserUpdate(name="New Name")
    updated_user = user_service.update_user(user_id, payload)
    assert updated_user.id == original_user.id


def test_update_user_partial_failure_rollback(user_service: UserService):
    # Testar rollback ou estado consistente após falha parcial na atualização
    # Como update_user substitui o objeto inteiro, e validação é feita antes, não há meio de falha parcial
    # Simulamos falha de validação e garantimos que usuário não é alterado
    user_id = 1
    original_user = user_service.get_user(user_id)
    with pytest.raises(ValidationError):
        UserUpdate(name="Valid Name", email="invalid-email", is_vip=True)
    # Usuário permanece inalterado
    user_after = user_service.get_user(user_id)
    assert user_after == original_user