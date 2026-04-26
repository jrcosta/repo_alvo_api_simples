import pytest
from fastapi.testclient import TestClient
from app.api.routes import router, search_users, user_service, UserResponse
from fastapi import FastAPI

app = FastAPI()
app.include_router(router)

client = TestClient(app)


class UserWithoutIsVip:
    def __init__(self, id, name, email):
        self.id = id
        self.name = name
        self.email = email
        # no is_vip attribute


@pytest.fixture
def mock_users(monkeypatch):
    users = [
        UserResponse(id=1, name="Ana Silva", email="ana@example.com", status="ACTIVE", role="USER", is_vip=True),
        UserResponse(id=2, name="Bruno Lima", email="bruno@example.com", status="ACTIVE", role="USER", is_vip=False),
        UserResponse(id=3, name="Mariana Costa", email="mariana@example.com", status="ACTIVE", role="USER", is_vip=True),
        UserResponse(id=4, name="Olivia", email="olivia@example.com", status="ACTIVE", role="USER", is_vip=False),
        UserResponse(id=5, name="Vipiana", email="vipiana@example.com", status="ACTIVE", role="USER", is_vip=True),
        UserResponse(id=6, name="NoVipUser", email="novip@example.com", status="ACTIVE", role="USER", is_vip=False),
    ]

    def mock_list_users(limit=None, offset=None):
        return users

    monkeypatch.setattr(user_service, "list_users", mock_list_users)
    return users


def test_search_users_without_prefix_returns_all_matching_users(mock_users):
    # Query without vip: prefix, should return all users whose name contains "ana" case-insensitive
    results = search_users("ana")
    assert isinstance(results, list)
    assert all(isinstance(u, UserResponse) for u in results)
    # Should include VIP and non-VIP users with "ana" in name
    expected_ids = {1, 3}  # Ana Silva and Mariana Costa
    result_ids = {u.id for u in results}
    assert expected_ids == result_ids


def test_search_users_with_vip_prefix_returns_only_vip_users_with_term(mock_users):
    # Query with vip: prefix and term "ana"
    results = search_users("vip:ana")
    assert isinstance(results, list)
    assert all(isinstance(u, UserResponse) for u in results)
    # Only VIP users with "ana" in name
    expected_ids = {1, 3}  # Ana Silva and Mariana Costa (both VIP)
    result_ids = {u.id for u in results}
    assert expected_ids == result_ids
    # No non-VIP users included
    assert all(getattr(u, "is_vip", False) for u in results)


def test_search_users_with_vip_prefix_and_empty_term_returns_all_vip_users(mock_users):
    # Query with vip: prefix and empty term
    results = search_users("vip:")
    assert isinstance(results, list)
    # Should return all VIP users regardless of name
    expected_ids = {1, 3, 5}  # Ana Silva, Mariana Costa, Vipiana
    result_ids = {u.id for u in results}
    assert expected_ids == result_ids
    # All users must be VIP
    assert all(getattr(u, "is_vip", False) for u in results)


def test_search_users_with_vip_prefix_and_nonexistent_term_returns_empty_list(mock_users):
    # Query with vip: prefix and term that does not exist
    results = search_users("vip:xyz")
    assert isinstance(results, list)
    assert results == []


@pytest.mark.parametrize("query", ["VIP:ana", "ViP:ana"])
def test_search_users_vip_prefix_case_insensitive(query, mock_users):
    results = search_users(query)
    expected_ids = {1, 3}  # Ana Silva and Mariana Costa (VIP)
    result_ids = {u.id for u in results}
    assert expected_ids == result_ids
    assert all(getattr(u, "is_vip", False) for u in results)


def test_search_users_with_vip_in_middle_of_term_searches_normally(mock_users):
    # Query with "vip:" in the middle of the term, should not activate VIP filter
    results = search_users("olivip:ia")
    assert isinstance(results, list)
    # Should match users whose name contains "olivip:ia" case-insensitive
    # None of the mock users have this substring, so expect empty list
    assert results == []


