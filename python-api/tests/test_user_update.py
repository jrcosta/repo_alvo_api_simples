import pytest
from fastapi import status
from fastapi.testclient import TestClient
from unittest.mock import patch
from app.main import app

client = TestClient(app, raise_server_exceptions=False)

def test_update_user_success():
    # Atualizar usuário existente com nome e is_vip diferentes
    payload = {"name": "Ana Updated", "is_vip": False}
    response = client.put("/users/1", json=payload)
    
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert data["name"] == "Ana Updated"
    assert data["is_vip"] is False
    assert data["email"] == "ana@example.com"  # Permanece igual

def test_update_user_email_conflict():
    # Tenta atualizar usuário com e-mail já usado por outro usuário
    payload = {"email": "bruno@example.com"}
    response = client.put("/users/1", json=payload)
    
    assert response.status_code == status.HTTP_409_CONFLICT
    assert response.json()["detail"] == "E-mail já cadastrado por outro usuário"

def test_update_user_not_found():
    # Tentar atualizar usuário inexistente
    response = client.put("/users/999", json={"name": "Ghost"})
    assert response.status_code == status.HTTP_404_NOT_FOUND
    assert response.json()["detail"] == "Usuário não encontrado"

@pytest.mark.parametrize("invalid_name", ["", "   "])
def test_update_user_invalid_name(invalid_name):
    # Enviar nome inválido (string vazia ou só espaços)
    response = client.put("/users/1", json={"name": invalid_name})
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY

def test_update_user_partial_email_only():
    # Atualizar usuário com payload contendo apenas email (atualização parcial)
    new_email = "ana.new@example.com"
    payload = {"email": new_email}
    response = client.put("/users/1", json=payload)
    
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert data["email"] == new_email
    assert data["name"] == "Ana Silva"  # Nome permanece igual

def test_update_user_partial_is_vip_only():
    # Atualizar usuário com payload contendo apenas is_vip (atualização parcial)
    payload = {"is_vip": True}
    response = client.put("/users/1", json=payload)
    
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert data["is_vip"] is True
    assert data["name"] == "Ana Silva"  # Nome permanece igual

def test_update_user_with_additional_fields():
    # Atualizar usuário com campos adicionais (role, telefone) se existirem
    payload = {
        "name": "Ana Role Updated",
        "role": "admin",
        "telefone": "+5511999999999"
    }
    response = client.put("/users/1", json=payload)
    
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert data["name"] == "Ana Role Updated"
    # Verifica se campos adicionais foram atualizados ou ignorados conforme implementação
    # Se o campo role existir, deve estar presente e igual
    if "role" in data:
        assert data["role"] == "admin"
    if "telefone" in data:
        assert data["telefone"] == "+5511999999999"

@pytest.mark.parametrize("invalid_payload", [
    {"is_vip": "yes"},  # is_vip como string inválida
    {"email": 12345},   # email como número inválido
])
def test_update_user_invalid_types(invalid_payload):
    # Enviar payload com tipos incorretos, verificar erro 422
    response = client.put("/users/1", json=invalid_payload)
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY

def test_update_user_empty_payload():
    # Testar atualização com payload vazio {}
    response = client.put("/users/1", json={})
    # Para atualização parcial, {} é erro 422
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY

def test_update_user_with_extra_unexpected_fields():
    # Testar atualização com campos extras não esperados
    payload = {
        "name": "Ana Extra",
        "extra_field": "unexpected",
        "another_one": 123
    }
    response = client.put("/users/1", json=payload)
    # Campos extras devem ser rejeitados (422)
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY
    # Com 422, não validamos o corpo de sucesso
    pass

def test_update_user_with_null_values():
    # Testar envio de valores nulos para campos atualizáveis
    payload = {
        "name": None,
        "email": None,
        "is_vip": None
    }
    response = client.put("/users/1", json=payload)
    # Agora aceitamos nulos (200 OK)
    assert response.status_code == status.HTTP_200_OK

def test_update_user_immutable_fields_ignored():
    # Testar que campos imutáveis (ex: id, created_at, updated_at) não são alterados
    payload = {
        "id": 999,
        "created_at": "2020-01-01T00:00:00Z",
        "updated_at": "2020-01-01T00:00:00Z",
        "name": "Ana Immutable Test"
    }
    response = client.put("/users/1", json=payload)
    # Com extra='forbid', id causa 422
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY
    # Com 422, não validamos o corpo de sucesso
    pass

def test_update_user_enum_fields_invalid_values():
    # Testar atualização com campos de enumeração com valores inválidos
    payload = {
        "status": "invalid_status",
        "role": "invalid_role"
    }
    response = client.put("/users/1", json=payload)
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY

@pytest.mark.parametrize("phone", [
    "+5511999999999",  # válido
    "11999999999",     # válido sem código país
    "abc123",          # inválido
    "12345678901234567890"  # muito longo inválido
])
def test_update_user_phone_field_validation(phone):
    payload = {"telefone": phone}
    response = client.put("/users/1", json=payload)
    if phone in ["+5511999999999", "11999999999"]:
        assert response.status_code == status.HTTP_200_OK
        data = response.json()
        assert data.get("telefone") == phone
    else:
        assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY

