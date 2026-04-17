from fastapi.testclient import TestClient
from pathlib import Path
from app.main import app

client = TestClient(app)

STATIC_DIR = Path(__file__).resolve().parent.parent / "static"

def test_root_endpoint_integration() -> None:
    response = client.get("/")
    assert response.status_code == 200
    assert "html" in response.headers["content-type"]
    
    # Verifica se o conteúdo esperado está presente no index.html
    with open(STATIC_DIR / "index.html") as f:
        expected_content = f.read()
    assert expected_content in response.text

def test_access_static_file_integration() -> None:
    response = client.get("/static/somefile.js")
    assert response.status_code == 200
    assert response.text == "console.log('Hello World');"  # Conteúdo esperado

def test_access_nonexistent_static_file_returns_404_integration() -> None:
    response = client.get("/static/nonexistentfile.js")
    assert response.status_code == 404