"""Regression checks for CatFi security workflow wiring (stdlib only)."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / ".github" / "workflows" / "security.yml"


class SecurityWorkflowTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.content = WORKFLOW.read_text(encoding="utf-8")

    def test_trivy_action_tag_is_valid_in_both_jobs(self):
        refs = re.findall(r"uses:\s*(aquasecurity/trivy-action@\S+)", self.content)
        self.assertEqual(refs, ["aquasecurity/trivy-action@v0.36.0"] * 2)

    def test_trivy_scans_fail_on_high_severity(self):
        for name in ("repository-scan", "container"):
            block = self.content.split(f"  {name}:\n", 1)[1].split("\n  codeql:", 1)[0] if name == "container" else self.content.split(f"  {name}:\n", 1)[1].split("\n  container:", 1)[0]
            self.assertIn("severity: HIGH,CRITICAL", block)
            self.assertIn("exit-code: '1'", block)

    def test_other_security_jobs_remain_present(self):
        for name in ("secrets", "dependencies", "repository-scan", "container", "codeql", "sonar"):
            self.assertIn(f"  {name}:\n", self.content)


if __name__ == "__main__":
    unittest.main()
