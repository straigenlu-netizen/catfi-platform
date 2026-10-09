import { test, expect } from "@playwright/test";
test("browser trigger reaches the BFF and the Kafka pipeline returns a fresh financial report", async ({
    page,
    request,
}) => {
    const initial = await request.get("/api/ui/dashboard");
    expect(initial.ok()).toBe(true);
    const rows = await initial.json();
    expect(rows.length).toBeGreaterThan(0);
    const id = rows[0].propertyId;
    const previous = await (
        await request.get(`/api/ui/properties/${id}`)
    ).json();
    // This isolated Kafka/BFF test uses a permit-all test filter with CSRF disabled.
    // The real BFF CSRF handshake is covered by dedicated Spring Security tests.
    await page.route("**/api/auth/csrf", (route) =>
        route.fulfill({ json: { token: "integration-test-csrf" } }),
    );
    await page.goto("/simulation");
    await page.getByRole("button", { name: "Run simulation" }).click();
    await expect(page.getByText(/Event ID:/)).toBeVisible();
    await expect
        .poll(
            async () => {
                const details = await (
                    await request.get(`/api/ui/properties/${id}`)
                ).json();
                const report = details.report;
                if (
                    !report ||
                    report.status !== "REPORTED" ||
                    report.reportGeneratedAt ===
                        previous.report?.reportGeneratedAt
                )
                    return false;
                const investments = Object.values(
                    report.currentPortfolio,
                ).reduce<number>((sum, amount) => sum + Number(amount), 0);
                return (
                    Math.abs(
                        report.actualPayout +
                            investments -
                            report.totalCompensation,
                    ) < 0.000001
                );
            },
            { timeout: 30000 },
        )
        .toBe(true);
    await page.goto(`/properties/${id}`);
    await expect(page.getByText("Impact & recovery")).toBeVisible();
    await expect(page.locator("canvas")).toBeVisible();
    await page.screenshot({
        path: "../docs/ui-preview/live-property.png",
        fullPage: true,
    });
});
