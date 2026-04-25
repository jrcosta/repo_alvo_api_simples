from app.schemas import UserCreate, UserResponse


class UserService:
    def __init__(self) -> None:
        self._users: list[UserResponse] = [
            UserResponse(id=1, name="Ana Silva", email="ana@example.com", is_vip=True),
            UserResponse(id=2, name="Bruno Lima", email="bruno@example.com", is_vip=False),
        ]
        self._next_id = 3

    def list_users(self, limit: int | None = None, offset: int = 0) -> list[UserResponse]:
        """Return a slice of users. `limit` and `offset` implement simple pagination.

        - limit: maximum number of users to return (None means no limit)
        - offset: number of users to skip from the start
        """
        if offset < 0:
            offset = 0

        if limit is None:
            return self._users[offset:]

        return self._users[offset: offset + limit]

    def get_user(self, user_id: int) -> UserResponse | None:
        for user in self._users:
            if user.id == user_id:
                return user
        return None

    def find_by_email(self, email: str) -> UserResponse | None:
        for user in self._users:
            if user.email == email:
                return user
        return None

    def create_user(self, payload: UserCreate) -> UserResponse:
        user = UserResponse(
            id=self._next_id,
            name=payload.name,
            email=payload.email,
            is_vip=payload.is_vip,
        )
        self._users.append(user)
        self._next_id += 1
        return user

    def reset(self) -> None:
        """Reinitialise the service to its original seeded state. Intended for use in tests."""
        self._users = [
            UserResponse(id=1, name="Ana Silva", email="ana@example.com", is_vip=True),
            UserResponse(id=2, name="Bruno Lima", email="bruno@example.com", is_vip=False),
        ]
        self._next_id = 3
