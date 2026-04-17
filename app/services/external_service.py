import httpx

from app.schemas import AgeEstimateResponse


class ExternalService:
    """Service to integrate with public APIs.

    This service currently wraps the free https://api.agify.io API to estimate
    a person's age from their name. No authentication is required.
    """

    AGIFY_URL = "https://api.agify.io"

    def estimate_age(self, name: str) -> AgeEstimateResponse:
        try:
            resp = httpx.get(self.AGIFY_URL, params={"name": name}, timeout=5.0)
            resp.raise_for_status()
            data = resp.json()
            # Map API response into our Pydantic model
            return AgeEstimateResponse(**data)
        except httpx.RequestError:
            # On network errors, return an empty/nullable response
            return AgeEstimateResponse(name=name, age=None, count=None)
        except httpx.HTTPStatusError:
            return AgeEstimateResponse(name=name, age=None, count=None)
