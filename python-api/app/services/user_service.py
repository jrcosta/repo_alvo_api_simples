from app.schemas import UserCreate, UserResponse, UserUpdate


class UserService:
    def __init__(self) -> None:
        self._users: list[UserResponse] = [
            UserResponse(id=1, name="Ana Silva", email="ana@example.com", is_vip=True, status="active", role="admin", phone_number="+55 11 90000-0001"),
            UserResponse(id=2, name="Bruno Lima", email="bruno@example.com", is_vip=False, status="active", role="user", phone_number="+55 11 90000-0002"),
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
            status=payload.status,
            role=payload.role,
            phone_number=payload.phone_number,
        )
        self._users.append(user)
        self._next_id += 1
        return user

    def update_user(self, user_id: int, payload: UserUpdate) -> UserResponse | None:
        for i, user in enumerate(self._users):
            if user.id == user_id:
                updated_name = payload.name if payload.name is not None else user.name
                updated_email = payload.email if payload.email is not None else user.email
                updated_is_vip = payload.is_vip if payload.is_vip is not None else user.is_vip
                updated_status = payload.status if payload.status is not None else user.status
                updated_role = payload.role if payload.role is not None else user.role
                updated_phone = payload.phone_number if payload.phone_number is not None else user.phone_number

                updated_user = UserResponse(
                    id=user.id,
                    name=updated_name,
                    email=updated_email,
                    is_vip=updated_is_vip,
                    status=updated_status,
                    role=updated_role,
                    phone_number=updated_phone
                )
                self._users[i] = updated_user
                return updated_user
        return None

    def delete_user(self, user_id: int) -> bool:
        for i, user in enumerate(self._users):
            if user.id == user_id:
                self._users.pop(i)
                return True
        return False

    def reset(self) -> None:
        """Reinitialise the service to its original seeded state. Intended for use in tests."""
        self._users = [
            UserResponse(id=1, name="Ana Silva", email="ana@example.com", is_vip=True, status="active", role="admin", phone_number="+55 11 90000-0001"),
            UserResponse(id=2, name="Bruno Lima", email="bruno@example.com", is_vip=False, status="active", role="user", phone_number="+55 11 90000-0002"),
        ]
        self._next_id = 3


user_service = UserService()
