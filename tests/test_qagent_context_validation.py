import os
import pytest

QAGENT_CONTEXT_PATH = ".qagent/knowledge/qagent-context.md"

def test_qagent_context_file_exists():
    assert os.path.isfile(QAGENT_CONTEXT_PATH), f"Arquivo {QAGENT_CONTEXT_PATH} deve existir."

def test_qagent_context_file_not_empty():
    with open(QAGENT_CONTEXT_PATH, "r", encoding="utf-8") as f:
        content = f.read()
    assert len(content.strip()) > 0, "Arquivo não deve estar vazio."

def test_qagent_context_has_required_sections():
    with open(QAGENT_CONTEXT_PATH, "r", encoding="utf-8") as f:
        content = f.read()

    required_sections = [
        "## 1. Visão Geral do Projeto",
        "## 2. Stack Tecnológica",
        "## 3. Estrutura do Projeto",
        "## 4. Regras de Arquitetura",
        "## 5. Regras de Teste",
        "## 6. Regras de Review",
        "## 7. Padrões de Código",
        "## 8. Coisas que o Agente NÃO deve fazer",
        "## 9. Exemplos Práticos"
    ]

    for section in required_sections:
        assert section in content, f"Seção obrigatória '{section}' não encontrada no arquivo."

def test_qagent_context_formatting_is_valid_markdown():
    # Basic validation: check for balanced headers and no broken lines
    with open(QAGENT_CONTEXT_PATH, "r", encoding="utf-8") as f:
        lines = f.readlines()

    header_counts = {
        "#": 0,
        "##": 0,
        "###": 0
    }
    for line in lines:
        line_strip = line.strip()
        if line_strip.startswith("### "):
            header_counts["###"] += 1
        elif line_strip.startswith("## "):
            header_counts["##"] += 1
        elif line_strip.startswith("# "):
            header_counts["#"] += 1

    # Expect at least one top-level header and multiple second-level headers
    assert header_counts["#"] >= 1, "Deve haver pelo menos um header nível 1 (#)."
    assert header_counts["##"] >= 5, "Deve haver múltiplos headers nível 2 (##)."

def test_qagent_context_no_hardcoded_secrets():
    with open(QAGENT_CONTEXT_PATH, "r", encoding="utf-8") as f:
        content = f.read().lower()
    forbidden_keywords = ["token", "senha", "password", "apikey", "secret", "chave"]
    for keyword in forbidden_keywords:
        assert keyword not in content, f"Arquivo não deve conter '{keyword}' hardcoded."

@pytest.mark.parametrize("partial_content,expected_error", [
    ("## 1. Visão Geral do Projeto\n- Apenas uma linha", False),
    ("## 1. Visão Geral do Projeto\n- Linha incompleta sem quebra", False),
    ("# Início sem seção", True),
    ("", True),
])
def test_qagent_context_robustness_against_incomplete_formatting(tmp_path, partial_content, expected_error):
    test_file = tmp_path / "qagent-context.md"
    test_file.write_text(partial_content, encoding="utf-8")

    def load_and_validate(path):
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        if not content.strip():
            raise ValueError("Arquivo vazio")
        if not content.startswith("## 1. Visão Geral do Projeto"):
            raise ValueError("Seção inicial obrigatória ausente")
        return True

    if expected_error:
        with pytest.raises(ValueError):
            load_and_validate(test_file)
    else:
        assert load_and_validate(test_file) is True

