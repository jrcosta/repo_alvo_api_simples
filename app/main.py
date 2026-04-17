from pathlib import Path

from fastapi import FastAPI
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles

from app.api.routes import router

STATIC_DIR = Path(__file__).resolve().parent.parent / "static"

app = FastAPI(
    title="Repo Alvo API Simples",
    version="1.0.0",
    description="API simples para servir como repositório-alvo em análises automatizadas de QA.",
)

app.include_router(router)

app.mount("/static", StaticFiles(directory=str(STATIC_DIR)), name="static")


@app.get("/", include_in_schema=False)
def root():
    return FileResponse(STATIC_DIR / "index.html")
