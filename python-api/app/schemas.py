import re
from typing import List, Literal, Optional
from pydantic import BaseModel, ConfigDict, EmailStr, Field, StrictBool, field_validator

class HealthResponse(BaseModel):
    model_config = ConfigDict(extra='forbid')
    status: str

class UserCreate(BaseModel):
    name: str = Field(..., min_length=3, max_length=2000)
    email: EmailStr
    is_vip: StrictBool = False
    status: Literal["active", "inactive", "pending"] = "active"
    role: Literal["user", "admin", "guest"] = "user"
    phone_number: Optional[str] = Field(None, alias="telefone")

    model_config = ConfigDict(populate_by_name=True, extra='forbid')

    @field_validator("name")
    @classmethod
    def reject_blank_name(cls, value: str) -> str:
        if not value.strip():
            raise ValueError("must not be blank")
        return value

class UserUpdate(BaseModel):
    name: Optional[str] = Field(None, min_length=3, max_length=2000)
    email: Optional[EmailStr] = None
    is_vip: Optional[StrictBool] = None
    status: Optional[Literal["active", "inactive", "pending"]] = None
    role: Optional[Literal["user", "admin", "guest"]] = None
    phone_number: Optional[str] = Field(None, alias="telefone")

    model_config = ConfigDict(populate_by_name=True, extra='forbid')

    @field_validator("name", mode="before")
    @classmethod
    def reject_blank_name(cls, value: any) -> any:
        if isinstance(value, str) and not value.strip():
            raise ValueError("must not be blank")
        return value

    @field_validator("phone_number")
    @classmethod
    def reject_invalid_phone(cls, value: str | None) -> str | None:
        if value is not None and not re.match(r"^\+?\d{10,15}$", value):
            raise ValueError("invalid phone format")
        return value

class UserResponse(BaseModel):
    id: int
    name: str = Field(..., min_length=1)
    email: EmailStr
    is_vip: StrictBool = False
    status: str = "active"
    role: str = "user"
    phone_number: Optional[str] = Field(None, alias="telefone")

    model_config = ConfigDict(populate_by_name=True, extra='forbid')

    @field_validator("name")
    @classmethod
    def reject_blank_name(cls, value: str) -> str:
        if not value.strip():
            raise ValueError("must not be blank")
        return value

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

class DiscountRequest(BaseModel):
    model_config = ConfigDict(extra='forbid')
    base_price: float = Field(..., ge=0)
    discount_percentage: float = Field(0.0, ge=0, le=100)
    coupon_code: str | None = None
    is_vip: bool = False

class DiscountResponse(BaseModel):
    model_config = ConfigDict(extra='forbid')
    final_price: float

class CartItemSchema(BaseModel):
    id: str = Field(..., min_length=1)
    name: str = Field(..., min_length=1)
    price: float = Field(..., ge=0)
    quantity: int = Field(1, ge=1)

    @field_validator("id", "name")
    @classmethod
    def reject_blank_strings(cls, value: str) -> str:
        if not value.strip():
            raise ValueError("must not be blank")
        return value

class CartRequest(BaseModel):
    items: List[CartItemSchema]
    coupon_code: str | None = None
    is_vip: bool = False

class CartResponse(BaseModel):
    subtotal: float
    tax_amount: float
    final_price: float
    items_count: int
