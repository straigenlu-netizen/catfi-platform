import { test, expect } from "@playwright/test";
const customer = {
    clientId: "20000000-0000-0000-0000-000000000001",
    fullName: "Demo Client 1",
    email: "demo1@example.com",
    riskProfile: "MODERATE",
};

test("customer can view only own policy, payout history and edit contact data", async ({ page }) => {
    await page.route("**/api/auth/me", route => route.fulfill({ json: {
        username: "demo1@example.com", role: "CUSTOMER",
    } }));
    await page.route("**/api/auth/csrf", route => route.fulfill({ json: { token: "test-csrf" } }));
    let saved: { fullName: string; email: string } | undefined;
    await page.route("**/api/customer/profile", async route => {
        if (route.request().method() === "PUT") {
            saved = route.request().postDataJSON();
            expect(route.request().headers()["x-xsrf-token"]).toBe("test-csrf");
            return route.fulfill({ json: { ...customer, ...saved } });
        }
        return route.fulfill({ json: customer });
    });
    await page.route("**/api/customer/policies", route => route.fulfill({ json: [{
        property: { propertyId: "property1", address: "Chiyoda, Tokyo", propertyType: "RESIDENTIAL", estimatedValue: 250000 },
        client: customer,
        policy: { policyId: "policy1", active: true, policyLimit: 100000, coverageRate: .9, deductible: 5000 },
        report: { status: "REPORTED", totalCompensation: 13000, actualPayout: 6500 },
    }] }));
    await page.route("**/api/customer/payments", route => route.fulfill({ json: [{
        paymentId: "payment1", propertyId: "property1", compensationResultId: "result1",
        amount: 6500, status: "COMPLETED", initiatedAt: "2026-10-08T12:00:00Z", completedAt: "2026-10-08T12:01:00Z",
    }] }));
    await page.goto("/customer");
    await expect(page.getByText("My policies and claims")).toBeVisible();
    await expect(page.getByText("Chiyoda, Tokyo")).toBeVisible();
    await expect(page.getByText("Payment history")).toBeVisible();
    await page.getByRole("textbox", { name: "Full name" }).fill("Updated Client");
    await page.getByRole("button", { name: "Save changes" }).click();
    await expect(page.getByText("Profile updated.")).toBeVisible();
    expect(saved).toEqual({ fullName: "Updated Client", email: "demo1@example.com" });
});

test("customer role cannot open the agent dashboard", async ({ page }) => {
    await page.route("**/api/auth/me", route => route.fulfill({ json: {
        username: "demo1@example.com", role: "CUSTOMER",
    } }));
    let protectedRequests = 0;
    await page.route("**/api/agent/**", route => { protectedRequests++; return route.fulfill({ status: 403 }); });
    await page.goto("/agent");
    await expect(page.getByText("You do not have access to this portal.")).toBeVisible();
    expect(protectedRequests).toBe(0);
});

test("agent can browse insured clients and simulated payment history", async ({ page }) => {
    await page.route("**/api/auth/me", route => route.fulfill({ json: {
        username: "demo.agent", role: "AGENT",
    } }));
    await page.route("**/api/agent/clients", route => route.fulfill({ json: [{
        clientId: customer.clientId, fullName: customer.fullName, email: customer.email,
        riskProfile: customer.riskProfile, propertiesCount: 1,
    }] }));
    await page.route("**/api/agent/properties", route => route.fulfill({ json: [{
        propertyId: "property1", address: "Chiyoda, Tokyo", clientName: customer.fullName,
        riskProfile: customer.riskProfile, status: "REPORTED", totalDamage: 20000,
        totalCompensation: 13000, actualPayout: 6500, policy: null,
    }] }));
    await page.route("**/api/agent/payments", route => route.fulfill({ json: [] }));
    await page.goto("/agent");
    await expect(page.getByText("Insurance clients")).toBeVisible();
    await expect(page.getByText("demo1@example.com")).toBeVisible();
    await expect(page.getByText("All insured properties")).toBeVisible();
});
