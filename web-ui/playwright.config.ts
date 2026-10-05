import { defineConfig } from "@playwright/test";
const external = process.env.CATFI_BASE_URL;
export default defineConfig({
    testDir: "./tests",
    testMatch: process.env.CATFI_E2E_LIVE
        ? "**/live-pipeline.spec.ts"
        : "**/control-room.spec.ts",
    timeout: 45000,
    workers: 1,
    use: {
        baseURL: external ?? "http://127.0.0.1:5173",
        viewport: { width: 1440, height: 960 },
        trace: "retain-on-failure",
    },
    webServer: external
        ? undefined
        : {
              command: "npm run dev",
              url: "http://127.0.0.1:5173",
              reuseExistingServer: !process.env.CATFI_E2E_LIVE,
              timeout: 30000,
          },
});
