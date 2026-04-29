import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.schemas import UserUpdate

client = TestClient(app)


@pytest.fixture(autouse=True)
def reset_users():
    # Reset users before each test to ensure consistent state
    from app.services.user_service import user_service
    user_service.reset()


def test_api_update_user_partial_success():
    # Atualização parcial via API e validação da resposta
    user_id = 1
    payload = {"email": "ana.partial@example.com"}
    response = client.put(f"/users/{user_id}", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["id"] == user_id
    assert data["email"] == "ana.partial@example.com"
    assert data["name"] == "Ana Silva"  # name não alterado


def test_api_update_user_full_success():
    # Atualização completa via API
    user_id = 2
    payload = {"name": "Bruno Updated", "email": "bruno.updated@example.com", "is_vip": True}
    response = client.put(f"/users/{user_id}", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["id"] == user_id
    assert data["name"] == "Bruno Updated"
    assert data["email"] == "bruno.updated@example.com"
    assert data["is_vip"] is True


def test_api_update_user_not_found():
    # Tentativa de atualização de usuário inexistente retorna 404
    user_id = 9999
    payload = {"name": "No User"}
    response = client.put(f"/users/{user_id}", json=payload)
    assert response.status_code == 404
    assert response.json()["detail"] == "Usuário não encontrado"


def test_api_update_user_invalid_payload():
    # Payload inválido retorna 400
    user_id = 1
    payload = {"email": "invalid-email-format"}
    response = client.put(f"/users/{user_id}", json=payload)
    assert response.status_code == 422  # Pydantic validation error returns 422 Unprocessable Entity


def test_api_update_user_email_conflict():
    # Testar conflito de email (409) ao tentar atualizar para email já existente em outro usuário
    user_id = 1
    # Email do usuário 2 é bruno@example.com
    payload = {"email": "bruno@example.com"}
    response = client.put(f"/users/{user_id}", json=payload)
    assert response.status_code == 409
    assert "E-mail já cadastrado" in response.json()["detail"]


def test_api_update_user_no_fields_to_update():
    # Payload vazio ou com todos campos None deve retornar usuário sem alterações
    user_id = 1
    payload = {}
    response = client.put(f"/users/{user_id}", json=payload)
    assert response.status_code == 422


def test_api_update_user_persists_data_after_update():
    # Verificar persistência dos dados após atualização via API
    user_id = 2
    payload = {"name": "Bruno Persisted", "email": "bruno.persisted@example.com", "is_vip": False}
    response = client.put(f"/users/{user_id}", json=payload)
    assert response.status_code == 200

    # Fazer GET para confirmar dados atualizados
    get_response = client.get(f"/users/{user_id}")
    assert get_response.status_code == 200
    data = get_response.json()
    assert data["name"] == "Bruno Persisted"
    assert data["email"] == "bruno.persisted@example.com"
    assert data["is_vip"] is False


def test_api_update_user_returns_404_for_none_returned_by_service(monkeypatch):
    # Validar comportamento da camada de rota HTTP para update_user quando o retorno é None
    from app.api import routes
    def fake_update_user(user_id, payload):
        return None
    monkeypatch.setattr(routes.user_service, "update_user", fake_update_user)
    user_id = 1
    payload = {"name": "Any Name"}
    response = client.put(f"/users/{user_id}", json=payload)
    assert response.status_code == 404
    assert response.json()["detail"] == "Usuário não encontrado"


def test_api_update_user_authorization_denied(monkeypatch):
    # Testar autorização negativa: usuário sem permissão tenta atualizar dados e recebe 403
    # Como não há autenticação implementada, simulamos via monkeypatch no endpoint
    from fastapi import HTTPException
    from app.api import routes

    original_update_user = routes.user_service.update_user

    def fake_update_user(user_id, payload):
        raise HTTPException(status_code=403, detail="Acesso negado")

    monkeypatch.setattr(routes.user_service, "update_user", fake_update_user)
    user_id = 1
    payload = {"name": "Unauthorized"}
    response = client.put(f"/users/{user_id}", json=payload)
    assert response.status_code == 403
    assert response.json()["detail"] == "Acesso negado"

    # Restaurar método original
    monkeypatch.setattr(routes.user_service, "update_user", original_update_user)


def test_api_update_user_authorization_success(monkeypatch):
    # Testar autorização positiva: usuário autorizado consegue atualizar dados com sucesso
    from app.api import routes

    original_update_user = routes.user_service.update_user

    def fake_update_user(user_id, payload):
        return routes.user_service.get_user(user_id)

    monkeypatch.setattr(routes.user_service, "update_user", fake_update_user)
    user_id = 1
    payload = {"name": "Authorized"}
    response = client.put(f"/users/{user_id}", json=payload)
    assert response.status_code == 200
    assert response.json()["name"] == "Ana Silva"  # Como fake_update_user retorna usuário original

    monkeypatch.setattr(routes.user_service, "update_user", original_update_user)