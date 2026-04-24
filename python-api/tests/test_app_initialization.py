import pytest
import importlib
import sys

@pytest.fixture(autouse=True)
def cleanup_module():
    # Ensure app.main is reloaded fresh for each test
    module_name = "app.main"
    if module_name in sys.modules:
        del sys.modules[module_name]
    yield
    if module_name in sys.modules:
        del sys.modules[module_name]

def test_fastapi_app_initializes_with_all_services():
    """
    Test that the FastAPI app initializes correctly when all service modules are present.
    This test imports app.main and checks that 'app' attribute exists and is a FastAPI instance.
    """
    import app.main
    app_instance = getattr(app.main, "app", None)
    assert app_instance is not None, "FastAPI app instance should be present in app.main"
    # We do not import FastAPI directly to avoid dependency, just check type name
    assert app_instance.__class__.__name__ == "FastAPI"

def test_fastapi_app_initialization_fails_when_services_main_missing(monkeypatch):
    """
    Simulate removal of 'app.services.main' by monkeypatching sys.modules to raise ModuleNotFoundError.
    Then assert that importing app.main raises ImportError or ModuleNotFoundError.
    """
    original_import = __import__

    def mocked_import(name, *args, **kwargs):
        if name == "app.services.main":
            raise ModuleNotFoundError("No module named 'app.services.main'")
        return original_import(name, *args, **kwargs)

    monkeypatch.setattr("builtins.__import__", mocked_import)

    with pytest.raises(ModuleNotFoundError):
        importlib.reload(importlib.import_module("app.main"))