import { test, expect } from "@playwright/test";
const id = "10000000-0000-0000-0000-000000000001";
const policy = {
    policyId: "policy-1",
    coverageRate: 0.9,
    deductible: 5000,
    policyLimit: 100000,
    active: true,
};
const report = {
    totalDamage: 20000,
    totalCompensation: 13000,
    actualPayout: 6500,
    currentPortfolio: { BOND_US10Y: 2600, GOLD: 1950, BTC: 1300, ETH: 650 },
    status: "REPORTED",
    reportGeneratedAt: "2026-10-05T10:00:00Z",
};
const rows = [
    {
        propertyId: id,
        address: "Chiyoda, Tokyo",
        clientName: "Demo Client 1",
        riskProfile: "MODERATE",
        status: "REPORTED",
        totalDamage: 20000,
        totalCompensation: 13000,
        actualPayout: 6500,
        policy,
    },
    {
        propertyId: "p2",
        address: "Shinjuku, Tokyo",
        clientName: "Demo Client 2",
        riskProfile: "CONSERVATIVE",
        status: "SAFE",
        totalDamage: 0,
        totalCompensation: 0,
        actualPayout: 0,
        policy,
    },
    {
        propertyId: "p3",
        address: "Minato, Tokyo",
        clientName: "Demo Client 3",
        riskProfile: "AGGRESSIVE",
        status: "IMPACTED",
        totalDamage: 15000,
        totalCompensation: 0,
        actualPayout: 0,
        policy,
    },
];
test.beforeEach(async ({ page }) => {
    await page.route("**/api/ui/**", async (route) => {
        const url = new URL(route.request().url());
        const data = url.pathname.endsWith("dashboard")
            ? rows
            : url.pathname.endsWith("trigger-disaster")
              ? { eventId: "browser-event-1" }
              : {
                    property: {
                        propertyId: id,
                        address: "Chiyoda, Tokyo",
                        propertyType: "RESIDENTIAL",
                        latitude: 35.67,
                        longitude: 139.7,
                        estimatedValue: 250000,
                    },
                    client: {
                        fullName: "Demo Client 1",
                        riskProfile: "MODERATE",
                    },
                    policy,
                    report,
                };
        await route.fulfill({ json: data });
    });
});
test("dashboard navigation and ECharts financial detail", async ({ page }) => {
    const errors: string[] = [];
    page.on("pageerror", (error) => errors.push(error.message));
    await page.goto("/");
    await expect(page.getByText("Chiyoda, Tokyo")).toBeVisible();
    await page.screenshot({
        path: "../docs/ui-preview/dashboard.png",
        fullPage: true,
    });
    await page
        .getByRole("link", { name: "Chiyoda, Tokyo", exact: true })
        .click();
    await expect(page.getByText("Impact & recovery")).toBeVisible();
    await expect(page.locator("canvas")).toBeVisible();
    await expect(
        page.getByRole("img", { name: /Damage.*compensation.*payout/ }),
    ).toBeVisible();
    await page.screenshot({
        path: "../docs/ui-preview/property.png",
        fullPage: true,
    });
    expect(errors).toEqual([]);
});
test("simulation submits numeric values to the BFF", async ({ page }) => {
    let body: unknown;
    await page.route("**/api/ui/trigger-disaster", async (route) => {
        body = route.request().postDataJSON();
        await route.fulfill({ json: { eventId: "browser-event-1" } });
    });
    await page.goto("/simulation");
    await page
        .getByRole("spinbutton", { name: "Intensity", exact: true })
        .fill("7.5");
    await page.screenshot({
        path: "../docs/ui-preview/simulation.png",
        fullPage: true,
    });
    await page.getByRole("button", { name: "Run simulation" }).click();
    await expect(page.getByText("Event ID: browser-event-1")).toBeVisible();
    expect(body).toEqual({
        type: "EARTHQUAKE",
        intensity: 7.5,
        latitude: 35.6762,
        longitude: 139.6503,
    });
});
test("mobile layout fits the viewport", async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto("/simulation");
    await expect(
        page.getByRole("button", { name: "Run simulation" }),
    ).toBeVisible();
    expect(
        await page.evaluate(
            () => document.documentElement.scrollWidth <= window.innerWidth,
        ),
    ).toBe(true);
    await page.screenshot({
        path: "../docs/ui-preview/mobile.png",
        fullPage: true,
    });
});
