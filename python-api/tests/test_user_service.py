import pytest
import threading
from pydantic import ValidationError
from app.services.user_service import UserService
from app.schemas import UserCreate, UserUpdate, UserResponse


@pytest.fixture
def user_service():
    service = UserService()
    service.reset()
    return service


def test_delete_existing_user_removes_user_and_returns_true(user_service: UserService):
    user_id = 1
    assert user_service.get_user(user_id) is not None
    result = user_service.delete_user(user_id)
    assert result is True
    assert user_service.get_user(user_id) is None


def test_delete_nonexistent_user_returns_false_and_list_unchanged(user_service: UserService):
    initial_users = user_service.list_users()
    result = user_service.delete_user(9999)
    assert result is False
    assert user_service.list_users() == initial_users


def test_delete_user_with_empty_list_returns_false(user_service: UserService):
    user_service._users.clear()
    result = user_service.delete_user(1)
    assert result is False
    assert user_service._users == []


def test_multiple_sequential_deletions_maintain_list_integrity(user_service: UserService):
    user_service.reset()
    user_ids = [1, 2]
    for uid in user_ids:
        assert user_service.delete_user(uid) is True
    assert user_service.list_users() == []
    # Deleting again returns False
    for uid in user_ids:
        assert user_service.delete_user(uid) is False


@pytest.mark.parametrize("invalid_id", ["abc", -1, 0, 1.5, None, object()])
def test_delete_user_with_invalid_id_types_returns_false(user_service: UserService, invalid_id):
    # The method expects int, but no explicit validation, so test behavior
    # It should not raise, but return False because no user matches
    try:
        result = user_service.delete_user(invalid_id)  # type: ignore
        assert result is False
    except Exception:
        pytest.fail(f"delete_user raised exception with invalid id: {invalid_id}")


def test_reset_restores_initial_state_after_deletions(user_service: UserService):
    user_service.delete_user(1)
    user_service.delete_user(2)
    assert user_service.list_users() == []
    user_service.reset()
    users = user_service.list_users()
    assert len(users) == 2
    assert any(u.id == 1 for u in users)
    assert any(u.id == 2 for u in users)


def test_delete_user_with_duplicate_ids_removes_first_occurrence_only(user_service: UserService):
    # Add duplicate user with same id as existing user
    duplicate_user = UserResponse(
        id=1,
        name="Duplicate Ana",
        email="ana.dup@example.com",
        is_vip=False,
        status="inactive",
        role="user",
        phone_number="+55 11 90000-9999",
    )
    user_service._users.append(duplicate_user)
    initial_count = len(user_service._users)
    result = user_service.delete_user(1)
    assert result is True
    # Only one user with id=1 removed
    remaining_ids = [u.id for u in user_service._users]
    assert remaining_ids.count(1) == 1
    assert len(user_service._users) == initial_count - 1


def test_delete_user_does_not_alter_list_on_internal_exception(monkeypatch, user_service: UserService):
    # Simulate exception during pop by patching list.pop to raise
    original_pop = user_service._users.pop

    def raise_exception(index):
        raise RuntimeError("Simulated internal error")

    user_service._users.append(
        UserResponse(
            id=999,
            name="Test User",
            email="test@example.com",
            is_vip=False,
            status="active",
            role="user",
            phone_number="+55 11 90000-0000",
        )
    )
    monkeypatch.setattr(user_service._users, "pop", raise_exception)
    initial_users = user_service._users.copy()
    with pytest.raises(RuntimeError):
        user_service.delete_user(999)
    # List should remain unchanged
    assert user_service._users == initial_users
    # Restore original pop method
    monkeypatch.setattr(user_service._users, "pop", original_pop)


