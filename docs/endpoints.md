# Endpoints da API

Todos os endpoints estão definidos em `python-api/app/api/routes.py`. A documentação interativa (Swagger) está disponível em `/docs` ao rodar o servidor.

---

## Health

### `GET /health`

Verifica se a API está respondendo.

**Response** `200`:
```json
{ "status": "ok" }
```

---

## Usuários

### `GET /users`

Lista usuários com paginação.

| Parâmetro | Tipo | Default | Descrição |
|---|---|---|---|
| `limit` | int (≥1) | 100 | Máximo de usuários a retornar |
| `offset` | int (≥0) | 0 | Quantidade de usuários a pular |

**Response** `200`: `list[UserResponse]`

```json
[
  { "id": 1, "name": "Ana Silva", "email": "ana@example.com" },
  { "id": 2, "name": "Bruno Lima", "email": "bruno@example.com" }
]
```

---

### `GET /users/count`

Retorna o total de usuários cadastrados.

**Response** `200`:
```json
{ "count": 2 }
```

---

### `GET /users/search?q={termo}`

Busca usuários cujo nome contenha o termo (case-insensitive).

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `q` | string | sim | Substring para buscar no nome |

**Response** `200`: `list[UserResponse]`

---

### `GET /users/duplicates`

Retorna usuários cujo e-mail aparece mais de uma vez no sistema.

**Response** `200`: `list[UserResponse]`

---

### `GET /users/{user_id}`

Retorna um usuário pelo ID.

**Response** `200`: `UserResponse`  
**Response** `404`: `{ "detail": "Usuário não encontrado" }`

---

### `POST /users`

Cria um novo usuário.

**Body**:
```json
{ "name": "Maria Oliveira", "email": "maria@example.com" }
```

| Campo | Tipo | Regras |
|---|---|---|
| `name` | string | 3–100 caracteres |
| `email` | string | E-mail válido |

**Response** `201`: `UserResponse`  
**Response** `409`: `{ "detail": "E-mail já cadastrado" }`

---

### `GET /users/{user_id}/email`

Retorna apenas o e-mail de um usuário.

**Response** `200`:
```json
{ "email": "ana@example.com" }
```

**Response** `404`: `{ "detail": "Usuário não encontrado" }`

---

### `GET /users/first-email`

Retorna o primeiro usuário da lista (endpoint originalmente bugado, agora corrigido).

**Response** `200`: `UserResponse`  
**Response** `404`: `{ "detail": "Nenhum usuário encontrado" }`

---

### `GET /users/broken`

⚠️ **Endpoint intencionalmente bugado** — declara `CountResponse` como `response_model` mas retorna `{ "total": N }` em vez de `{ "count": N }`.

---

## Integração Externa

### `GET /users/{user_id}/age-estimate`

Estima a idade do usuário usando a API pública [agify.io](https://api.agify.io).

**Response** `200`:
```json
{ "name": "Ana", "age": 35, "count": 12345 }
```

Se a API externa falhar, `age` e `count` serão `null`.

**Response** `404`: `{ "detail": "Usuário não encontrado" }`

---

## Schemas

| Schema | Campos |
|---|---|
| `HealthResponse` | `status: str` |
| `UserCreate` | `name: str`, `email: EmailStr` |
| `UserResponse` | `id: int`, `name: str`, `email: EmailStr` |
| `CountResponse` | `count: int` |
| `EmailResponse` | `email: EmailStr` |
| `AgeEstimateResponse` | `name: str`, `age: int \| None`, `count: int \| None` |
