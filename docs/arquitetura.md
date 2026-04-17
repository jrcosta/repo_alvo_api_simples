# Arquitetura do Projeto

## Visão Geral

O **Repo Alvo API Simples** é uma API REST construída com [FastAPI](https://fastapi.tiangolo.com/) e Python 3.10+. Foi projetada para ser um ambiente controlado de testes para agentes de IA voltados a QA e revisão técnica.

## Estrutura de Pastas

```text
repo_alvo_api_simples/
├── app/
│   ├── __init__.py
│   ├── main.py                  # Entrada da aplicação FastAPI
│   ├── schemas.py               # Modelos Pydantic (request/response)
│   ├── api/
│   │   ├── __init__.py
│   │   └── routes.py            # Definição de todos os endpoints
│   └── services/
│       ├── __init__.py
│       ├── user_service.py      # Lógica de negócio para usuários
│       └── external_service.py  # Integração com APIs externas (agify.io)
├── static/
│   └── index.html               # Frontend simples (HTML/CSS/JS)
├── tests/
│   ├── test_api.py              # Testes unitários dos endpoints
│   ├── test_external.py         # Testes da integração externa (com mock)
│   └── test_integration.py      # Testes de integração (fluxos completos)
├── docs/                        # Documentação do projeto
├── .github/
│   └── workflows/
│       └── python-tests.yml     # CI com GitHub Actions
├── requirements.txt
├── AGENTS.md                    # Diretrizes para agentes de IA
└── README.md
```

## Camadas

### 1. Entrada (`app/main.py`)

- Instancia o `FastAPI`
- Inclui o router de rotas
- Monta os arquivos estáticos (`/static`)
- Serve o frontend na rota raiz (`/`)

### 2. Rotas (`app/api/routes.py`)

- Define todos os endpoints REST
- Faz validação de entrada via `response_model` e `Query`
- Delega lógica de negócio aos serviços

### 3. Serviços (`app/services/`)

- **`UserService`**: armazena usuários em memória (lista Python), oferece operações CRUD
- **`ExternalService`**: encapsula chamadas HTTP à API pública [agify.io](https://api.agify.io)

### 4. Schemas (`app/schemas.py`)

- Modelos Pydantic v2 para validação e serialização
- Garantem contratos entre cliente e API

## Decisões de Design

| Decisão | Justificativa |
|---|---|
| Armazenamento em memória | Simplicidade — o projeto é para testes, não produção |
| Serviços separados das rotas | Facilita testes unitários e mocks |
| Rotas estáticas antes de dinâmicas | Evita que `/users/count` seja capturado por `/users/{user_id}` |
| Frontend inline (sem framework JS) | Mantém o projeto leve e sem dependências de build |

## Dependências Principais

| Pacote | Uso |
|---|---|
| `fastapi` | Framework web |
| `uvicorn` | Servidor ASGI |
| `pydantic` | Validação de dados |
| `httpx` | Cliente HTTP para APIs externas |
| `pytest` | Testes automatizados |
| `email-validator` | Validação de e-mails no Pydantic |