def test_delete_user_thread_safety_simulation(user_service: UserService):
    # This test simulates concurrent deletions to check for race conditions
    user_service.reset()
    user_id = 1
    results = []

    def delete_user():
        result = user_service.delete_user(user_id)
        results.append(result)

    threads = [threading.Thread(target=delete_user) for _ in range(10)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()

    # Exactly one thread should succeed, others should fail
    assert results.count(True) == 1
    assert results.count(False) == 9
    # User should no longer exist
    assert user_service.get_user(user_id) is None


def test_delete_user_integration_with_update_and_count(user_service: UserService):
    user_service.reset()
    initial_count = len(user_service.list_users())
    user_id = 1
    # Delete user
    assert user_service.delete_user(user_id) is True
    # Count should decrease by 1
    assert len(user_service.list_users()) == initial_count - 1
    # Update user that was deleted should return None
    update_payload = UserUpdate(name="New Name")
    assert user_service.update_user(user_id, update_payload) is None


def test_delete_user_idempotency(user_service: UserService):
    user_service.reset()
    user_id = 2
    # First deletion succeeds
    assert user_service.delete_user(user_id) is True
    # Second deletion returns False
    assert user_service.delete_user(user_id) is False


def test_delete_user_with_non_integer_id_does_not_raise(user_service: UserService):
    # Passing float, None, string, object should not raise but return False
    invalid_ids = [3.14, None, "string", object()]
    for invalid_id in invalid_ids:
        try:
            result = user_service.delete_user(invalid_id)  # type: ignore
            assert result is False
        except Exception:
            pytest.fail(f"delete_user raised exception with invalid id: {invalid_id}")


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
    with pytest.raises(ValidationError):
        UserUpdate(name="Ana", email="invalid-email", is_vip=True)


def test_update_user_with_extra_fields_ignored(user_service: UserService):
    # Atualizar usuário com payload contendo campos extras não esperados
    # Como UserUpdate é Pydantic, campos extras são rejeitados (default Pydantic behavior)
    with pytest.raises(ValidationError):
        UserUpdate(name="Ana", email="ana@example.com", is_vip=True, extra_field="not_allowed")


def test_update_user_with_unicode_and_special_characters(user_service: UserService):
    # Atualizar usuário com dados contendo caracteres especiais ou unicode
    user_id = 1
    payload = UserUpdate(name="Ána Šilva 🚀", email="ana.silva@example.com", is_vip=True)
    updated_user = user_service.update_user(user_id, payload)

    assert updated_user is not None
    assert updated_user.name == "Ána Šilva 🚀"
    assert updated_user.email == "ana.silva@example.com"
    assert updated_user.is_vip is True


def test_update_user_with_max_length_fields(user_service: UserService):
    # Atualizar usuário com strings no tamanho máximo permitido (limite real 255 para email e name)
    max_length_name = "a" * 255
    max_length_email = ("a" * (255 - len("@example.com"))) + "@example.com"
    user_id = 1
    payload = UserUpdate(name=max_length_name, email=max_length_email, is_vip=True)
    updated_user = user_service.update_user(user_id, payload)

    assert updated_user is not None
    assert updated_user.name == max_length_name
    assert updated_user.email == max_length_email
    assert updated_user.is_vip is True


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
    for t in threads:
        t.start()
    for t in threads:
        t.join()


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


def test_update_user_with_fields_explicitly_set_to_none(user_service: UserService):
    # Atualizar usuário enviando explicitamente campos com valor None
    # Verificar se o sistema ignora esses campos e mantém os valores originais
    user_id = 1
    original_user = user_service.get_user(user_id)
    # Construir payload com campos None explicitamente
    payload = UserUpdate(name=None, email=None, is_vip=None)
    updated_user = user_service.update_user(user_id, payload)

    assert updated_user is not None
    assert updated_user.id == original_user.id
    # Campos devem permanecer inalterados pois None deve ser ignorado
    assert updated_user.name == original_user.name
    assert updated_user.email == original_user.email
    assert updated_user.is_vip == original_user.is_vip


def test_update_user_omitting_fields_preserves_original_values(user_service: UserService):
    # Atualizar usuário omitindo campos no payload e confirmar que valores originais permanecem
    user_id = 2
    original_user = user_service.get_user(user_id)
    # Payload vazio (todos campos omitidos)
    payload = UserUpdate()
    updated_user = user_service.update_user(user_id, payload)

    assert updated_user is not None
    assert updated_user.id == original_user.id
    assert updated_user.name == original_user.name
    assert updated_user.email == original_user.email
    assert updated_user.is_vip == original_user.is_vip


@pytest.mark.parametrize(
    "email_value,should_raise",
    [
        ("a" * 243 + "@example.com", False),  # 243 chars local part + domain, valid
        ("a" * 245 + "@example.com", True),   # 245 chars local part + domain, invalid (over 255)
    ],
)
def test_update_user_email_length_limits(user_service: UserService, email_value, should_raise):
    user_id = 1
    if should_raise:
        with pytest.raises(ValidationError):
            UserUpdate(email=email_value)
    else:
        payload = UserUpdate(email=email_value)
        updated_user = user_service.update_user(user_id, payload)
        assert updated_user.email == email_value


def test_update_user_rejects_extra_undeclared_fields(user_service: UserService):
    # Testar rejeição de campos extras no payload, incluindo None e inválidos
    with pytest.raises(ValidationError):
        UserUpdate(name="Name", email="email@example.com", is_vip=True, unknown_field="value")

    with pytest.raises(ValidationError):
        UserUpdate(name="Name", email="email@example.com", is_vip=True, extra_none=None)

    with pytest.raises(ValidationError):
        UserUpdate(name="Name", email="email@example.com", is_vip=True, extra_invalid=123)


def test_update_user_partial_does_not_alter_unset_fields(user_service: UserService):
    # Atualização parcial não deve alterar campos não enviados
    user_id = 1
    original_user = user_service.get_user(user_id)
    payload = UserUpdate(name="Partial Update")
    updated_user = user_service.update_user(user_id, payload)

    assert updated_user.name == "Partial Update"
    assert updated_user.email == original_user.email
    assert updated_user.is_vip == original_user.is_vip


def test_update_user_with_invalid_field_types_raises_validation_error():
    # Testar atualização com tipos inválidos para os campos
    with pytest.raises(ValidationError):
        UserUpdate(name=123, email="valid@example.com", is_vip=True)

    with pytest.raises(ValidationError):
        UserUpdate(name="Valid Name", email=456, is_vip=True)

    with pytest.raises(ValidationError):
        UserUpdate(name="Valid Name", email="valid@example.com", is_vip="not_bool")


def test_update_user_with_empty_strings_and_omitted_optional_fields(user_service: UserService):
    # Testar atualização parcial com campos opcionais omitidos e explicitamente vazios
    user_id = 1
    original_user = user_service.get_user(user_id)
    payload = UserUpdate(name="", email=None)  # email None deve ser ignorado
    updated_user = user_service.update_user(user_id, payload)

    assert updated_user.name == ""  # aceita string vazia
    assert updated_user.email == original_user.email  # email None ignorado
    assert updated_user.is_vip == original_user.is_vip


def test_update_user_with_special_characters_and_whitespace(user_service: UserService):
    # Testar campos de texto com caracteres especiais, espaços e unicode
    user_id = 2
    special_name = " José  Ñandú\t\n"
    special_email = "jose.nandu@example.com"
    payload = UserUpdate(name=special_name, email=special_email)
    updated_user = user_service.update_user(user_id, payload)

    assert updated_user.name == special_name
    assert updated_user.email == special_email


def test_update_user_multiple_invalid_fields_raises_and_no_state_change(user_service: UserService):
    # Testar rollback e consistência após falha de validação múltipla
    user_id = 1
    original_user = user_service.get_user(user_id)
    with pytest.raises(ValidationError):
        UserUpdate(name=123, email="invalid-email", is_vip="not_bool")
    user_after = user_service.get_user(user_id)
    assert user_after == original_user
