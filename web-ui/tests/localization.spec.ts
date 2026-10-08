import { test, expect } from "@playwright/test";

test("Japanese selection survives a reload and English can be restored", async ({ page }) => {
    await page.route("**/api/ui/dashboard", (route) => route.fulfill({ json: [] }));
    await page.goto("/");
    await expect(page.getByText("Your control room")).toBeVisible();
    await page.getByRole("combobox", { name: "Language" }).selectOption("ja");
    await expect(page.getByText("管理ダッシュボード")).toBeVisible();
    await expect(page.locator("html")).toHaveAttribute("lang", "ja");
    await page.reload();
    await expect(page.getByText("管理ダッシュボード")).toBeVisible();
    await page.getByRole("combobox", { name: "Language" }).selectOption("en");
    await expect(page.getByText("Your control room")).toBeVisible();
});