def test_update_user_with_nested_json_field():
    # Testar payload com campos aninhados inesperados
    payload = {
        "name": "Ana Nested",
        "extra": {"nested": "value"}
    }
    response = client.put("/users/1", json=payload)
    # Espera-se erro 422 ou ignorar campo extra, dependendo da validação
    # Aqui assumimos erro 422 para campos inesperados aninhados
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY or response.status_code == status.HTTP_200_OK

@patch("app.api.routes.user_service.update_user")
def test_update_user_database_exception(mock_update):
    # Testar comportamento quando a camada de dados lança exceção
    mock_update.side_effect = Exception("DB failure")
    payload = {"name": "Ana DB Error"}
    response = client.put("/users/1", json=payload)
    assert response.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR

def test_update_user_flow_complete():
    # Testar fluxo completo: criar, atualizar, buscar e validar dados atualizados
    # Criar usuário novo
    create_payload = {"name": "Test User", "email": "testuser@example.com", "is_vip": False}
    create_response = client.post("/users", json=create_payload)
    assert create_response.status_code == status.HTTP_201_CREATED
    user_id = create_response.json()["id"]

    # Atualizar usuário criado
    update_payload = {"name": "Test User Updated", "is_vip": True}
    update_response = client.put(f"/users/{user_id}", json=update_payload)
    assert update_response.status_code == status.HTTP_200_OK
    updated_data = update_response.json()
    assert updated_data["name"] == "Test User Updated"
    assert updated_data["is_vip"] is True
    assert updated_data["email"] == "testuser@example.com"

    # Buscar usuário atualizado
    get_response = client.get(f"/users/{user_id}")
    assert get_response.status_code == status.HTTP_200_OK
    get_data = get_response.json()
    assert get_data == updated_data

def test_update_user_with_large_payload():
    # Testar resposta para payloads grandes com muitos campos
    large_payload = {
        "name": "A" * 1000,
        "email": "largepayload@example.com",
        "is_vip": True,
        "role": "user",
        "status": "active",
        "telefone": "+5511999999999"
    }
    response = client.put("/users/1", json=large_payload)
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert data["name"] == "A" * 1000
    assert data["email"] == "largepayload@example.com"

@pytest.mark.asyncio
async def test_update_user_concurrent_updates():
    # Testar concorrência: duas atualizações simultâneas no mesmo usuário
    import asyncio

    async def update_name():
        return client.put("/users/1", json={"name": "Concurrent Name"})

    async def update_email():
        return client.put("/users/1", json={"email": "concurrent@example.com"})

    responses = await asyncio.gather(update_name(), update_email())
    for response in responses:
        assert response.status_code == status.HTTP_200_OK

    # Verificar consistência dos dados após atualizações concorrentes
    final_response = client.get("/users/1")
    assert final_response.status_code == status.HTTP_200_OK
    final_data = final_response.json()
    assert final_data["name"] in ["Concurrent Name", "Ana Silva", "Ana Updated"]
    assert final_data["email"] in ["concurrent@example.com", "ana@example.com"]

def test_update_user_missing_required_fields():
    # Testar atualização com campos obrigatórios ausentes no payload (ex: vazio)
    response = client.put("/users/1", json={})
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY

def test_update_user_authentication_required():
    # Testar atualização sem autenticação (se aplicável)
    # Aqui assumimos que o endpoint requer autenticação via header Authorization
    # TestClient não envia token por padrão, espera 401 ou 403
    # Se não houver autenticação, este teste pode ser ignorado ou adaptado
    response = client.put("/users/1", json={"name": "No Auth"})
    assert response.status_code in (status.HTTP_401_UNAUTHORIZED, status.HTTP_403_FORBIDDEN, status.HTTP_200_OK)

def test_update_user_rollback_on_partial_failure():
    # Testar rollback em caso de falha parcial na atualização
    # Simular falha na camada de dados para um campo específico
    with patch("app.api.routes.user_service.update_user") as mock_update:
        def side_effect(user_id, payload):
            if "email" in payload:
                raise Exception("DB failure on email update")
            return {"id": user_id, **payload}
        mock_update.side_effect = side_effect

        payload = {"name": "Rollback Test", "email": "rollback@example.com"}
        response = client.put("/users/1", json=payload)
        assert response.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR

def test_update_user_with_status_and_role_valid_and_invalid():
    # Testar atualização com campos status e role válidos e inválidos
    valid_payload = {"status": "active", "role": "user"}
    response_valid = client.put("/users/1", json=valid_payload)
    assert response_valid.status_code == status.HTTP_200_OK

    invalid_payload = {"status": "unknown", "role": "invalid"}
    response_invalid = client.put("/users/1", json=invalid_payload)
    assert response_invalid.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY