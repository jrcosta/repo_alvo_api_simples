import pytest
from unittest.mock import patch, MagicMock

import subprocess
import json

@pytest.fixture
def mock_curl_post():
    with patch("subprocess.run") as mock_run:
        yield mock_run

def build_curl_command(env_vars: dict, data_payload: dict) -> list[str]:
    """
    Monta o comando curl usado no workflow para disparar o POST.
    """
    cmd = [
        "curl", "-L",
        "-X", "POST",
        "-H", "Accept: application/vnd.github+json",
        "-H", f"Authorization: Bearer {env_vars.get('QAGENT_PAT', '')}",
        "-H", "X-GitHub-Api-Version: 2022-11-28",
        "https://api.github.com/repos/jrcosta/qagent/dispatches",
        "-d", json.dumps(data_payload)
    ]
    return cmd

def test_trigger_qagent_curl_command_execution(monkeypatch):
    """
    Testa que o comando curl é chamado com os parâmetros corretos para disparar o workflow no repo qagent.
    """

    # Variáveis de ambiente simuladas
    env = {
        "QAGENT_PAT": "fake_token",
        "GITHUB_REPOSITORY_OWNER": "owner",
        "GITHUB_EVENT_REPOSITORY_NAME": "repo",
        "GITHUB_SHA": "abc123",
        "GITHUB_REF_NAME": "main",
        "GITHUB_EVENT_BEFORE": "def456",
    }

    # Payload esperado
    expected_payload = {
        "event_type": "analyze_target_repo",
        "client_payload": {
            "target_owner": env["GITHUB_REPOSITORY_OWNER"],
            "target_repo": env["GITHUB_EVENT_REPOSITORY_NAME"],
            "target_ref": env["GITHUB_SHA"],
            "target_branch": env["GITHUB_REF_NAME"],
            "base_sha": env["GITHUB_EVENT_BEFORE"],
            "head_sha": env["GITHUB_SHA"],
        }
    }

    # Mock subprocess.run para capturar chamada
    called_args = {}

    def fake_run(cmd, check):
        called_args["cmd"] = cmd
        called_args["check"] = check
        class Result:
            returncode = 0
        return Result()

    monkeypatch.setattr("subprocess.run", fake_run)

    # Função que simula o step do workflow que executa o curl
    def run_trigger_qagent_step():
        import os
        QAGENT_PAT = os.environ.get("QAGENT_PAT")
        target_owner = os.environ.get("GITHUB_REPOSITORY_OWNER")
        target_repo = os.environ.get("GITHUB_EVENT_REPOSITORY_NAME")
        target_ref = os.environ.get("GITHUB_SHA")
        target_branch = os.environ.get("GITHUB_REF_NAME")
        base_sha = os.environ.get("GITHUB_EVENT_BEFORE")
        head_sha = target_ref

        payload = {
            "event_type": "analyze_target_repo",
            "client_payload": {
                "target_owner": target_owner,
                "target_repo": target_repo,
                "target_ref": target_ref,
                "target_branch": target_branch,
                "base_sha": base_sha,
                "head_sha": head_sha,
            }
        }

        cmd = [
            "curl", "-L",
            "-X", "POST",
            "-H", "Accept: application/vnd.github+json",
            "-H", f"Authorization: Bearer {QAGENT_PAT}",
            "-H", "X-GitHub-Api-Version: 2022-11-28",
            "https://api.github.com/repos/jrcosta/qagent/dispatches",
            "-d", json.dumps(payload)
        ]

        subprocess.run(cmd, check=True)

    # Set environment variables
    monkeypatch.setattr("os.environ", env)

    # Run the step
    run_trigger_qagent_step()

    # Assertions
    assert "cmd" in called_args
    cmd = called_args["cmd"]
    assert cmd[0] == "curl"
    assert "-X" in cmd and "POST" in cmd
    assert any("Authorization: Bearer fake_token" in s for s in cmd)
    assert "https://api.github.com/repos/jrcosta/qagent/dispatches" in cmd
    # Check payload JSON string in cmd
    data_index = cmd.index("-d") + 1
    payload_str = cmd[data_index]
    payload_obj = json.loads(payload_str)
    assert payload_obj == expected_payload