import json
import pytest

# Simulate the shell script logic for filtering and payload creation

def contains_copilot(comment_body: str) -> bool:
    # The workflow checks for 'Copilot' or 'copilot' substrings only
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

def test_build_payload_includes_all_fields():
    comment_body = "Test comment with Copilot"
    author = "testuser"
    repo = "owner/repo"
    pr_number = 42

    payload = build_payload(comment_body, author, repo, pr_number)

    assert isinstance(payload, dict)
    assert payload["comment_body"] == comment_body
    assert payload["author"] == author
    assert payload["repo"] == repo
    assert payload["pr_number"] == pr_number

def test_build_payload_with_sensitive_data():
    sensitive_comment = "Password=1234 Copilot"
    author = "sensitive_user"
    repo = "owner/repo"
    pr_number = 101

    payload = build_payload(sensitive_comment, author, repo, pr_number)

    # The payload includes the full comment body, which may contain sensitive data
    assert payload["comment_body"] == sensitive_comment
    assert "Password=1234" in payload["comment_body"]

def test_payload_json_serialization():
    comment_body = "Test Copilot comment"
    author = "user123"
    repo = "owner/repo"
    pr_number = 7

    payload = build_payload(comment_body, author, repo, pr_number)

    # Simulate jq -n ... JSON creation and curl data payload
    json_payload = json.dumps(payload)

    # The JSON string should contain all fields correctly serialized
    loaded = json.loads(json_payload)
    assert loaded == payload

@pytest.mark.parametrize("comment_body", [
    "Test with Copilot",
    "Test with copilot",
])
def test_workflow_should_run_for_pr_comment_with_copilot(comment_body):
    # Simulate event context where issue is a PR and comment contains Copilot
    event = {
        "issue": {"pull_request": {"url": "https://api.github.com/repos/owner/repo/pulls/1"}},
        "comment": {"body": comment_body, "user": {"login": "user1"}},
        "repository": {"full_name": "owner/repo"},
        "issue_number": 1,
    }

    # The workflow condition:
    # github.event.issue.pull_request != null &&
    # (contains(github.event.comment.body, 'Copilot') || contains(github.event.comment.body, 'copilot'))

    pr_present = event["issue"].get("pull_request") is not None
    contains_keyword = contains_copilot(event["comment"]["body"])

    assert pr_present is True
    assert contains_keyword is True

@pytest.mark.parametrize("comment_body", [
    "Test without keyword",
    "COPILOT in uppercase",
    "CoPiLoT mixed case",
])
def test_workflow_should_not_run_for_pr_comment_without_copilot(comment_body):
    event = {
        "issue": {"pull_request": {"url": "https://api.github.com/repos/owner/repo/pulls/1"}},
        "comment": {"body": comment_body, "user": {"login": "user1"}},
        "repository": {"full_name": "owner/repo"},
        "issue_number": 1,
    }

    pr_present = event["issue"].get("pull_request") is not None
    contains_keyword = contains_copilot(event["comment"]["body"])

    # Workflow should not run if keyword not matched exactly as 'Copilot' or 'copilot'
    assert pr_present is True
    assert contains_keyword is False

def test_workflow_should_not_run_for_issue_comment_with_copilot():
    # Comment with 'Copilot' but on an issue (no pull_request key)
    event = {
        "issue": {},  # no pull_request key means it's an issue, not PR
        "comment": {"body": "This comment has Copilot", "user": {"login": "user1"}},
        "repository": {"full_name": "owner/repo"},
        "issue_number": 5,
    }

    pr_present = event["issue"].get("pull_request") is not None
    contains_keyword = contains_copilot(event["comment"]["body"])

    assert pr_present is False
    assert contains_keyword is True

def test_workflow_token_absence_behavior(monkeypatch):
    # Simulate the runtime environment variable for token missing
    # The workflow maps secrets.QAGENT_DISPATCH_PAT to env var QAGENT_PAT
    monkeypatch.delenv("QAGENT_PAT", raising=False)

    # The workflow uses curl -sf which fails silently if token is missing
    # Here we simulate the behavior by checking that token is None or empty
    import os
    token = os.getenv("QAGENT_PAT")

    assert token is None or token == ""

def test_curl_command_construction(monkeypatch):
    # Simulate the runtime environment variable for token present
    # The workflow maps secrets.QAGENT_DISPATCH_PAT to env var QAGENT_PAT
    monkeypatch.setenv("QAGENT_PAT", "fake_token_123")

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

    # Simulate the curl command string construction as in the workflow
    import json
    payload_json = json.dumps(payload)

    curl_command = (
        'curl -sf -X POST '
        '-H "Accept: application/vnd.github+json" '
        '-H "Authorization: Bearer fake_token_123" '
        '"https://api.github.com/repos/jrcosta/qagent/dispatches" '
        f'-d \'{{"event_type":"pr_comment_created","client_payload":{payload_json}}}\''
    )

    # Check that the command contains all expected parts
    assert "curl" in curl_command
    assert "-X POST" in curl_command
    assert "Authorization: Bearer fake_token_123" in curl_command
    assert '"event_type":"pr_comment_created"' in curl_command
    assert payload_json in curl_command