import importlib
import sys
import pytest

MODULE_PATH = "app.services.main"

def test_services_main_module_importable():
    """
    Test that the module 'app.services.main' is importable.
    This test should pass when the file python-api/app/services/main.py exists.
    """
    try:
        module = importlib.import_module(MODULE_PATH)
        assert module is not None
    except ModuleNotFoundError:
        pytest.fail(f"Module '{MODULE_PATH}' not found. The file 'python-api/app/services/main.py' may be missing.")

def test_services_main_module_import_fails_after_removal(monkeypatch):
    """
    Simulate removal of the module by removing it from sys.modules and sys.path,
    then assert that import raises ModuleNotFoundError.
    This test is to confirm that removal of the file causes import failure.
    """
    # Remove module if already loaded
    sys.modules.pop(MODULE_PATH, None)

    # Temporarily remove the services directory from sys.path to simulate removal
    # The services directory is python-api/app/services
    import os
    import pathlib

    base_dir = pathlib.Path(__file__).parent.parent / "app" / "services"
    base_dir_str = str(base_dir.resolve())

    # Remove base_dir from sys.path if present
    original_sys_path = sys.path.copy()
    sys.path = [p for p in sys.path if p != base_dir_str]

    # Also remove parent app directory to simulate missing module
    app_dir = base_dir.parent
    app_dir_str = str(app_dir.resolve())
    sys.path = [p for p in sys.path if p != app_dir_str]

    with pytest.raises(ModuleNotFoundError):
        importlib.import_module(MODULE_PATH)

    # Restore sys.path
    sys.path = original_sys_path