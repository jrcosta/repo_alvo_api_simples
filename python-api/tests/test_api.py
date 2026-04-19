import pytest
from pydantic import ValidationError
from app.schemas import EmailDomainCountResponse


def test_email_domain_count_response_creation_with_valid_data():
    response = EmailDomainCountResponse(domain="example.com", count=10)
    assert response.domain == "example.com"
    assert response.count == 10


@pytest.mark.parametrize(
    "invalid_domain",
    [123, 45.6, True, None, [], {}],
)
def test_email_domain_count_response_invalid_domain_type_raises_validation_error(invalid_domain):
    with pytest.raises(ValidationError) as exc_info:
        EmailDomainCountResponse(domain=invalid_domain, count=5)
    errors = exc_info.value.errors()
    assert any(error["loc"] == ("domain",) and error["type"].startswith("type_error") for error in errors)


@pytest.mark.parametrize(
    "invalid_count",
    ["string", 12.34, True, None, [], {}],
)
def test_email_domain_count_response_invalid_count_type_raises_validation_error(invalid_count):
    with pytest.raises(ValidationError) as exc_info:
        EmailDomainCountResponse(domain="example.com", count=invalid_count)
    errors = exc_info.value.errors()
    assert any(error["loc"] == ("count",) and error["type"].startswith("type_error") for error in errors)


def test_email_domain_count_response_serialization_and_deserialization():
    original = EmailDomainCountResponse(domain="example.com", count=42)
    json_str = original.model_dump_json()
    # Deserialize back
    deserialized = EmailDomainCountResponse.model_validate_json(json_str)
    assert deserialized == original
    assert deserialized.domain == "example.com"
    assert deserialized.count == 42