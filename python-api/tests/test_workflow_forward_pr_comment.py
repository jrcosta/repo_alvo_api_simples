import json
import pytest
import os

# Simulate the shell script logic for filtering and payload creation

def contains_copilot(comment_body: str) -> bool:
    # The workflow checks for 'Copilot' or 'copilot' substrings only
    if not isinstance(comment_body, str):
        return False
    return 'Copilot' in comment_body or 'copilot' in comment_body

def build_payload(comment_body: str, author: str, repo: str, pr_number: int) -> dict:
    return {
        "comment_body": comment_body,
        "author": author,
        "repo": repo,
        "pr_number": pr_number,
    }

@pytest.mark.parametrize("comment_body,expected", [
    ("This is a test with Copilot", True),
    ("This is a test with copilot", True),
    ("This is a test with COPILOT", False),
    ("This is a test with CoPiLoT", False),
    ("No keyword here", False),
    ("copilots are great", True),  # contains 'copilot' substring
    ("Copilots are great", True),  # contains 'Copilot' substring
    ("", False),
])
def test_contains_copilot_filter(comment_body, expected):
    assert contains_copilot(comment_body) == expected

@pytest.mark.parametrize("non_string_input", [
    None,
    123,
    12.34,
    [],
    {},
    True,
    False,
])
def test_contains_copilot_with_non_string_inputs_returns_false(non_string_input):
    assert contains_copilot(non_string_input) is False

@pytest.mark.parametrize("comment_body", [
    "mycopilot is here",
    "the copilots are ready",
    "this is a Copilot test",
    "copilot",
    "Copilot",
])
def test_contains_copilot_with_copilot_as_substring(comment_body):
    # According to current implementation, substrings containing 'copilot' or 'Copilot' return True
    assert contains_copilot(comment_body) is True

@pytest.mark.parametrize("author,repo,pr_number", [
    (None, None, None),
    ("", "", 0),
    ("author", None, 0),
    (None, "repo", -1),
    ("", "repo", 1),
])
def test_build_payload_with_null_or_empty_fields(author, repo, pr_number):
    comment_body = "Test comment"
    payload = build_payload(comment_body, author, repo, pr_number)
    assert payload["comment_body"] == comment_body
    assert payload["author"] == author
    assert payload["repo"] == repo
    assert payload["pr_number"] == pr_number

@pytest.mark.parametrize("comment_body", [
    "Unicode test: 測試 Copilot",
    "Emoji test: 🚀 copilot",
    "Special chars: !@#$%^&*() Copilot",
])
def test_payload_json_serialization_with_special_characters(comment_body):
    author = "user_unicode"
    repo = "owner/repo_unicode"
    pr_number = 123

    payload = build_payload(comment_body, author, repo, pr_number)
    json_payload = json.dumps(payload)
    loaded = json.loads(json_payload)
    assert loaded == payload
    assert loaded["comment_body"] == comment_body

@pytest.mark.parametrize("token_value", [
    "valid_token_123",
    "",
    "   ",
    None,
])
def test_curl_command_construction_with_various_tokens(monkeypatch, token_value):
    if token_value is None:
        monkeypatch.delenv("QAGENT_PAT", raising=False)
    else:
        monkeypatch.setenv("QAGENT_PAT", token_value)

    comment_body = "Test Copilot comment"
    author = "user123"
    repo = "owner/repo"
    pr_number = 7

    payload = {
        "comment_body": comment_body,
        "author": author,
        "repo": repo,
        "pr_number": pr_number,
    }

    payload_json = json.dumps(payload)

    token = os.getenv("QAGENT_PAT")
    if token and token.strip():
        curl_command = (
            'curl -sf -X POST '
            '-H "Accept: application/vnd.github+json" '
            f'-H "Authorization: Bearer {token.strip()}" '
            '"https://api.github.com/repos/jrcosta/qagent/dispatches" '
            f'-d \'{{"event_type":"pr_comment_created","client_payload":{payload_json}}}\''
        )
        assert "curl" in curl_command
        assert "-X POST" in curl_command
        assert f"Authorization: Bearer {token.strip()}" in curl_command
        assert '"event_type":"pr_comment_created"' in curl_command
        assert payload_json in curl_command
    else:
        # When token is empty or None, simulate that curl command should not be constructed or is empty
        # Here we just assert token is falsy and no command is constructed
        assert not token or not token.strip()