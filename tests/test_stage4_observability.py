"""Static checks for optional, non-public monitoring; no Docker required."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]

class Stage4ConfigurationTest(unittest.TestCase):
    def test_scrape_uses_authenticated_bff(self):
        text = (ROOT / "infra/monitoring/prometheus.yml").read_text()
        self.assertIn("catfi-bff:8087", text)
        self.assertIn("password_file: /etc/prometheus/agent-password", text)
        self.assertNotIn("password: ", text)

    def test_metrics_restricted_to_agents(self):
        text = (ROOT / "services/catfi-bff/src/main/java/dev/catfi/bff/security/AgentSecurityConfiguration.java").read_text()
        self.assertIn('.requestMatchers("/actuator/**").hasRole("AGENT")', text)
        self.assertIn('.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()', text)

    def test_load_scenarios_do_not_embed_password(self):
        text = (ROOT / "services/performance-tests/src/test/java/dev/catfi/performance/CatFiLoadSimulation.java").read_text()
        self.assertIn('CATFI_LOAD_AGENT_PASSWORD', text)
        self.assertIn('jsonPath("$.token")', text)
        self.assertIn('"X-XSRF-TOKEN"', text)
        self.assertNotIn('agent_391', text)

    def test_monitored_ports_bound_to_loopback(self):
        text = (ROOT / "compose.yml").read_text()
        self.assertIn('127.0.0.1:9091:9090', text)
        self.assertIn('127.0.0.1:3000:3000', text)
        self.assertIn('profiles: [monitoring]', text)

if __name__ == "__main__":
    unittest.main()
