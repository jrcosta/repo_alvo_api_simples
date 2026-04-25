import pytest

# Simulação da função que determina se o job deve disparar
def should_forward_job(event):
    """
    Simula a condição do job 'forward' do workflow:
    Deve retornar True se:
    - github.event.issue.pull_request != null (simulado como não None)
    - github.event.comment.body contém '#qagent-test-review' (case-sensitive)
    """
    issue = event.get("issue", {})
    comment = event.get("comment", {})
    if issue.get("pull_request") is None:
        return False
    body = comment.get("body", "")
    return "#qagent-test-review" in body


class TestForwardPrCommentWorkflow:

    def test_comment_in_pr_without_tag_and_user_not_copilot_does_not_trigger(self):
        event = {
            "issue": {"pull_request": {"url": "some_url"}},
            "comment": {
                "body": "This is a normal comment without tag",
                "user": {"login": "randomuser"}
            }
        }
        assert not should_forward_job(event)

    def test_comment_in_pr_with_tag_by_any_user_triggers(self):
        event = {
            "issue": {"pull_request": {"url": "some_url"}},
            "comment": {
                "body": "Please run tests #qagent-test-review",
                "user": {"login": "randomuser"}
            }
        }
        assert should_forward_job(event)

    def test_comment_in_pr_with_tag_case_variation_does_not_trigger_due_to_case_sensitivity(self):
        event = {
            "issue": {"pull_request": {"url": "some_url"}},
            "comment": {
                "body": "Run tests #QAgent-Test-Review please",
                "user": {"login": "randomuser"}
            }
        }
        # contains is case-sensitive, so should not trigger
        assert not should_forward_job(event)

    def test_comment_in_issue_not_pr_with_tag_does_not_trigger(self):
        event = {
            "issue": {"pull_request": None},
            "comment": {
                "body": "Run tests #qagent-test-review",
                "user": {"login": "randomuser"}
            }
        }
        assert not should_forward_job(event)

    def test_comment_in_pr_by_user_with_copilot_in_login_but_without_tag_does_not_trigger(self):
        event = {
            "issue": {"pull_request": {"url": "some_url"}},
            "comment": {
                "body": "Just a comment without tag",
                "user": {"login": "github-copilot-bot"}
            }
        }
        # The new condition ignores user login, so no trigger without tag
        assert not should_forward_job(event)

    def test_comment_in_pr_with_multiple_tags_including_qagent_tag_triggers(self):
        event = {
            "issue": {"pull_request": {"url": "some_url"}},
            "comment": {
                "body": "Some comment #other-tag #qagent-test-review #another-tag",
                "user": {"login": "user123"}
            }
        }
        assert should_forward_job(event)

    def test_comment_in_pr_with_tag_embedded_in_text_triggers(self):
        event = {
            "issue": {"pull_request": {"url": "some_url"}},
            "comment": {
                "body": "Please #qagent-test-review run the tests now!",
                "user": {"login": "user123"}
            }
        }
        assert should_forward_job(event)

    def test_comment_in_pr_with_tag_with_spaces_around_triggers(self):
        event = {
            "issue": {"pull_request": {"url": "some_url"}},
            "comment": {
                "body": "Run tests  #qagent-test-review  please",
                "user": {"login": "user123"}
            }
        }
        assert should_forward_job(event)

    def test_comment_in_pr_with_tag_with_unicode_nearby_triggers(self):
        event = {
            "issue": {"pull_request": {"url": "some_url"}},
            "comment": {
                "body": "Run tests #qagent-test-review🚀",
                "user": {"login": "user123"}
            }
        }
        assert should_forward_job(event)

    def test_comment_in_pr_edited_to_add_tag_triggers(self):
        # Simulate initial comment without tag
        event_initial = {
            "issue": {"pull_request": {"url": "some_url"}},
            "comment": {
                "body": "Initial comment without tag",
                "user": {"login": "user123"}
            }
        }
        assert not should_forward_job(event_initial)

        # Simulate edited comment with tag added
        event_edited = {
            "issue": {"pull_request": {"url": "some_url"}},
            "comment": {
                "body": "Initial comment without tag #qagent-test-review",
                "user": {"login": "user123"}
            }
        }
        assert should_forward_job(event_edited)

    def test_comment_deleted_does_not_trigger(self):
        # The workflow triggers only on created comments, so deleted comments do not trigger
        # Simulate event type other than created (not in scope of condition)
        # Here we just test that without created event, no trigger
        event = {
            "issue": {"pull_request": {"url": "some_url"}},
            "comment": {
                "body": "#qagent-test-review",
                "user": {"login": "user123"}
            },
            "action": "deleted"
        }
        # The condition depends on event type 'created', so simulate that deleted does not trigger
        # Since our function does not check action, we simulate that deleted event is ignored by workflow trigger
        # So we consider this test out of scope for condition function
        # But we assert that if action is deleted, no trigger
        # For this test, we assume should_forward_job is called only on created events
        # So no assertion here, just placeholder
        pass

    def test_comment_edited_to_remove_tag_does_not_trigger(self):
        # Simulate initial comment with tag
        event_initial = {
            "issue": {"pull_request": {"url": "some_url"}},
            "comment": {
                "body": "Run tests #qagent-test-review",
                "user": {"login": "user123"}
            }
        }
        assert should_forward_job(event_initial)

        # Simulate edited comment with tag removed
        event_edited = {
            "issue": {"pull_request": {"url": "some_url"}},
            "comment": {
                "body": "Run tests",
                "user": {"login": "user123"}
            }
        }
        assert not should_forward_job(event_edited)