def test_qagent_context_simulate_agent_interpretation(monkeypatch):
    """
    Simula a interpretação do arquivo por um agente de IA, verificando se regras são aplicadas corretamente.
    """

    class FakeAgent:
        def __init__(self, context_path):
            self.context_path = context_path
            self.rules_loaded = False
            self.rules = []

        def load_context(self):
            with open(self.context_path, "r", encoding="utf-8") as f:
                content = f.read()
            if "Regras de Arquitetura" in content:
                self.rules_loaded = True
                self.rules.append("Arquitetura")
            if "Regras de Teste" in content:
                self.rules.append("Testes")
            if "Coisas que o Agente NÃO deve fazer" in content:
                self.rules.append("Restrições")
            return self.rules_loaded

        def analyze_code(self, code_snippet):
            if not self.rules_loaded:
                raise RuntimeError("Contexto não carregado")
            # Simula análise simples baseada em regras
            if "hardcoded" in code_snippet:
                return "Erro: hardcoded detectado"
            return "Análise OK"

    agent = FakeAgent(QAGENT_CONTEXT_PATH)
    assert agent.load_context() is True
    result_ok = agent.analyze_code("função sem problemas")
    result_err = agent.analyze_code("token hardcoded na função")

    assert result_ok == "Análise OK"
    assert result_err == "Erro: hardcoded detectado"

def test_qagent_context_simulate_conflicting_rules(monkeypatch):
    """
    Simula cenário de conflito entre regras documentadas no arquivo e regras emergentes em outra documentação.
    Verifica se a priorização correta das regras é aplicada.
    """

    class RuleSet:
        def __init__(self, base_rules, emergent_rules):
            self.base_rules = base_rules
            self.emergent_rules = emergent_rules

        def get_effective_rules(self):
            # Prioriza regras emergentes sobre base
            effective = self.base_rules.copy()
            effective.update(self.emergent_rules)
            return effective

    base_rules = {
        "max_line_length": 120,
        "require_tests": True,
        "allow_hardcoded": False
    }
    emergent_rules = {
        "max_line_length": 100,  # regra emergente mais restritiva
        "allow_hardcoded": True  # conflito: emergente permite hardcoded
    }

    ruleset = RuleSet(base_rules, emergent_rules)
    effective = ruleset.get_effective_rules()

    assert effective["max_line_length"] == 100
    assert effective["require_tests"] is True
    assert effective["allow_hardcoded"] is True

def test_qagent_context_handle_partial_updates(tmp_path):
    """
    Testa a capacidade dos agentes de IA de lidar com atualizações parciais ou incrementais no arquivo,
    assegurando que mudanças menores não causem falhas na interpretação.
    """
    base_content = (
        "## 1. Visão Geral do Projeto\n"
        "- Descrição inicial\n"
        "## 2. Stack Tecnológica\n"
        "- Python, Java, JS\n"
    )
    updated_content = (
        base_content +
        "## 3. Estrutura do Projeto\n"
        "- Novas pastas adicionadas\n"
    )

    test_file = tmp_path / "qagent-context.md"
    test_file.write_text(base_content, encoding="utf-8")

    class Agent:
        def __init__(self, path):
            self.path = path
            self.sections = []

        def load(self):
            with open(self.path, "r", encoding="utf-8") as f:
                content = f.read()
            self.sections = [line for line in content.splitlines() if line.startswith("## ")]
            return self.sections

    agent = Agent(test_file)
    sections_before = agent.load()
    assert "## 3. Estrutura do Projeto" not in sections_before

    test_file.write_text(updated_content, encoding="utf-8")
    sections_after = agent.load()
    assert "## 3. Estrutura do Projeto" in sections_after

def test_qagent_context_simulate_read_failure(monkeypatch):
    """
    Simula falha na leitura ou carregamento do arquivo por agentes, avaliando a resiliência e fallback dos agentes.
    """

    class Agent:
        def __init__(self, path):
            self.path = path
            self.context_loaded = False

        def load_context(self):
            try:
                with open(self.path, "r", encoding="utf-8") as f:
                    _ = f.read()
                self.context_loaded = True
            except Exception:
                self.context_loaded = False
            return self.context_loaded

    agent = Agent(QAGENT_CONTEXT_PATH)
    assert agent.load_context() is True

    # Simula falha de leitura
    agent_fail = Agent("/caminho/invalido/qagent-context.md")
    assert agent_fail.load_context() is False