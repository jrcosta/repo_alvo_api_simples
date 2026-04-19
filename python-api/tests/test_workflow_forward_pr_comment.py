import pytest

def should_workflow_trigger(event):
    """
    Simulates the workflow 'if' condition logic:
    github.event.issue.pull_request != null &&
    contains(github.event.comment.user.login, 'copilot')
    """
    pr = event.get("issue", {}).get("pull_request")
    user_login = event.get("comment", {}).get("user", {}).get("login", "")
    return pr is not None and "copilot" in user_login

@pytest.mark.parametrize(
    "event,expected",
    [
        # 1. Comentário por usuário com login contendo "copilot"
        (
            {
                "issue": {"pull_request": {"url": "some_url"}},
                "comment": {"user": {"login": "github-copilot-bot"}, "body": "any text"},
            },
            True,
        ),
        # 2. Comentário por usuário sem "copilot" no login, mas com palavra "Copilot" no corpo
        (
            {
                "issue": {"pull_request": {"url": "some_url"}},
                "comment": {"user": {"login": "usuario123"}, "body": "Please Copilot do this"},
            },
            False,
        ),
        # 3. Comentário por usuário sem "copilot" no login e sem a palavra "Copilot" no corpo
        (
            {
                "issue": {"pull_request": {"url": "some_url"}},
                "comment": {"user": {"login": "usuario123"}, "body": "Hello world"},
            },
            False,
        ),
        # 4. Comentário em issue que não seja PR
        (
            {
                "issue": {},  # no pull_request key
                "comment": {"user": {"login": "github-copilot-bot"}, "body": "any text"},
            },
            False,
        ),
        # 5. Comentário em issue que seja PR, user login contains "mycopilotbot" (substring)
        (
            {
                "issue": {"pull_request": {"url": "some_url"}},
                "comment": {"user": {"login": "mycopilotbot"}, "body": "any text"},
            },
            True,
        ),
    ],
)
def test_workflow_trigger_condition(event, expected):
    assert should_workflow_trigger(event) is expected


def generate_payload(comment_body, author_login, repo, pr_number):
    """
    Simulates the payload generation in the workflow.
    """
    return {
        "comment_body": comment_body,
        "author": author_login,
        "repo": repo,
        "pr_number": pr_number,
    }

@pytest.mark.parametrize(
    "comment_body,author_login,repo,pr_number,expected_payload",
    [
        (
            "This is a test comment",
            "github-copilot-bot",
            "owner/repo",
            "123",
            {
                "comment_body": "This is a test comment",
                "author": "github-copilot-bot",
                "repo": "owner/repo",
                "pr_number": "123",
            },
        ),
        (
            "Another comment",
            "mycopilotbot",
            "owner/repo",
            "456",
            {
                "comment_body": "Another comment",
                "author": "mycopilotbot",
                "repo": "owner/repo",
                "pr_number": "456",
            },
        ),
    ],
)
def test_payload_generation(comment_body, author_login, repo, pr_number, expected_payload):
    payload = generate_payload(comment_body, author_login, repo, pr_number)
    assert payload == expected_payload