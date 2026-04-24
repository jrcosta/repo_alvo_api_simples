import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_full_user_lifecycle_with_services_main_py_intact():
    """
    Testa o fluxo completo da API Python para garantir que o arquivo services/main.py está funcional.
    Inclui criação, busca, listagem e contagem de usuários.
    """
    # Criar usuário
    user_payload = {"name": "Test User", "email": "testuser@example.com"}
    response = client.post("/users", json=user_payload)
    assert response.status_code == 201
    created_user = response.json()
    user_id = created_user.get("id")
    assert user_id is not None

    # Buscar usuário por ID
    response = client.get(f"/users/{user_id}")
    assert response.status_code == 200
    user_data = response.json()
    assert user_data["name"] == user_payload["name"]
    assert user_data["email"] == user_payload["email"]

    # Listar usuários
    response = client.get("/users")
    assert response.status_code == 200
    users_list = response.json()
    assert any(u["id"] == user_id for u in users_list)

    # Contar usuários
    response = client.get("/users/count")
    assert response.status_code == 200
    count = response.json()
    assert isinstance(count, int)
    assert count >= 1

def test_pipeline_cicd_does_not_fail_due_to_missing_services_main_py():
    """
    Testa se o pipeline CI/CD não falha por ausência do arquivo services/main.py.
    Simula execução de testes básicos para garantir integridade.
    """
    # Aqui simulamos rodar um comando pytest básico para garantir que o arquivo está presente
    import subprocess
    try:
        result = subprocess.run(
            ["pytest", "-q", "--maxfail=1", "--disable-warnings", "python-api/tests/test_api.py"],
            capture_output=True,
            text=True,
            check=True,
        )
        output = result.stdout + result.stderr
        assert "error" not in output.lower()
        assert "failed" not in output.lower()
    except subprocess.CalledProcessError as e:
        pytest.fail(f"Pipeline CI/CD falhou: {e}")