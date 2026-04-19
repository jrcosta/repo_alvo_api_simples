from pydantic import BaseModel, EmailStr, Field


class HealthResponse(BaseModel):
    status: str


class UserCreate(BaseModel):
    name: str = Field(..., min_length=3, max_length=100)
    email: EmailStr


class UserResponse(BaseModel):
    id: int
    name: str
    email: EmailStr


class CountResponse(BaseModel):
    count: int


class EmailResponse(BaseModel):
    email: EmailStr


class AgeEstimateResponse(BaseModel):
    name: str
    age: int | None = None
    count: int | None = None


class EmailDomainCountResponse(BaseModel):
    domain: str
    count: int
