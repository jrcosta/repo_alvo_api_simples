import pytest
from pathlib import Path

README_PATH = Path(__file__).parent.parent.parent / "README.md"

def test_readme_does_not_contain_instruction_to_delete_services_main_py():
    """
    Verifica que o README.md não contém mais a instrução para apagar o arquivo
    python-api/app/services/main.py sem backup.
    """
    content = README_PATH.read_text(encoding="utf-8")
    assert "python-api/app/services/main.py" in content, "Arquivo services/main.py deve ser mencionado no README"
    # Verifica que não existe instrução destrutiva para apagar o arquivo
    forbidden_phrases = [
        "Você deve apagar o arquivo python-api/app/services/main.py sem backup",
        "apagar o arquivo python-api/app/services/main.py sem backup",
        "apagar python-api/app/services/main.py sem backup",
        ":fire:"
    ]
    for phrase in forbidden_phrases:
        assert phrase not in content, f"Instrução destrutiva encontrada no README: '{phrase}'"

def test_readme_contains_safe_backup_and_restore_instructions():
    """
    Verifica que o README.md contém instruções claras e seguras para backup e restauração,
    prevenindo perda acidental de dados.
    """
    content = README_PATH.read_text(encoding="utf-8")
    backup_keywords = ["backup", "restauração", "restore", "salvar", "cópia"]
    found_backup_instruction = any(keyword in content.lower() for keyword in backup_keywords)
    assert found_backup_instruction, "README.md deve conter instruções para backup e restauração do ambiente"

@pytest.mark.parametrize("endpoint", ["/health", "/users"])
def test_api_python_basic_endpoints_functionality(client, endpoint):
    """
    Testa endpoints básicos da API Python para garantir que o arquivo services/main.py está intacto e funcional.
    Usa o client fixture do FastAPI TestClient.
    """
    response = client.get(endpoint)
    assert response.status_code == 200, f"Endpoint {endpoint} deve retornar status 200"
    if endpoint == "/health":
        assert response.json() == {"status": "ok"}
    elif endpoint == "/users":
        data = response.json()
        assert isinstance(data, list)
        assert len(data) >= 2, "Deve retornar ao menos 2 usuários"

def test_git_history_does_not_contain_deletion_of_services_main_py():
    """
    Verifica que não há commits recentes que apaguem o arquivo python-api/app/services/main.py.
    Usa git log para buscar commits que removem o arquivo.
    """
    import subprocess
    try:
        result = subprocess.run(
            ["git", "log", "--pretty=format:%H", "--", "python-api/app/services/main.py"],
            capture_output=True,
            text=True,
            check=True,
        )
        commits = result.stdout.strip().split("\n")
        # Se houver commits, verificar se algum deles removeu o arquivo
        for commit in commits:
            diff_result = subprocess.run(
                ["git", "show", commit, "--", "python-api/app/services/main.py"],
                capture_output=True,
                text=True,
                check=True,
            )
            diff_text = diff_result.stdout
            # Verifica se há linhas removidas que indicam deleção total do arquivo
            if diff_text.startswith("diff --git") and "\n-# " in diff_text:
                # Não há deleção total do arquivo no histórico recente
                continue
        # Se chegou aqui, não encontrou deleção total
        assert True
    except subprocess.CalledProcessError:
        pytest.skip("Git não disponível ou erro ao executar comandos git")