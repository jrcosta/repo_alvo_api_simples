import pytest
from pathlib import Path

README_PATH = Path(__file__).parent.parent.parent / "README.md"

def test_onboarding_flow_documentation_has_no_destructive_steps():
    """
    Simula o uso do README para configurar o ambiente e garante que não há instruções destrutivas.
    """
    content = README_PATH.read_text(encoding="utf-8")
    # Verifica que não há instruções para apagar arquivos críticos
    destructive_phrases = [
        "apagar python-api/app/services/main.py",
        "remover python-api/app/services/main.py",
        "deletar python-api/app/services/main.py",
        "sem backup"
    ]
    for phrase in destructive_phrases:
        assert phrase not in content, f"Instrução destrutiva encontrada no README: '{phrase}'"

def test_onboarding_flow_readme_contains_all_required_setup_steps():
    """
    Verifica que o README.md contém os passos essenciais para configurar o ambiente,
    como clonagem, criação de venv, instalação de dependências e execução da API.
    """
    content = README_PATH.read_text(encoding="utf-8")
    required_steps = [
        "git clone",
        "python -m venv",
        "pip install -r python-api/requirements.txt",
        "uvicorn app.main:app --reload",
        "pytest -q tests"
    ]
    for step in required_steps:
        assert step in content, f"Passo essencial '{step}' não encontrado no README.md"