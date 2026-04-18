# Testes

O projeto usa [pytest](https://docs.pytest.org/) como framework de testes. Todos os testes estão na pasta `tests/`.

## Como Rodar

```bash
# Ativar o ambiente virtual
source .venv/bin/activate   # Linux/macOS
.venv\Scripts\Activate.ps1  # Windows PowerShell

# Rodar todos os testes
pytest -q

# Rodar com saída detalhada
pytest -v

# Rodar apenas um arquivo
pytest tests/test_api.py -v

# Rodar um teste específico
pytest tests/test_api.py::test_healthcheck_returns_ok -v
```

## Estrutura dos Testes

### `tests/test_api.py` — Testes Unitários

| Teste | Endpoint | O que valida |
|---|---|---|
| `test_healthcheck_returns_ok` | `GET /health` | Retorna 200 com `{"status": "ok"}` |
| `test_list_users_returns_seeded_users` | `GET /users` | Lista retorna ao menos 2 usuários |
| `test_list_users_pagination_limit_offset` | `GET /users?limit=&offset=` | Paginação funciona corretamente |
| `test_users_count_returns_number` | `GET /users/count` | Retorna inteiro ≥ 2 |
| `test_users_count_route_not_captured_by_id` | `GET /users/count` | Rota estática não é capturada pela dinâmica |
| `test_create_user_returns_201` | `POST /users` | Criação retorna 201 com dados corretos |
| `test_create_user_duplicate_email_returns_409` | `POST /users` | Email duplicado retorna 409 |
| `test_get_user_email_returns_email` | `GET /users/1/email` | Retorna email correto |
| `test_search_users_returns_matching_results` | `GET /users/search?q=Ana` | Busca retorna resultados com campos esperados |
| `test_duplicates_returns_empty_when_no_duplicates` | `GET /users/duplicates` | Lista vazia quando não há duplicatas |
| `test_duplicates_returns_users_with_same_email` | `GET /users/duplicates` | Detecta emails duplicados corretamente |
| `test_duplicates_returns_valid_user_objects` | `GET /users/duplicates` | Objetos retornados têm campos id, name, email |

### `tests/test_external.py` — Testes com Mock

| Teste | O que valida |
|---|---|
| `test_age_estimate_returns_mocked_data` | Retorna dados mockados da agify.io |
| `test_age_estimate_null_age` | Lida com `age=null` sem erro |
| `test_age_estimate_user_not_found` | Retorna 404 para usuário inexistente |

### `tests/test_integration.py` — Testes de Integração

| Teste | Fluxo |
|---|---|
| `test_full_user_lifecycle` | Criar → buscar por id → email → search → contagem → lista → duplicatas |
| `test_duplicate_email_rejection_flow` | Criar → duplicar email → 409 → contagem inalterada |

## Cobertura

Para rodar com cobertura (requer `pytest-cov`):

```bash
pip install pytest-cov
pytest --cov=app --cov-report=term-missing
```