def test_search_users_handles_user_without_is_vip(monkeypatch):
    # Prepare users list with one user missing is_vip attribute
    users = [
        UserResponse(id=1, name="Ana Silva", email="ana@example.com", status="ACTIVE", role="USER", is_vip=True),
        UserWithoutIsVip(id=99, name="No VIP Field", email="novipfield@example.com"),
    ]

    def mock_list_users(limit=None, offset=None):
        return users

    monkeypatch.setattr(user_service, "list_users", mock_list_users)

    # Query with vip: prefix, should not raise AttributeError, user without is_vip is ignored
    results = search_users("vip:ana")
    assert isinstance(results, list)
    # Only user with is_vip True and name containing "ana" should be returned
    assert len(results) == 1
    assert results[0].id == 1

    # Query without vip: prefix, should include user without is_vip if name matches
    results_no_prefix = search_users("no vip")
    # The user without is_vip has name "No VIP Field" which contains "no vip" case-insensitive
    # So it should be included
    assert any(u.id == 99 for u in results_no_prefix)


def test_search_users_returns_list_of_userresponse(mock_users):
    results = search_users("ana")
    assert isinstance(results, list)
    for user in results:
        assert isinstance(user, UserResponse)


def test_search_users_filters_out_non_vip_when_vip_prefix_used(mock_users):
    results = search_users("vip:ana")
    # All returned users must have is_vip True
    assert all(getattr(u, "is_vip", False) for u in results)
    # No non-VIP users should be present
    non_vip_users = [u for u in results if not getattr(u, "is_vip", False)]
    assert len(non_vip_users) == 0


def test_get_users_search_endpoint_without_prefix_returns_expected_results(mock_users):
    response = client.get("/users/search", params={"q": "ana"})
    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)
    # Should include both VIP and non-VIP users with "ana" in name
    returned_ids = {u["id"] for u in data}
    expected_ids = {1, 3}
    assert expected_ids == returned_ids


def test_get_users_search_endpoint_with_vip_prefix_returns_only_vip_users(mock_users):
    response = client.get("/users/search", params={"q": "vip:ana"})
    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)
    # All returned users must have is_vip True
    for user in data:
        assert user.get("is_vip") is True
    returned_ids = {u["id"] for u in data}
    expected_ids = {1, 3}
    assert expected_ids == returned_ids


def test_get_users_search_endpoint_with_vip_prefix_and_empty_term_returns_all_vip_users(mock_users):
    response = client.get("/users/search", params={"q": "vip:"})
    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)
    # All users must be VIP
    for user in data:
        assert user.get("is_vip") is True
    expected_ids = {1, 3, 5}
    returned_ids = {u["id"] for u in data}
    assert expected_ids == returned_ids


def test_get_users_search_endpoint_with_vip_prefix_and_nonexistent_term_returns_empty_list(mock_users):
    response = client.get("/users/search", params={"q": "vip:xyz"})
    assert response.status_code == 200
    data = response.json()
    assert data == []


@pytest.mark.parametrize("query", ["VIP:ana", "ViP:ana"])
def test_get_users_search_endpoint_vip_prefix_case_insensitive(query, mock_users):
    response = client.get("/users/search", params={"q": query})
    assert response.status_code == 200
    data = response.json()
    expected_ids = {1, 3}
    returned_ids = {u["id"] for u in data}
    assert expected_ids == returned_ids
    for user in data:
        assert user.get("is_vip") is True


def test_get_users_search_endpoint_with_vip_in_middle_of_term_searches_normally(mock_users):
    response = client.get("/users/search", params={"q": "olivip:ia"})
    assert response.status_code == 200
    data = response.json()
    # No user has this substring, so expect empty list
    assert data == []


def test_get_users_search_endpoint_handles_user_without_is_vip(monkeypatch):
    users = [
        UserResponse(id=1, name="Ana Silva", email="ana@example.com", status="ACTIVE", role="USER", is_vip=True),
        UserWithoutIsVip(id=99, name="No VIP Field", email="novipfield@example.com"),
    ]

    def mock_list_users(limit=None, offset=None):
        return users

    monkeypatch.setattr(user_service, "list_users", mock_list_users)

    response = client.get("/users/search", params={"q": "vip:ana"})
    assert response.status_code == 200
    data = response.json()
    # Only user with is_vip True and name containing "ana" should be returned
    assert len(data) == 1
    assert data[0]["id"] == 1

    response_no_prefix = client.get("/users/search", params={"q": "no vip"})
    assert response_no_prefix.status_code == 200
    data_no_prefix = response_no_prefix.json()
    # User without is_vip should be included if name matches
    assert any(u["id"] == 99 for u in data_no_prefix)