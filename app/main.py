from fastapi import FastAPI

from app.api.routes import router

app = FastAPI(
    title="Repo Alvo API Simples",
    version="1.0.0",
    description="API simples para servir como repositório-alvo em análises automatizadas de QA.",
)

app.include_router(router)
