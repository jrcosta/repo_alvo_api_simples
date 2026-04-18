import pytest

from app.api import routes
from app.schemas import UserResponse


@pytest.fixture(autouse=True)
def reset_user_service() -> None:
    """Reset UserService state before each test to ensure isolation."""
    routes.user_service._users = [
        UserResponse(id=1, name="Ana Silva", email="ana@example.com"),
        UserResponse(id=2, name="Bruno Lima", email="bruno@example.com"),
    ]
    routes.user_service._next_id = 3
