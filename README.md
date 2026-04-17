# Repo Alvo API Simples

API REST em **FastAPI** criada para servir como repositório-alvo em testes com agentes de IA para QA e revisão técnica.

## Início Rápido

```bash
# Clonar e instalar
git clone https://github.com/jrcosta/repo_alvo_api_simples.git
cd repo_alvo_api_simples
python -m venv .venv
source .venv/bin/activate        # Linux/macOS
# .venv\Scripts\Activate.ps1     # Windows PowerShell
pip install -r requirements.txt

# Rodar a API
uvicorn app.main:app --reload

# Rodar os testes
pytest -q
```

Acesse:

| URL | Descrição |
|---|---|
| http://localhost:8000 | Frontend |
| http://localhost:8000/docs | Swagger (documentação interativa) |
| http://localhost:8000/redoc | ReDoc |

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/health` | Healthcheck |
| `GET` | `/users` | Listar usuários (com paginação) |
| `GET` | `/users/count` | Total de usuários |
| `GET` | `/users/search?q=` | Buscar por nome |
| `GET` | `/users/duplicates` | Encontrar emails duplicados |
| `GET` | `/users/{id}` | Buscar por ID |
| `GET` | `/users/{id}/email` | Email de um usuário |
| `GET` | `/users/{id}/age-estimate` | Estimativa de idade (agify.io) |
| `POST` | `/users` | Criar usuário |

## 📚 Documentação

| Documento | Conteúdo |
|---|---|
| [Arquitetura](docs/arquitetura.md) | Estrutura de pastas, camadas, decisões de design |
| [Endpoints](docs/endpoints.md) | Referência completa da API com exemplos |
| [Testes](docs/testes.md) | Como rodar, lista de testes, cobertura |
| [Frontend](docs/frontend.md) | Interface web e como funciona |
| [CI/CD](docs/ci.md) | GitHub Actions e pipeline de testes |
| [Integração Externa](docs/integracao-externa.md) | API agify.io, tratamento de erros, limitações |

## Objetivo

Este repositório existe para:

- Servir como alvo de análise automatizada por agentes de IA
- Testar workflows com GitHub Actions
- Validar geração de relatórios de QA
- Permitir mudanças controladas para exercitar cenários de revisão

As diretrizes para agentes estão descritas em [`AGENTS.md`](AGENTS.md).
