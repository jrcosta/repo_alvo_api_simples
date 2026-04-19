import pytest

from app.api import routes


@pytest.fixture(autouse=True)
def reset_user_service() -> None:
    """Reset UserService state before each test to ensure isolation."""
    routes.user_service.reset()
