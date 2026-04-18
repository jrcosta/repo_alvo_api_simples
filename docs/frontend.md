# Frontend

O projeto inclui um frontend simples em HTML/CSS/JS puro, servido diretamente pelo FastAPI.

## Acesso

Com o servidor rodando:

```bash
cd python-api
uvicorn app.main:app --reload
```

Acesse: **http://localhost:8000**

A documentação Swagger da API continua disponível em: **http://localhost:8000/docs**

## Funcionalidades

| Seção | Descrição |
|---|---|
| 🩺 Status & Contagem | Verifica saúde da API e conta usuários |
| ➕ Criar Usuário | Formulário para criar usuário (nome + email) |
| 🔍 Buscar por Nome | Pesquisa via `/users/search` |
| 👤 Buscar por ID | Busca usuário ou só o email pelo ID |
| 📋 Listar Usuários | Lista paginada com controle de limit/offset |

## Arquitetura

- Arquivo único: `python-api/static/index.html`
- Sem dependências externas (sem React, Vue, etc.)
- CSS inline com design responsivo
- JavaScript vanilla fazendo `fetch()` contra a API
- Servido pelo FastAPI via `StaticFiles` e `FileResponse`

## Como Funciona

Em `python-api/app/main.py`:

```python
STATIC_DIR = Path(__file__).resolve().parent.parent / "static"

app.mount("/static", StaticFiles(directory=str(STATIC_DIR)), name="static")

@app.get("/", include_in_schema=False)
def root():
    return FileResponse(STATIC_DIR / "index.html")
```

- `GET /` → retorna o `index.html`
- Arquivos em `python-api/static/` são acessíveis via `/static/...`
- O frontend faz chamadas à API no mesmo host (sem CORS necessário)
