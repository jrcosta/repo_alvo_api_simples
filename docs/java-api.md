# API Java (Spring Boot)

Este repositório agora possui duas implementações equivalentes da API:

- API Python/FastAPI (na raiz do projeto)
- API Java/Spring Boot (na pasta `java-api/`)

## Estrutura Java

```text
java-api/
├── pom.xml
├── README.md
└── src/
    ├── main/java/com/repoalvo/javaapi/
    │   ├── JavaApiApplication.java
    │   ├── controller/UserController.java
    │   ├── model/
    │   └── service/
    └── test/java/com/repoalvo/javaapi/
        └── UserControllerIntegrationTest.java
```

## Endpoints equivalentes

A implementação Java expõe as mesmas rotas principais da API Python:

- `GET /health`
- `GET /users`
- `GET /users/count`
- `GET /users/search?q=...`
- `GET /users/duplicates`
- `GET /users/{id}`
- `GET /users/{id}/email`
- `GET /users/{id}/age-estimate`
- `POST /users`
- `GET /users/first-email`
- `GET /users/broken`

## Rodar localmente

```bash
cd java-api
mvn spring-boot:run
```

Servidor padrão: `http://localhost:8080`

## Testes

```bash
cd java-api
mvn test
```
