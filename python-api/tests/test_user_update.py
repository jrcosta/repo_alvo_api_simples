import pytest
from fastapi import status
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app, raise_server_exceptions=False)

def test_update_user_success():
    # Ana Silva (id=1)
    payload = {"name": "Ana Updated", "is_vip": False}
    response = client.put("/users/1", json=payload)
    
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert data["name"] == "Ana Updated"
    assert data["is_vip"] is False
    assert data["email"] == "ana@example.com"  # Permanece igual

def test_update_user_email_conflict():
    # Tenta atualizar Ana Silva (id=1) com o e-mail do Bruno (id=2)
    payload = {"email": "bruno@example.com"}
    response = client.put("/users/1", json=payload)
    
    assert response.status_code == status.HTTP_409_CONFLICT
    assert response.json()["detail"] == "E-mail já cadastrado por outro usuário"

def test_update_user_not_found():
    response = client.put("/users/999", json={"name": "Ghost"})
    assert response.status_code == status.HTTP_404_NOT_FOUND
    assert response.json()["detail"] == "Usuário não encontrado"

def test_update_user_invalid_name():
    response = client.put("/users/1", json={"name": "  "})
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY
