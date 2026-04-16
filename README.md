# Repo Alvo API Simples

API pequena em **FastAPI** criada para servir como repositório-alvo em testes do QAgent.

## O que tem aqui

- endpoint de healthcheck
- listagem de usuários
- consulta de usuário por id
- criação de usuário com validação e conflito de e-mail
- testes automatizados com pytest

## Estrutura

```text
app/
├─ api/
│  └─ routes.py
├─ services/
│  └─ user_service.py
├─ main.py
└─ schemas.py

tests/
└─ test_api.py
```

## Instalação

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

No Windows PowerShell:

```powershell
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

## Rodando a API

```bash
uvicorn app.main:app --reload
```

A documentação estará em:

- `/docs`
- `/redoc`

## Rodando os testes

```bash
pytest
```

## Endpoints

- `GET /health`
- `GET /users`
- `GET /users/{user_id}`
- `POST /users`

### Exemplo de payload

```json
{
  "name": "Carlos Souza",
  "email": "carlos@example.com"
}
```

## Objetivo

Este repositório existe para:

- servir como alvo de análise automatizada
- testar workflows com GitHub Actions
- validar geração de relatórios do agente
- permitir mudanças controladas para exercitar cenários de QA
