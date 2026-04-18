# Integração Externa

O projeto integra com a API pública [agify.io](https://api.agify.io) para estimar a idade de uma pessoa a partir do nome.

## Serviço

Arquivo: `app/services/external_service.py`

### `ExternalService.estimate_age(name: str) -> AgeEstimateResponse`

- Faz uma requisição GET para `https://api.agify.io?name={name}`
- Timeout de 5 segundos
- Não requer autenticação
- Retorna `AgeEstimateResponse(name, age, count)`

### Tratamento de Erros

| Cenário | Comportamento |
|---|---|
| Resposta normal | Retorna `age` e `count` da API |
| Erro de rede (`RequestError`) | Retorna `age=None, count=None` |
| Erro HTTP (`HTTPStatusError`) | Retorna `age=None, count=None` |

A API nunca lança exceção para o cliente — erros externos são tratados internamente.

## Endpoint

### `GET /users/{user_id}/age-estimate`

1. Busca o usuário pelo ID
2. Se não existe → `404`
3. Chama `ExternalService.estimate_age(user.name)`
4. Retorna o resultado

## Testes

Os testes em `tests/test_external.py` usam `monkeypatch` para substituir o método `estimate_age`, evitando chamadas reais à API externa.

```python
# Exemplo de mock
def fake_estimate(self, name):
    return AgeEstimateResponse(name=name, age=30, count=1000)

monkeypatch.setattr(ExternalService, "estimate_age", fake_estimate)
```

## Limitações

- A API agify.io tem limite de requisições por dia (1.000 sem API key)
- Não há cache implementado — cada chamada faz uma requisição HTTP
- A estimativa depende do primeiro nome; nomes compostos podem ter resultados imprecisos
