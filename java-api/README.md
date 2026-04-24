# Java API (Spring Boot)

Esta pasta contém uma implementação Java equivalente à API Python do repositório.

## Requisitos

- Java 21+
- Maven 3.9+

## Rodando localmente

```bash
cd java-api
mvn spring-boot:run
```

A API sobe em `http://localhost:8080` por padrão.

- Swagger UI: `http://localhost:8080/swagger-ui.html` *(não habilitado por padrão neste projeto)*
- Endpoints principais:
  - `GET /health`
  - `GET /users`
  - `GET /users/count`
  - `GET /users/search?q=...`
  - `GET /users/duplicates`
  - `GET /users/email-domains`
  - `GET /users/{id}`
  - `GET /users/{id}/exists` (retorna `{ "exists": true|false }`)
  - `GET /users/{id}/email`
  - `GET /users/{id}/age-estimate`
  - `POST /users`
  - `PUT /users/{id}` (atualização parcial: `name`, `email` ou ambos)

## Testes

```bash
cd java-api
mvn test
```

## Observações

- Armazenamento em memória, com usuários seed:
  - `Ana Silva` / `ana@example.com`
  - `Bruno Lima` / `bruno@example.com`
- Integração externa via `https://api.agify.io` para estimativa de idade.
