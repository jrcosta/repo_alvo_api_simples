# Repo Alvo API Simples

Monorepo com **duas implementações equivalentes** da mesma API:

- API Python/FastAPI (`python-api/`)
- API Java/Spring Boot (`java-api/`)

O projeto foi criado para servir como repositório-alvo em testes com agentes de IA para QA e revisão técnica.

## Estrutura do Repositório

```text
repo_alvo_api_simples/
├── python-api/          # API Python/FastAPI
│   ├── app/
│   ├── tests/
│   ├── static/
│   └── requirements.txt
├── docs/                # Documentação geral e por contexto
├── java-api/            # API Java/Spring Boot equivalente
└── README.md
```

## Início Rápido

```bash
# Clonar e instalar
git clone https://github.com/jrcosta/repo_alvo_api_simples.git
cd repo_alvo_api_simples
python -m venv .venv
source .venv/bin/activate        # Linux/macOS
# .venv\Scripts\Activate.ps1     # Windows PowerShell
pip install -r python-api/requirements.txt
cd python-api

# Rodar a API
uvicorn app.main:app --reload

# Rodar os testes
pytest -q tests
```

## API Java (Spring Boot)

```bash
cd java-api
mvn spring-boot:run

# testes
mvn test
```

A API Java sobe em `http://localhost:8080` por padrão.

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
| `GET` | `/users/by-email` | Buscar por e-mail |
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
| [API Java](docs/java-api.md) | Organização, execução e testes da implementação Spring Boot |

## Objetivo

Este repositório existe para:

- Servir como alvo de análise automatizada por agentes de IA
- Testar workflows com GitHub Actions
- Validar geração de relatórios de QA
- Permitir mudanças controladas para exercitar cenários de revisão

As diretrizes para agentes estão descritas em [`AGENTS.md`](AGENTS.md).
