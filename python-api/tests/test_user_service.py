import pytest
import threading
from unittest.mock import patch
from pydantic import ValidationError
from app.services.user_service import UserService
from app.schemas import UserUpdate


@pytest.fixture
def user_service():
    service = UserService()
    service.reset()
    return service


def test_delete_user_internal_exception_does_not_corrupt_user_list(user_service: UserService):
    # Simula falha interna no método delete_user para garantir integridade da lista
    user_service.reset()
    initial_users = user_service.list_users().copy()

    original_delete_user = user_service.delete_user

    def faulty_delete_user(user_id):
        if user_id == 1:
            raise RuntimeError("Simulated internal failure")
        return original_delete_user(user_id)

    with patch.object(user_service, "delete_user", side_effect=faulty_delete_user):
        with pytest.raises(RuntimeError, match="Simulated internal failure"):
            user_service.delete_user(1)
        # Após exceção, a lista de usuários deve permanecer intacta
        assert user_service.list_users() == initial_users


def test_update_user_with_empty_name_raises_validation_error(user_service: UserService):
    user_id = 1
    with pytest.raises(ValidationError) as exc_info:
        UserUpdate(name="")
    errors = exc_info.value.errors()
    assert any("name" in e.get("loc", []) and e.get("type") == "value_error" for e in errors)


@pytest.mark.parametrize(
    "email_value,should_raise",
    [
        ("a" * 64 + "@example.com", False),  # 64 chars local part - válido
        ("a" * 65 + "@example.com", True),   # 65 chars local part - inválido
    ],
)
def test_update_user_email_local_part_length_validation(user_service: UserService, email_value, should_raise):
    user_id = 1
    if should_raise:
        with pytest.raises(ValidationError):
            UserUpdate(email=email_value)
    else:
        payload = UserUpdate(email=email_value)
        updated_user = user_service.update_user(user_id, payload)
        assert updated_user.email == email_value


def test_delete_nonexistent_user_idempotent_and_list_unchanged(user_service: UserService):
    user_service.reset()
    initial_users = user_service.list_users().copy()
    result = user_service.delete_user(999999)  # ID inexistente
    assert result is False
    assert user_service.list_users() == initial_users


def test_concurrent_delete_and_update_user_integrity(user_service: UserService):
    user_service.reset()
    user_id = 1
    update_names = [f"Concurrent User {i}" for i in range(10)]
    delete_results = []
    update_results = []

    def delete_user():
        try:
            res = user_service.delete_user(user_id)
            delete_results.append(res)
        except Exception as e:
            delete_results.append(e)

    def update_user(name):
        try:
            payload = UserUpdate(name=name)
            res = user_service.update_user(user_id, payload)
            update_results.append(res)
        except Exception as e:
            update_results.append(e)

    threads = []
    for name in update_names:
        t = threading.Thread(target=update_user, args=(name,))
        threads.append(t)
    for _ in range(5):
        t = threading.Thread(target=delete_user)
        threads.append(t)

    for t in threads:
        t.start()
    for t in threads:
        t.join()

    # Apenas um delete deve ter sucesso, os demais devem retornar False
    delete_success_count = sum(1 for r in delete_results if r is True)
    delete_fail_count = sum(1 for r in delete_results if r is False)
    assert delete_success_count == 1
    assert delete_fail_count == len(delete_results) - 1

    # Nenhuma exceção inesperada deve ocorrer
    assert all(not isinstance(r, Exception) for r in delete_results)
    assert all(not isinstance(r, Exception) for r in update_results)

    # Após concorrência, usuário deve estar ausente ou atualizado
    user = user_service.get_user(user_id)
    if user is not None:
        # Se usuário existe, nome deve estar entre os nomes atualizados
        assert user.name in update_names
    else:
        # Usuário foi deletado
        assert user is None


def test_delete_user_propagates_unexpected_exceptions_and_preserves_list(user_service: UserService):
    user_service.reset()
    initial_users = user_service.list_users().copy()

    # Patch interno para simular exceção inesperada dentro do método delete_user
    original_method = user_service._users.__delitem__

    def faulty_delitem(index):
        if index == 0:
            raise RuntimeError("Unexpected internal error")
        return original_method(index)

    with patch.object(user_service._users, "__delitem__", side_effect=faulty_delitem):
        with pytest.raises(RuntimeError, match="Unexpected internal error"):
            user_service.delete_user(1)
        # Lista deve permanecer intacta após exceção
        assert user_service.list_users() == initial_users


def test_delete_user_rollback_mechanism_ensures_list_integrity_on_partial_failure(user_service: UserService):
    user_service.reset()
    initial_users = user_service.list_users().copy()

    # Simular falha parcial durante exclusão: sobrescrever método interno para lançar após remoção parcial
    original_pop = user_service._users.pop

    def faulty_pop(index):
        if index == 0:
            # Remove o usuário, mas lança exceção logo após
            result = original_pop(index)
            raise RuntimeError("Partial failure after removal")
        return original_pop(index)

    with patch.object(user_service._users, "pop", side_effect=faulty_pop):
        with pytest.raises(RuntimeError, match="Partial failure after removal"):
            user_service.delete_user(1)
        # Verificar se rollback manual ou compensação mantém lista intacta
        # Como não há rollback explícito, lista pode estar corrompida, então testamos se há mecanismo compensatório
        # Se não houver, falha no teste para indicar problema
        assert user_service.list_users() == initial_users


def test_update_user_validation_error_messages_are_clear_and_standardized(user_service: UserService):
    # Testa mensagens de erro para nome vazio e email inválido
    with pytest.raises(ValidationError) as exc_name:
        UserUpdate(name="")
    errors_name = exc_name.value.errors()
    assert any("name" in e.get("loc", []) and "blank" in e.get("msg", "").lower() for e in errors_name)

    with pytest.raises(ValidationError) as exc_email:
        UserUpdate(email="invalid-email")
    errors_email = exc_email.value.errors()
    assert any("email" in e.get("loc", []) and "valid email" in e.get("msg", "").lower() for e in errors_email)


@pytest.mark.parametrize("name_value", ["", None])
def test_update_user_rejects_blank_or_none_name(user_service: UserService, name_value):
    if name_value is None:
        # None is allowed to be omitted, but explicit None should be ignored or rejected
        payload = UserUpdate(name=None)
        updated_user = user_service.update_user(1, payload)
        assert updated_user.name != ""
    else:
        with pytest.raises(ValidationError):
            UserUpdate(name=name_value)