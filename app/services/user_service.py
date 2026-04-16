from app.schemas import UserCreate, UserResponse


class UserService:
    def __init__(self) -> None:
        self._users: list[UserResponse] = [
            UserResponse(id=1, name="Ana Silva", email="ana@example.com"),
            UserResponse(id=2, name="Bruno Lima", email="bruno@example.com"),
        ]
        self._next_id = 3

    def list_users(self) -> list[UserResponse]:
        return self._users

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
        )
        self._users.append(user)
        self._next_id += 1
        return user
