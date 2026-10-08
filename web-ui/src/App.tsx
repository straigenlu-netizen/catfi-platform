import { ReportExport } from "./ReportExport";
import { useState, lazy, Suspense } from "react";
import { Link, NavLink, Route, Routes, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    MenuItem,
    Paper,
    Stack,
    TextField,
    Typography,
} from "@mui/material";
import DashboardOutlined from "@mui/icons-material/DashboardOutlined";
import BoltOutlined from "@mui/icons-material/BoltOutlined";
import ArrowForward from "@mui/icons-material/ArrowForward";
import ArrowBack from "@mui/icons-material/ArrowBack";
import Refresh from "@mui/icons-material/Refresh";
import { t, setLanguage, useLanguage } from "./i18n";
import { api, label, money, type DisasterType, type Trigger, prepareCsrf } from "./api";
import { AgentPortal, CustomerPortal, PortalLogin } from "./Portals";
const FinancialChart = lazy(() => import("./FinancialChart"));
function Status({ value }: { value: string }) {
    return (
        <Chip
            size="small"
            label={label(value)}
            className={`status status-${value.toLowerCase()}`}
        />
    );
}
function Metric({
    title,
    value,
    detail,
}: {
    title: string;
    value: string;
    detail: string;
}) {
    return (
        <Paper className="metric">
            <span className="eyebrow">{title}</span>
            <strong>{value}</strong>
            <span>{detail}</span>
        </Paper>
    );
}
function ErrorState({ retry }: { retry: () => void }) {
    return (
        <Alert
            severity="error"
            action={
                <Button color="inherit" onClick={retry}>
                    {t("Retry")}
                </Button>
            }
        >
            {t("Could not reach the control room. Check that the BFF and its backend")}
            {t("services are running.")}
        </Alert>
    );
}
export function Dashboard() {
    const query = useQuery({
        queryKey: ["dashboard"],
        queryFn: api.dashboard,
        refetchInterval: 2000,
    });
    const rows = query.data ?? [];
    const sum = (key: "totalDamage" | "totalCompensation" | "actualPayout") =>
        rows.reduce((total, row) => total + row[key], 0);
    return (
        <>
            <div className="page-heading">
                <div>
                    <span className="eyebrow">{t("Portfolio overview")}</span>
                    <Typography variant="h4">{t("Your control room")}</Typography>
                    <p>{t("Follow every property, from impact to recovery.")}</p>
                </div>
                <Button
                    component={Link}
                    to="/simulation"
                    variant="contained"
                    startIcon={<BoltOutlined />}
                >
                    {t("New simulation")}
                </Button>
            </div>
            {query.isError && <ErrorState retry={() => void query.refetch()} />}
            {query.isPending && (
                <Box role="status" className="loading">
                    <CircularProgress size={24} /> {t("Loading properties…")}
                </Box>
            )}
            <div className="metrics">
                <Metric
                    title={t("Monitored properties")}
                    value={String(rows.length)}
                    detail={t("Registered in the policy service")}
                />
                <Metric
                    title={t("Assessed damage")}
                    value={money(sum("totalDamage"))}
                    detail={t("Latest assessments across properties")}
                />
                <Metric
                    title={t("Calculated coverage")}
                    value={money(sum("totalCompensation"))}
                    detail={t("After deductibles and policy limits")}
                />
                <Metric
                    title={t("Simulated payout")}
                    value={money(sum("actualPayout"))}
                    detail={t("Completed customer payments")}
                />
            </div>
            <Paper className="section-panel">
                <div className="section-heading">
                    <div>
                        <Typography variant="h6" sx={{ fontWeight: 700 }}>
                            {t("Property watchlist")}
                        </Typography>
                        <span className="muted">{t("Updates every 2 seconds")}</span>
                    </div>
                    <Button
                        size="small"
                        startIcon={<Refresh />}
                        onClick={() => void query.refetch()}
                    >
                        {t("Refresh")}
                    </Button>
                </div>
                <div className="table-scroll">
                    <table>
                        <thead>
                            <tr>
                                <th>{t("Property")}</th>
                                <th>{t("Owner & strategy")}</th>
                                <th>{t("Status")}</th>
                                <th>{t("Damage")}</th>
                                <th>{t("Coverage")}</th>
                                <th />
                            </tr>
                        </thead>
                        <tbody>
                            {rows.map((row) => (
                                <tr key={row.propertyId}>
                                    <td>
                                        <Link
                                            to={`/properties/${row.propertyId}`}
                                            className="property-link"
                                        >
                                            {row.address}
                                        </Link>
                                        <small>
                                            {t("Registered property · Japan")}
                                        </small>
                                    </td>
                                    <td>
                                        {row.clientName}
                                        <small className="capitalize">
                                            {label(row.riskProfile)}
                                        </small>
                                    </td>
                                    <td>
                                        <Status value={row.status} />
                                    </td>
                                    <td>{money(row.totalDamage)}</td>
                                    <td>{money(row.totalCompensation)}</td>
                                    <td>
                                        <Button
                                            component={Link}
                                            to={`/properties/${row.propertyId}`}
                                            aria-label={`View ${row.address}`}
                                            size="small"
                                        >
                                            <ArrowForward fontSize="small" />
                                        </Button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
                {!query.isPending && !query.isError && rows.length === 0 && (
                    <div className="empty">
                        {t("No properties yet. Start the policy service with its")}
                        {t("demo profile.")}
                    </div>
                )}
            </Paper>
            <p className="note">
                {t("Synthetic clients. Simulated money. A learning environment for")}
                {t("disaster recovery.")}
            </p>
        </>
    );
}
export function Simulation() {
    const [type, setType] = useState<DisasterType>("EARTHQUAKE");
    const [intensity, setIntensity] = useState("7");
    const [latitude, setLatitude] = useState("35.6762");
    const [longitude, setLongitude] = useState("139.6503");
    const [error, setError] = useState("");
    const client = useQueryClient();
    const mutation = useMutation({
        mutationFn: async (request: Trigger) => {
            await prepareCsrf();
            return api.trigger(request);
        },
        onSuccess: () => {
            void client.invalidateQueries({ queryKey: ["dashboard"] });
        },
    });
    function submit(event: React.FormEvent) {
        event.preventDefault();
        const raw = [intensity, latitude, longitude];
        const [strength, lat, lon] = raw.map(Number);
        if (
            raw.some((value) => value.trim() === "") ||
            ![strength, lat, lon].every(Number.isFinite) ||
            strength <= 0 ||
            strength > 10 ||
            lat < -90 ||
            lat > 90 ||
            lon < -180 ||
            lon > 180
        ) {
            setError(t("Enter valid intensity and coordinates."));
            return;
        }
        setError("");
        mutation.mutate({
            type,
            intensity: strength,
            latitude: lat,
            longitude: lon,
        });
    }
    return (
        <>
            <div className="page-heading">
                <div>
                    <span className="eyebrow">{t("Scenario lab")}</span>
                    <Typography variant="h4">{t("Start a simulation")}</Typography>
                    <p>{t("One event. An entire recovery pipeline.")}</p>
                </div>
                <Chip label={t("Simulation environment")} variant="outlined" />
            </div>
            <div className="simulation-layout">
                <Paper className="section-panel form-panel">
                    <Typography variant="h6" sx={{ fontWeight: 700 }}>
                        {t("Disaster parameters")}
                    </Typography>
                    <p className="muted">
                        {t("Create a synthetic event and publish it to the platform.")}
                    </p>
                    <form onSubmit={submit}>
                        <Stack spacing={3}>
                            <TextField
                                select
                                label={t("Disaster type")}
                                value={type}
                                onChange={(event) =>
                                    setType(event.target.value as DisasterType)
                                }
                            >
                                {[
                                    "EARTHQUAKE",
                                    "FLOOD",
                                    "TYPHOON",
                                    "TSUNAMI",
                                ].map((value) => (
                                    <MenuItem value={value} key={value}>
                                        {label(value)}
                                    </MenuItem>
                                ))}
                            </TextField>
                            <TextField
                                label={t("Intensity")}
                                type="number"
                                value={intensity}
                                onChange={(event) =>
                                    setIntensity(event.target.value)
                                }
                                slotProps={{
                                    htmlInput: { step: 0.1, min: 0.1, max: 10 },
                                }}
                                helperText={t("Relative simulation scale, 0–10")}
                                required
                            />
                            <div className="coordinates">
                                <TextField
                                    label={t("Latitude")}
                                    type="number"
                                    value={latitude}
                                    onChange={(event) =>
                                        setLatitude(event.target.value)
                                    }
                                    slotProps={{
                                        htmlInput: {
                                            step: 0.0001,
                                            min: -90,
                                            max: 90,
                                        },
                                    }}
                                    required
                                />
                                <TextField
                                    label={t("Longitude")}
                                    type="number"
                                    value={longitude}
                                    onChange={(event) =>
                                        setLongitude(event.target.value)
                                    }
                                    slotProps={{
                                        htmlInput: {
                                            step: 0.0001,
                                            min: -180,
                                            max: 180,
                                        },
                                    }}
                                    required
                                />
                            </div>
                            {error && <Alert severity="error">{error}</Alert>}
                            {mutation.isError && (
                                <Alert severity="error">
                                    {t("The event could not be published. Check the")}
                                    {t("BFF, simulator and Kafka, then retry.")}
                                </Alert>
                            )}
                            {mutation.isSuccess && (
                                <Alert severity="success">
                                    {t("Event published.")}{" "}
                                    <Link to="/">{t("Watch the dashboard")}</Link> {t("as reports arrive.")}
                                    <small className="event-id">
                                        {t("Event ID:")} {mutation.data.eventId}
                                    </small>
                                </Alert>
                            )}
                            <Button
                                type="submit"
                                variant="contained"
                                size="large"
                                startIcon={
                                    mutation.isPending ? (
                                        <CircularProgress
                                            size={18}
                                            color="inherit"
                                        />
                                    ) : (
                                        <BoltOutlined />
                                    )
                                }
                                disabled={mutation.isPending}
                            >
                                {mutation.isPending
                                    ? t("Publishing event…")
                                    : t("Run simulation")}
                            </Button>
                        </Stack>
                    </form>
                </Paper>
                <div className="scenario-aside">
                    <span className="eyebrow">{t("What happens next")}</span>
                    <h2>
                        {t("From impact")}
                        <br />
                        {t("to insight.")}
                    </h2>
                    <p>
                        {t("The platform generates sensor observations, assesses")}
                        {t("damage and applies the property’s policy. Compensation")}
                        {t("is split into a simulated payout and investments.")}
                    </p>
                    <div className="pipeline-steps">
                        {[
                            "Disaster event",
                            "Sensor observations",
                            "Damage assessment",
                            "Compensation & allocation",
                            "Financial report",
                        ].map((item, index) => (
                            <div key={item}>
                                <span>
                                    {String(index + 1).padStart(2, "0")}
                                </span>
                                {t(item)}
                            </div>
                        ))}
                    </div>
                    <p className="aside-footnote">
                        {t("Current demo: all registered properties receive")}
                        {t("observations. Location and intensity are retained as")}
                        {t("event metadata; geographic and physical damage models")}
                        {t("are planned.")}
                    </p>
                </div>
            </div>
        </>
    );
}
export function PropertyDetails() {
    const { id } = useParams();
    const query = useQuery({
        queryKey: ["property", id],
        queryFn: () => api.details(id!),
        enabled: Boolean(id),
        refetchInterval: 2000,
    });
    if (query.isPending)
        return (
            <Box role="status" className="loading">
                <CircularProgress size={24} /> {t("Loading property…")}
            </Box>
        );
    if (query.isError) return <ErrorState retry={() => void query.refetch()} />;
    const { property, policy, client, report } = query.data!;
    const investments = report?.currentPortfolio ?? {};
    return (
        <>
            <Button component={Link} to="/" startIcon={<ArrowBack />}>
                {t("Back to dashboard")}
            </Button>
            <div className="page-heading">
                <div>
                    <span className="eyebrow">{t("Property intelligence")}</span>
                    <Typography variant="h4">{property.address}</Typography>
                    <p>
                        {client.fullName} · {label(property.propertyType)} ·
                        {t("Japan")}
                    </p>
                </div>
                <Status value={report?.status ?? "SAFE"} />
            </div>
            <ReportExport propertyId={property.propertyId} ready={report?.status === "REPORTED"} />
            <div className="metrics">
                <Metric
                    title={t("Assessed damage")}
                    value={money(report?.totalDamage ?? 0)}
                    detail={t("Latest disaster assessment")}
                />
                <Metric
                    title={t("Calculated coverage")}
                    value={money(report?.totalCompensation ?? 0)}
                    detail={t("Compensation under the active policy")}
                />
                <Metric
                    title={t("Simulated payout")}
                    value={money(report?.actualPayout ?? 0)}
                    detail={t("Amount paid to the customer")}
                />
                <Metric
                    title={t("Investment allocation")}
                    value={money(
                        Object.values(investments).reduce((a, b) => a + b, 0),
                    )}
                    detail={`${label(client.riskProfile)} ${t("strategy")}`}
                />
            </div>
            {!report && (
                <Alert severity="info">
                    {t("No disaster report yet. Run a simulation to see the")}
                    {t("financial breakdown.")}
                </Alert>
            )}
            <div className="detail-layout">
                <Paper className="section-panel">
                    <Typography variant="h6" sx={{ fontWeight: 700 }}>
                        {t("Impact & recovery")}
                    </Typography>
                    <p className="muted">
                        {t("Damage, policy compensation and customer payout in EUR")}
                    </p>
                    <Suspense fallback={<CircularProgress size={20} />}>
                        <FinancialChart
                            damage={report?.totalDamage ?? 0}
                            compensation={report?.totalCompensation ?? 0}
                            payout={report?.actualPayout ?? 0}
                        />
                    </Suspense>
                </Paper>
                <Paper className="section-panel">
                    <Typography variant="h6" sx={{ fontWeight: 700 }}>
                        {t("Policy at a glance")}
                    </Typography>
                    {policy ? (
                        <dl>
                            <dt>{t("Coverage rate")}</dt>
                            <dd>{Math.round(policy.coverageRate * 100)}%</dd>
                            <dt>{t("Deductible")}</dt>
                            <dd>{money(policy.deductible)}</dd>
                            <dt>{t("Policy limit")}</dt>
                            <dd>{money(policy.policyLimit)}</dd>
                            <dt>{t("Property value")}</dt>
                            <dd>{money(property.estimatedValue)}</dd>
                        </dl>
                    ) : (
                        <Alert severity="warning">
                            {t("No active policy is available.")}
                        </Alert>
                    )}
                </Paper>
            </div>
            <Paper className="section-panel">
                <Typography variant="h6" sx={{ fontWeight: 700 }}>
                    {t("Investment breakdown")}
                </Typography>
                <p className="muted">
                    {t("Allocated amounts, not current market valuations")}
                </p>
                {Object.entries(investments).length ? (
                    <div className="allocation-grid">
                        {Object.entries(investments).map(([asset, amount]) => (
                            <div className="allocation" key={asset}>
                                <span>{asset}</span>
                                <strong>{money(amount)}</strong>
                            </div>
                        ))}
                    </div>
                ) : (
                    <p className="muted">
                        {t("Investment allocations will appear after processing")}
                        {t("completes.")}
                    </p>
                )}
            </Paper>
        </>
    );
}
export default function App() {
    const language = useLanguage();
    return (
        <div className="app-shell">
            <aside className="sidebar">
                <Link to="/" className="brand">
                    <span className="brand-mark">C</span>CatFi
                    <span className="brand-dot">.</span>
                </Link>
                <span className="sidebar-caption">{t("CONTROL ROOM")}</span>
                <nav>
                    <NavLink to="/" end>
                        <DashboardOutlined />
                        {t("Dashboard")}
                    </NavLink>
                    <NavLink to="/simulation">
                        <BoltOutlined />
                        {t("Simulation lab")}
                    </NavLink>
                    <NavLink to="/agent">{t("Agent portal")}</NavLink>
                    <NavLink to="/customer">{t("Customer portal")}</NavLink>
                </nav>
                <div className="sidebar-footer">
                    {t("● Local demo")}<span>{t("Japan · Synthetic portfolio")}</span>
                </div>
            </aside>
            <div className="workspace">
                <header className="topbar">
                    <span>{t("Disaster recovery platform")}</span>
                    <div className="locale-switcher">
                        <label htmlFor="catfi-language">Language / 言語</label>
                        <select id="catfi-language" aria-label="Language" value={language}
                            onChange={(event) => setLanguage(event.target.value as "en" | "ja")}>
                            <option value="en">English</option>
                            <option value="ja">日本語</option>
                        </select>
                        <Chip label="DEMO" size="small" variant="outlined" />
                    </div>
                </header>
                <main>
                    <Routes>
                        <Route path="/" element={<Dashboard />} />
                        <Route path="/simulation" element={<Simulation />} />
                        <Route path="/agent" element={<AgentPortal />} />
                        <Route path="/customer" element={<CustomerPortal />} />
                        <Route path="/login" element={<PortalLogin />} />
                        <Route
                            path="/properties/:id"
                            element={<PropertyDetails />}
                        />
                        <Route
                            path="*"
                            element={
                                <Alert severity="info">
                                    {t("Page not found.")}{" "}
                                    <Link to="/">{t("Return to dashboard")}</Link>.
                                </Alert>
                            }
                        />
                    </Routes>
                </main>
            </div>
        </div>
    );
}
