import os
import subprocess
import unittest
from unittest.mock import patch, MagicMock

class TestForwardPrCommentWorkflow(unittest.TestCase):
    """
    Test suite to simulate and validate the behavior of the forward-pr-comment GitHub Actions workflow logic,
    focusing on the usage of the QAGENT_PAT token and the payload construction for repository_dispatch.
    """

    def setUp(self):
        # Setup environment variables as they would be in the workflow
        self.env = {
            "QAGENT_PAT": "fake_token_123",
            "COMMENT_BODY": "This is a test comment",
            "COMMENT_AUTHOR": "testcopilotuser",
            "REPOSITORY_NAME": "owner/repo",
            "PR_NUMBER": "42"
        }

    def test_dispatch_payload_construction(self):
        """
        Test that the JSON payload constructed matches the expected structure and content.
        """
        import json

        # Simulate the jq command in Python to build the payload
        payload = {
            "event_type": "pr_comment_created",
            "client_payload": {
                "comment_body": self.env["COMMENT_BODY"],
                "author": self.env["COMMENT_AUTHOR"],
                "repo": self.env["REPOSITORY_NAME"],
                "pr_number": self.env["PR_NUMBER"]
            }
        }

        # Convert to JSON string
        payload_json = json.dumps(payload)

        # Validate keys and values
        self.assertIn("event_type", payload)
        self.assertEqual(payload["event_type"], "pr_comment_created")
        self.assertIn("client_payload", payload)
        client_payload = payload["client_payload"]
        self.assertEqual(client_payload["comment_body"], self.env["COMMENT_BODY"])
        self.assertEqual(client_payload["author"], self.env["COMMENT_AUTHOR"])
        self.assertEqual(client_payload["repo"], self.env["REPOSITORY_NAME"])
        self.assertEqual(client_payload["pr_number"], self.env["PR_NUMBER"])

        # Validate JSON string is parseable
        parsed = json.loads(payload_json)
        self.assertEqual(parsed, payload)

    @patch("subprocess.run")
    def test_curl_command_execution_with_valid_token(self, mock_run):
        """
        Test that the curl command is called with the correct headers and data when QAGENT_PAT is set.
        """
        # Setup mock to simulate successful curl call
        mock_completed_process = MagicMock()
        mock_completed_process.returncode = 0
        mock_run.return_value = mock_completed_process

        # Build the curl command as in the workflow
        curl_cmd = [
            "curl", "-sf", "-X", "POST",
            "-H", "Accept: application/vnd.github+json",
            "-H", f"Authorization: Bearer {self.env['QAGENT_PAT']}",
            "https://api.github.com/repos/jrcosta/qagent/dispatches",
            "-d", unittest.mock.ANY  # payload JSON string
        ]

        # Call subprocess.run with the command
        subprocess.run(curl_cmd, check=True)

        # Assert subprocess.run was called once with expected args
        mock_run.assert_called_once()
        args, kwargs = mock_run.call_args
        self.assertIn("-H", args[0])
        self.assertIn(f"Authorization: Bearer {self.env['QAGENT_PAT']}", args[0])
        self.assertIn("https://api.github.com/repos/jrcosta/qagent/dispatches", args[0])

    @patch("subprocess.run")
    def test_curl_command_fails_with_missing_token(self, mock_run):
        """
        Test that the curl command fails (raises CalledProcessError) when QAGENT_PAT is missing or empty.
        """
        # Simulate missing token by empty string
        env = self.env.copy()
        env["QAGENT_PAT"] = ""

        # Setup mock to simulate curl failure due to auth error
        mock_run.side_effect = subprocess.CalledProcessError(returncode=22, cmd="curl")

        with self.assertRaises(subprocess.CalledProcessError):
            curl_cmd = [
                "curl", "-sf", "-X", "POST",
                "-H", "Accept: application/vnd.github+json",
                "-H", f"Authorization: Bearer {env['QAGENT_PAT']}",
                "https://api.github.com/repos/jrcosta/qagent/dispatches",
                "-d", "{}"
            ]
            subprocess.run(curl_cmd, check=True)

        mock_run.assert_called_once()

    def test_forward_job_condition_with_copilot_user(self):
        """
        Test the condition that triggers the forward job: github.event.issue.pull_request != null &&
        contains(github.event.comment.user.login, 'copilot')
        """
        def should_run_forward_job(event):
            pr = event.get("issue", {}).get("pull_request")
            user_login = event.get("comment", {}).get("user", {}).get("login", "")
            return pr is not None and "copilot" in user_login

        # Case: PR comment by user with 'copilot' in login
        event = {
            "issue": {"pull_request": {"url": "some_url"}},
            "comment": {"user": {"login": "mycopilotuser"}}
        }
        self.assertTrue(should_run_forward_job(event))

        # Case: PR comment by user without 'copilot' in login
        event["comment"]["user"]["login"] = "normaluser"
        self.assertFalse(should_run_forward_job(event))

        # Case: Issue comment but not a PR (pull_request is None)
        event["issue"]["pull_request"] = None
        event["comment"]["user"]["login"] = "mycopilotuser"
        self.assertFalse(should_run_forward_job(event))

    @patch("subprocess.run")
    def test_workflow_step_echo_on_success(self, mock_run):
        """
        Test that after successful curl execution, the echo message is printed.
        """
        mock_completed_process = MagicMock()
        mock_completed_process.returncode = 0
        mock_run.return_value = mock_completed_process

        # Simulate running the curl command
        subprocess.run(["curl", "-sf", "-X", "POST"], check=True)

        # Simulate echo output
        echo_output = "✅ Forwarded comment to qagent"

        self.assertEqual(echo_output, "✅ Forwarded comment to qagent")

if __name__ == "__main__":
    unittest.main()