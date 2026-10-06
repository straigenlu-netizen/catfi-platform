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
import { api, label, money, type DisasterType } from "./api";
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
                    Retry
                </Button>
            }
        >
            Could not reach the control room. Check that the BFF and its backend
            services are running.
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
                    <span className="eyebrow">Portfolio overview</span>
                    <Typography variant="h4">Your control room</Typography>
                    <p>Follow every property, from impact to recovery.</p>
                </div>
                <Button
                    component={Link}
                    to="/simulation"
                    variant="contained"
                    startIcon={<BoltOutlined />}
                >
                    New simulation
                </Button>
            </div>
            {query.isError && <ErrorState retry={() => void query.refetch()} />}
            {query.isPending && (
                <Box role="status" className="loading">
                    <CircularProgress size={24} /> Loading properties…
                </Box>
            )}
            <div className="metrics">
                <Metric
                    title="Monitored properties"
                    value={String(rows.length)}
                    detail="Registered in the policy service"
                />
                <Metric
                    title="Assessed damage"
                    value={money(sum("totalDamage"))}
                    detail="Latest assessments across properties"
                />
                <Metric
                    title="Calculated coverage"
                    value={money(sum("totalCompensation"))}
                    detail="After deductibles and policy limits"
                />
                <Metric
                    title="Simulated payout"
                    value={money(sum("actualPayout"))}
                    detail="Completed customer payments"
                />
            </div>
            <Paper className="section-panel">
                <div className="section-heading">
                    <div>
                        <Typography variant="h6" sx={{ fontWeight: 700 }}>
                            Property watchlist
                        </Typography>
                        <span className="muted">Updates every 2 seconds</span>
                    </div>
                    <Button
                        size="small"
                        startIcon={<Refresh />}
                        onClick={() => void query.refetch()}
                    >
                        Refresh
                    </Button>
                </div>
                <div className="table-scroll">
                    <table>
                        <thead>
                            <tr>
                                <th>Property</th>
                                <th>Owner & strategy</th>
                                <th>Status</th>
                                <th>Damage</th>
                                <th>Coverage</th>
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
                                            Registered property · Japan
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
                        No properties yet. Start the policy service with its
                        demo profile.
                    </div>
                )}
            </Paper>
            <p className="note">
                Synthetic clients. Simulated money. A learning environment for
                disaster recovery.
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
        mutationFn: api.trigger,
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
            setError("Enter valid intensity and coordinates.");
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
                    <span className="eyebrow">Scenario lab</span>
                    <Typography variant="h4">Start a simulation</Typography>
                    <p>One event. An entire recovery pipeline.</p>
                </div>
                <Chip label="Simulation environment" variant="outlined" />
            </div>
            <div className="simulation-layout">
                <Paper className="section-panel form-panel">
                    <Typography variant="h6" sx={{ fontWeight: 700 }}>
                        Disaster parameters
                    </Typography>
                    <p className="muted">
                        Create a synthetic event and publish it to the platform.
                    </p>
                    <form onSubmit={submit}>
                        <Stack spacing={3}>
                            <TextField
                                select
                                label="Disaster type"
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
                                label="Intensity"
                                type="number"
                                value={intensity}
                                onChange={(event) =>
                                    setIntensity(event.target.value)
                                }
                                slotProps={{
                                    htmlInput: { step: 0.1, min: 0.1, max: 10 },
                                }}
                                helperText="Relative simulation scale, 0–10"
                                required
                            />
                            <div className="coordinates">
                                <TextField
                                    label="Latitude"
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
                                    label="Longitude"
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
                                    The event could not be published. Check the
                                    BFF, simulator and Kafka, then retry.
                                </Alert>
                            )}
                            {mutation.isSuccess && (
                                <Alert severity="success">
                                    Event published.{" "}
                                    <Link to="/">Watch the dashboard</Link> as
                                    reports arrive.
                                    <small className="event-id">
                                        Event ID: {mutation.data.eventId}
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
                                    ? "Publishing event…"
                                    : "Run simulation"}
                            </Button>
                        </Stack>
                    </form>
                </Paper>
                <div className="scenario-aside">
                    <span className="eyebrow">What happens next</span>
                    <h2>
                        From impact
                        <br />
                        to insight.
                    </h2>
                    <p>
                        The platform generates sensor observations, assesses
                        damage and applies the property’s policy. Compensation
                        is split into a simulated payout and investments.
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
                                {item}
                            </div>
                        ))}
                    </div>
                    <p className="aside-footnote">
                        Current demo: all registered properties receive
                        observations. Location and intensity are retained as
                        event metadata; geographic and physical damage models
                        are planned.
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
                <CircularProgress size={24} /> Loading property…
            </Box>
        );
    if (query.isError) return <ErrorState retry={() => void query.refetch()} />;
    const { property, policy, client, report } = query.data!;
    const investments = report?.currentPortfolio ?? {};
    return (
        <>
            <Button component={Link} to="/" startIcon={<ArrowBack />}>
                Back to dashboard
            </Button>
            <div className="page-heading">
                <div>
                    <span className="eyebrow">Property intelligence</span>
                    <Typography variant="h4">{property.address}</Typography>
                    <p>
                        {client.fullName} · {label(property.propertyType)} ·
                        Japan
                    </p>
                </div>
                <Status value={report?.status ?? "SAFE"} />
            </div>
            <ReportExport propertyId={property.propertyId} ready={report?.status === "REPORTED"} />
            <div className="metrics">
                <Metric
                    title="Assessed damage"
                    value={money(report?.totalDamage ?? 0)}
                    detail="Latest disaster assessment"
                />
                <Metric
                    title="Calculated coverage"
                    value={money(report?.totalCompensation ?? 0)}
                    detail="Compensation under the active policy"
                />
                <Metric
                    title="Simulated payout"
                    value={money(report?.actualPayout ?? 0)}
                    detail="Amount paid to the customer"
                />
                <Metric
                    title="Investment allocation"
                    value={money(
                        Object.values(investments).reduce((a, b) => a + b, 0),
                    )}
                    detail={`${label(client.riskProfile)} strategy`}
                />
            </div>
            {!report && (
                <Alert severity="info">
                    No disaster report yet. Run a simulation to see the
                    financial breakdown.
                </Alert>
            )}
            <div className="detail-layout">
                <Paper className="section-panel">
                    <Typography variant="h6" sx={{ fontWeight: 700 }}>
                        Impact & recovery
                    </Typography>
                    <p className="muted">
                        Damage, policy compensation and customer payout in EUR
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
                        Policy at a glance
                    </Typography>
                    {policy ? (
                        <dl>
                            <dt>Coverage rate</dt>
                            <dd>{Math.round(policy.coverageRate * 100)}%</dd>
                            <dt>Deductible</dt>
                            <dd>{money(policy.deductible)}</dd>
                            <dt>Policy limit</dt>
                            <dd>{money(policy.policyLimit)}</dd>
                            <dt>Property value</dt>
                            <dd>{money(property.estimatedValue)}</dd>
                        </dl>
                    ) : (
                        <Alert severity="warning">
                            No active policy is available.
                        </Alert>
                    )}
                </Paper>
            </div>
            <Paper className="section-panel">
                <Typography variant="h6" sx={{ fontWeight: 700 }}>
                    Investment breakdown
                </Typography>
                <p className="muted">
                    Allocated amounts, not current market valuations
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
                        Investment allocations will appear after processing
                        completes.
                    </p>
                )}
            </Paper>
        </>
    );
}
export default function App() {
    return (
        <div className="app-shell">
            <aside className="sidebar">
                <Link to="/" className="brand">
                    <span className="brand-mark">C</span>CatFi
                    <span className="brand-dot">.</span>
                </Link>
                <span className="sidebar-caption">CONTROL ROOM</span>
                <nav>
                    <NavLink to="/" end>
                        <DashboardOutlined />
                        Dashboard
                    </NavLink>
                    <NavLink to="/simulation">
                        <BoltOutlined />
                        Simulation lab
                    </NavLink>
                </nav>
                <div className="sidebar-footer">
                    ● Local demo<span>Japan · Synthetic portfolio</span>
                </div>
            </aside>
            <div className="workspace">
                <header className="topbar">
                    <span>Disaster recovery platform</span>
                    <Chip label="DEMO" size="small" variant="outlined" />
                </header>
                <main>
                    <Routes>
                        <Route path="/" element={<Dashboard />} />
                        <Route path="/simulation" element={<Simulation />} />
                        <Route
                            path="/properties/:id"
                            element={<PropertyDetails />}
                        />
                        <Route
                            path="*"
                            element={
                                <Alert severity="info">
                                    Page not found.{" "}
                                    <Link to="/">Return to dashboard</Link>.
                                </Alert>
                            }
                        />
                    </Routes>
                </main>
            </div>
        </div>
    );
}
