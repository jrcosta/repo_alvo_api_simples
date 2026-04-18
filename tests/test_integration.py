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
    # Normaliza quebras de linha para lidar com arquivos CRLF ou LF
    with open(STATIC_DIR / "index.html", encoding="utf-8") as f:
        expected_content = f.read()
    assert expected_content.replace("\r\n", "\n") in response.text.replace("\r\n", "\n")

def test_access_static_file_integration() -> None:
    # O único arquivo estático presente é index.html
    response = client.get("/static/index.html")
    assert response.status_code == 200
    assert "html" in response.headers["content-type"]

def test_access_nonexistent_static_file_returns_404_integration() -> None:
    response = client.get("/static/nonexistentfile.js")
    assert response.status_code == 404

def test_root_endpoint_with_special_characters_in_index_html() -> None:
    # Verifica que o endpoint `/` serve o conteúdo do arquivo index.html com caracteres especiais UTF-8
    index_path = STATIC_DIR / "index.html"
    original_content = index_path.read_text(encoding="utf-8")
    special_content = "<html><body><h1>Olá, Mundo!</h1></body></html>"
    try:
        index_path.write_text(special_content, encoding="utf-8")
        response = client.get("/")
        assert response.status_code == 200
        assert special_content.replace("\r\n", "\n") in response.text.replace("\r\n", "\n")
    finally:
        index_path.write_text(original_content, encoding="utf-8")

def test_read_malformed_index_html() -> None:
    # Verifica que o endpoint `/` não retorna 500 mesmo com HTML malformado
    index_path = STATIC_DIR / "index.html"
    original_content = index_path.read_text(encoding="utf-8")
    malformed_content = "<html><body><h1>Malformed HTML"
    try:
        index_path.write_text(malformed_content, encoding="utf-8")
        response = client.get("/")
        assert response.status_code == 200
        assert "Malformed HTML" in response.text
    finally:
        index_path.write_text(original_content, encoding="utf-8")