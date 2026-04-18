# CI/CD — GitHub Actions

O projeto utiliza GitHub Actions para rodar os testes automaticamente a cada push ou pull request na branch `main`.

## Workflow

Arquivo: `.github/workflows/python-tests.yml`

### Trigger

- **Push** na branch `main`
- **Pull Request** para a branch `main`

### Matrix

O workflow roda os testes em duas versões do Python:

| Versão |
|---|
| Python 3.10 |
| Python 3.11 |

> ⚠️ As versões estão entre aspas no YAML (`'3.10'`, `'3.11'`) para evitar que o parser YAML interprete `3.10` como o float `3.1`.

### Etapas

1. **Checkout** — clona o repositório
2. **Setup Python** — instala a versão do Python
3. **Install dependencies** — `pip install -r requirements.txt`
4. **Run tests** — `pytest -q`

### Exemplo de Uso

Ao fazer push para `main`, o workflow roda automaticamente. O resultado aparece na aba **Actions** do repositório GitHub.

## Melhorias Futuras Sugeridas

- Adicionar cache de pip para acelerar o CI
- Adicionar relatório de cobertura de testes
- Adicionar badge de status no README
- Adicionar lint (ruff/flake8) como step adicional
