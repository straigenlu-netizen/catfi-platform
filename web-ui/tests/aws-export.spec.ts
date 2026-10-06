import { test, expect } from "@playwright/test";
const propertyId = "10000000-0000-0000-0000-000000000001";

test("authenticated export follows job completion and clears session on logout", async ({ page }) => {
    await page.route("**/api/ui/**", (route) => route.fulfill({ json: {
        property: { propertyId, address: "Tokyo", propertyType: "RESIDENTIAL", latitude: 35, longitude: 139, estimatedValue: 100000 },
        client: { fullName: "Client", riskProfile: "MODERATE" }, policy: null,
        report: { totalDamage: 20, totalCompensation: 10, actualPayout: 5, currentPortfolio: { GOLD: 5 }, status: "REPORTED", reportGeneratedAt: "2026-01-01T00:00:00Z" },
    } }));
    await page.route("https://cognito-idp.eu-central-1.amazonaws.com/**", (route) => route.fulfill({ json: { AuthenticationResult: { IdToken: "test-jwt" } } }));
    let polls = 0;
    await page.route("https://exports.example/**", async (route) => {
        const request = route.request();
        expect(request.headers().authorization).toBe("Bearer test-jwt");
        if (request.method() === "POST") {
            expect(request.headers()["idempotency-key"]).toBeTruthy();
            return route.fulfill({ status: 202, json: { jobId: "job", status: "PENDING" } });
        }
        if (request.url().endsWith("/download")) return route.fulfill({ json: { url: "https://bucket.s3.amazonaws.com/report.json" } });
        return route.fulfill({ json: { status: ++polls === 1 ? "RUNNING" : "SUCCEEDED" } });
    });
    await page.goto(`/properties/${propertyId}`);
    await page.getByLabel("Email").fill("user@example.com");
    await page.getByLabel("Password").fill("test-password");
    await page.getByRole("button", { name: "Sign in for exports" }).click();
    await page.getByRole("button", { name: "Export JSON report" }).click();
    await expect(page.getByRole("link", { name: /Download JSON/ })).toHaveAttribute("href", "https://bucket.s3.amazonaws.com/report.json");
    await page.getByRole("button", { name: "Sign out" }).click();
    await expect(page.getByLabel("Password")).toHaveValue("");
    await expect(page.getByRole("link", { name: /Download JSON/ })).toHaveCount(0);
});
