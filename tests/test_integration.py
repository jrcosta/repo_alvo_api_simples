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

def test_read_index_html_with_different_encodings() -> None:
    # Testa a leitura do arquivo index.html com diferentes codificações
    encodings = ['utf-8', 'ISO-8859-1']
    for encoding in encodings:
        with open(STATIC_DIR / "index.html", encoding=encoding) as f:
            content = f.read()
            assert content is not None  # Verifica se o conteúdo foi lido

def test_root_endpoint_with_special_characters_in_index_html() -> None:
    # Simula a resposta do endpoint `/` com um arquivo index.html que contém caracteres especiais
    special_content = "<html><body><h1>Olá, Mundo!</h1></body></html>"
    with open(STATIC_DIR / "index.html", "w", encoding="utf-8") as f:
        f.write(special_content)

    response = client.get("/")
    assert response.status_code == 200
    assert special_content.replace("\r\n", "\n") in response.text.replace("\r\n", "\n")

def test_read_malformed_index_html() -> None:
    # Testa a aplicação com um arquivo index.html malformado
    malformed_content = "<html><body><h1>Malformed HTML"
    with open(STATIC_DIR / "index.html", "w", encoding="utf-8") as f:
        f.write(malformed_content)

    response = client.get("/")
    assert response.status_code == 200  # A aplicação deve lidar com o erro sem quebrar
    assert "Malformed HTML" in response.text