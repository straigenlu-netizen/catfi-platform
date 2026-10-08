# CatFi Control Room

React, TypeScript, MUI, TanStack Query, Axios and ECharts. All browser APIs go through the BFF (`/api/ui`, `/api/customer`, `/api/agent`).

## Customer and agent portals

Navigate to `/customer` or `/agent`, or use the links in the Control Room sidebar.
`/login` posts a Spring Security form login with a CSRF header and uses the session cookie.
Customer profile edits send only `fullName` and `email`; risk and ownership remain immutable.
Customer policies are filtered by server-derived identity. Agents can list insured clients,
property summaries and payment history. All new UI strings support English and Japanese.
Run `npm test`, `npm run build`, and `npm run test:e2e` to verify.

```powershell
npm ci
npm run dev
```

Open http://localhost:5173. Vite proxies `/api` to http://127.0.0.1:8087. Start the demo services first; see the root README.

```powershell
npm test
npm run build
```

Dashboard and details poll every two seconds. Trigger confirmation means Kafka acknowledged the event, not that the financial pipeline has finished. Charts show EUR amounts and investment allocations, not current portfolio valuations. Geographic filtering and intensity-driven physical models are planned.

## Optional authenticated AWS exports

Set VITE_EXPORT_API_URL, VITE_COGNITO_REGION and VITE_COGNITO_CLIENT_ID in .env.local (see .env.example). The property page then displays a Cognito sign-in and JSON export panel. Export API requires a JWT and an operator-created property grant; a CatFi client profile alone gives no permission. Token stays in memory and the password is cleared after login. API CORS BrowserOrigins must include this UI origin.

ReportExport.test.tsx tests login/export and authorization failures. Run the browser export contract with CATFI_E2E_EXPORT=1 plus mock export/Cognito configuration; see tests/aws-export.spec.ts. Default browser tests and live Kafka/browser tests remain separate. Current dashboard/BFF is still a local demo and has no production login flow.
