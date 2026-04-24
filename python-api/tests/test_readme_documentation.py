import pytest
from pathlib import Path

README_PATH = Path(__file__).parent.parent.parent / "README.md"

def test_readme_does_not_contain_other_destructive_instructions():
    """
    Verifica que o README.md não contém outras instruções potencialmente destrutivas
    relacionadas a arquivos críticos além da instrução removida.
    """
    content = README_PATH.read_text(encoding="utf-8")
    destructive_keywords = [
        "apagar", "remover", "deletar", "excluir", "sem backup", "sem salvar", "sem cópia"
    ]
    # Permitimos menção a arquivos, mas não instruções destrutivas
    for keyword in destructive_keywords:
        assert keyword not in content.lower(), f"Instrução destrutiva encontrada no README: '{keyword}'"

def test_readme_instructions_are_clear_for_agents_ia():
    """
    Verifica se o README.md possui instruções atualizadas e adequadas para agentes IA,
    para evitar impactos negativos na automação.
    """
    content = README_PATH.read_text(encoding="utf-8")
    # Busca por termos comuns para agentes IA e instruções claras
    agent_keywords = ["agentes ia", "agent", "automação", "workflow", "script", "diretrizes"]
    found_agent_instruction = any(keyword in content.lower() for keyword in agent_keywords)
    assert found_agent_instruction, "README.md deve conter instruções claras para agentes IA"