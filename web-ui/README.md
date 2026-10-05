# CatFi Control Room

React, TypeScript, MUI, TanStack Query, Axios and ECharts. All API requests go through the BFF at `/api/ui`.

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
