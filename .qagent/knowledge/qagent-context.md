# Contexto do Projeto para o QAgent

Este documento serve como base de conhecimento para orientar agentes de IA que analisarem este repositório.

## 1. Visão Geral do Projeto
- **O que o projeto faz:** É um monorepo que contém múltiplas implementações equivalentes da mesma API (ex: Python/FastAPI, Java/Spring Boot, JavaScript/Node).
- **Problema que resolve:** Serve como repositório-alvo controlado para desenvolvimento, validação e demonstração de automações com agentes de IA voltados para QA e revisão técnica.
- **Principais fluxos:** O repositório lida com endpoints simples de CRUD e simulação de regras de negócio, projetados para exercitar a capacidade de análise dos agentes (ex: identificar bugs, avaliar segurança, gerar testes e validar regressões).

## 2. Stack Tecnológica
- **Linguagens:** Python, Java, JavaScript.
- **Frameworks Principais:** FastAPI (Python), Spring Boot (Java), Express/Koa etc. (JavaScript).
- **Banco de Dados:** Em memória / Dummy data (projetado para focar nas regras de negócio e validações da API). <!-- TODO: Especificar banco real, caso venha a existir -->
- **Ferramentas de Teste:** `pytest` (Python), `JUnit`/`Mockito` via Maven (Java), bibliotecas equivalentes para JS (ex: `jest` / `mocha`).
- **Ferramentas de Build/CI:** GitHub Actions.

## 3. Estrutura do Projeto
- **Principais pastas e responsabilidades:**
  - `/python-api/`: Implementação da API em Python.
  - `/java-api/`: Implementação da API em Java.
  - `/javascript-api/`: Implementação da API em JavaScript.
  - `/docs/`: Documentação técnica geral, documentação arquitetural e guias.
  - `/.github/workflows/`: Pipelines de CI/CD para automação de testes e checagens.
- **Onde ficam os testes:**
  - Python: `/python-api/tests/`
  - Java: `/java-api/src/test/java/`
- **Onde ficam as configurações:**
  - Python: `requirements.txt`, arquivos na raiz do app FastAPI.
  - Java: `pom.xml`, `application.properties`/`application.yml`.
  - JS: `package.json`.

## 4. Regras de Arquitetura
- **Padrões a respeitar:** As implementações de API devem manter paridade funcional entre si (mesmos contratos de endpoints).
- **Camadas:** O projeto mantém a separação de responsabilidades (ex: controllers/rotas devem delegar regras de negócio mais complexas para a camada de services, quando aplicável).
- **Dependências permitidas:** Evitar adicionar bibliotecas externas pesadas desnecessariamente que fujam da simplicidade alvo do projeto.
- **Boas práticas de segurança:** Nenhuma credencial/secret (tokens, chaves de API) deve ser incluída (hardcoded) no código versionado.

## 5. Regras de Teste
- **Tipos esperados:** Testes unitários para regras de negócio e testes de integração de API.
- **Padrão de nomes:** Descritivos, evidenciando o cenário e resultado esperado. Exemplo: `test_deve_retornar_usuario_por_id_quando_existir`.
- **Bibliotecas usadas:** `pytest` (Python), `JUnit` (Java).
- **O que deve ser mockado:** Chamadas a APIs externas (ex: agify.io) ou dependências de infraestrutura complexas.
- **O que não deve ser mockado:** Regras de negócio internas da própria aplicação e funções simples de utilitários.
- **Critérios mínimos para um teste útil:** Testes devem possuir `asserts` fortes que validam estado e comportamento (sucesso, erros e edge cases). Testes que validam `assert True` ou sem asserts explícitos são inválidos.

## 6. Regras de Review
Ao analisar PRs, observe:
- **Foco principal:** O que realmente mudou no diff, impacto funcional e risco de regressão.
- **Risco Baixo:** Atualização de documentação, pequenas correções visuais e melhorias de leitura sem alteração de lógica.
- **Risco Médio:** Mudança em validações de inputs e alteração moderada de regras na camada de serviço.
- **Risco Alto:** Alterações em autenticação/autorização, modificação de contratos da API (novos ou remoção) e persistência.
- **Problemas comuns:** Remoção indevida de validações de entrada, hardcodes, introdução de código morto ou inclusão de novos comportamentos sem testes respectivos.

## 7. Padrões de Código
- **Nomenclatura:** Código com intenção clara, legível e seguindo as convenções nativas da linguagem trabalhada (snake_case no Python, camelCase no Java/JS).
- **Tratamento de erro:** Utilizar status HTTP apropriados para as requisições, evitar silenciosamente encobrir exceções críticas.
- **Logs:** Fazer logging de eventos importantes e usar níveis apropriados, mas NUNCA logar informações sensíveis (senhas, chaves, PII).
- **Validações:** Realizar validações estritas de contratos de entrada logo no controller/view da requisição.
- **Organização de imports:** Seguir os padrões definidos pelo formatador do projeto ou ordem nativa da comunidade.

## 8. Coisas que o Agente NÃO deve fazer
- Não sugerir mudanças profundas ou de escopo fora do diff da análise.
- Não inferir contexto sem evidências claras ou inventar classes/APIs não existentes.
- Não gerar testes apenas para aumentar cobertura métrica se eles forem genéricos ou tiverem `assert` nulo.
- Não desconsiderar ou assumir aprovações automáticas em CI sem os testes de validação funcionarem.
- Não agir de forma destrutiva nem modificar/remover configuração de pipeline sem forte embasamento.
- Nunca executar `git push` ou abrir PRs sem aprovação explícita e afirmativa do criador (como "autorizo enviar"). Silêncio não é consentimento.

## 9. Exemplos Práticos

### ✅ Exemplo de Boa Sugestão de Teste
```python
def test_deve_retornar_erro_404_quando_usuario_nao_encontrado(client):
    response = client.get("/users/999")
    assert response.status_code == 404
    assert response.json() == {"detail": "Usuário não encontrado"}
```

### ❌ Exemplo de Sugestão Ruim de Teste
```python
def test_users(client):
    response = client.get("/users/1")
    assert response is not None  # Assert fraco que não garante o contrato de dados
```

### 💬 Exemplo de Comentário de Review Útil
> "Notei que a validação de formato do e-mail foi removida na linha 42 de `UserController`. Isso pode permitir a entrada de dados inválidos no fluxo e gerar exceções incontroláveis mais abaixo. Sugiro manter a validação e, se possível, adicionar um teste unitário confirmando a negação de emails malformados."
