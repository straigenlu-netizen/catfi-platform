import { useEffect, useState, type ReactNode } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    Alert, Box, Button, CircularProgress, Paper, Stack, TextField,
    Typography, Divider, Chip,
} from "@mui/material";
import { label, money } from "./api";
import { t, useLanguage } from "./i18n";
import { portalApi, type PortalRole, type PaymentView } from "./portalApi";

function Loading() {
    return <Box role="status" sx={{ py: 3 }}><CircularProgress size={24} /> {t("Loading…")}</Box>;
}
function ErrorNotice() {
    return <Alert severity="error">{t("Unable to load portal data. Please try again.")}</Alert>;
}
function PortalCard({ title, children }: { title: string; children: ReactNode }) {
    return <Paper className="section-panel" sx={{ mb: 2 }}>
        <Typography variant="h6" sx={{ mb: 2 }}>{title}</Typography>
        {children}
    </Paper>;
}
function PaymentHistory({ payments }: { payments: PaymentView[] }) {
    if (!payments.length) return <Typography>{t("No payments yet.")}</Typography>;
    return <Box className="table-scroll">
        <table><thead><tr>
            <th>{t("Property")}</th><th>{t("Amount")}</th><th>{t("Status")}</th><th>{t("Date")}</th>
        </tr></thead><tbody>
        {payments.map(payment => <tr key={payment.paymentId}>
            <td>{payment.propertyId}</td>
            <td>{money(payment.amount)}</td>
            <td><Chip size="small" label={label(payment.status)} /></td>
            <td>{payment.completedAt ? new Intl.DateTimeFormat(
                localeForDate(), { dateStyle: "medium" }).format(new Date(payment.completedAt)) : "—"}</td>
        </tr>)}
        </tbody></table>
    </Box>;
}
function localeForDate(): string {
    // Shared locale store is observed by App and portals.
    return document.documentElement.lang === "ja" ? "ja-JP" : "en-GB";
}

export function PortalLogin() {
    useLanguage();
    const navigate = useNavigate();
    const queries = useQueryClient();
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const mutation = useMutation({
        mutationFn: () => portalApi.login(username, password),
        onSuccess: (session) => {
            queries.setQueryData(["portal-session"], session);
            setPassword("");
            navigate(session.role === "CUSTOMER" ? "/customer" : "/agent");
        },
    });
    return <Box sx={{ maxWidth: 520, mx: "auto", mt: 4 }}>
        <PortalCard title={t("Sign in to CatFi")}>
            <Box component="form" onSubmit={event => { event.preventDefault(); mutation.mutate(); }}>
                <Stack spacing={2}>
                    <TextField label={t("Username")} value={username} onChange={e => setUsername(e.target.value)} required autoComplete="username" />
                    <TextField label={t("Password")} type="password" value={password} onChange={e => setPassword(e.target.value)} required autoComplete="current-password" />
                    {mutation.isError && <Alert severity="error">{t("Invalid login or account unavailable.")}</Alert>}
                    <Button type="submit" variant="contained" disabled={mutation.isPending}>{t("Sign in")}</Button>
                </Stack>
            </Box>
        </PortalCard>
    </Box>;
}

function RoleGuard({ role, children }: { role: PortalRole; children: ReactNode }) {
    const session = useQuery({ queryKey: ["portal-session"], queryFn: portalApi.session, retry: false });
    if (session.isPending) return <Loading />;
    if (session.isError) return <PortalLogin />;
    if (session.data.role !== role) return <Alert severity="warning">
        {t("You do not have access to this portal.")}{" "}
        <Link to={session.data.role === "CUSTOMER" ? "/customer" : "/agent"}>{t("Go to your portal")}</Link>
    </Alert>;
    return <>{children}<PortalSignOut /></>;
}
function PortalSignOut() {
    const queries = useQueryClient();
    const navigate = useNavigate();
    const mutation = useMutation({
        mutationFn: portalApi.logout,
        onSuccess: () => { queries.clear(); navigate("/login"); },
    });
    return <Button sx={{ my: 2 }} onClick={() => mutation.mutate()} disabled={mutation.isPending}>
        {t("Sign out")}
    </Button>;
}

function CustomerHome() {
    useLanguage();
    const queries = useQueryClient();
    const profile = useQuery({ queryKey: ["customer-profile"], queryFn: portalApi.customerProfile });
    const policies = useQuery({ queryKey: ["customer-policies"], queryFn: portalApi.customerPolicies });
    const payments = useQuery({ queryKey: ["customer-payments"], queryFn: portalApi.customerPayments });
    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    useEffect(() => {
        if (profile.data) { setName(profile.data.fullName); setEmail(profile.data.email); }
    }, [profile.data?.fullName, profile.data?.email]);
    const update = useMutation({
        mutationFn: () => portalApi.updateProfile({ fullName: name, email }),
        onSuccess: (updated) => {
            queries.setQueryData(["customer-profile"], updated);
            void queries.invalidateQueries({ queryKey: ["customer-policies"] });
        },
    });
    return <>
        <Typography variant="h4" sx={{ mb: 2 }}>{t("Customer portal")}</Typography>
        <PortalCard title={t("My profile")}>
            {profile.isPending ? <Loading /> : profile.isError ? <ErrorNotice /> : <Box component="form" onSubmit={e => { e.preventDefault(); update.mutate(); }}>
                <Stack spacing={2} sx={{ maxWidth: 440 }}>
                    <TextField label={t("Full name")} value={name} onChange={e => setName(e.target.value)} required slotProps={{ htmlInput: { maxLength: 120 } }} />
                    <TextField label={t("Email")} type="email" value={email} onChange={e => setEmail(e.target.value)} required slotProps={{ htmlInput: { maxLength: 254 } }} />
                    <Typography color="text.secondary">{t("Risk profile")}: {label(profile.data!.riskProfile)} ({t("Read-only")})</Typography>
                    {update.isSuccess && <Alert severity="success">{t("Profile updated.")}</Alert>}
                    {update.isError && <Alert severity="error">{t("Could not save your profile.")}</Alert>}
                    <Button variant="contained" type="submit" disabled={update.isPending}>{t("Save changes")}</Button>
                </Stack>
            </Box>}
        </PortalCard>
        <PortalCard title={t("My policies and claims")}>
            {policies.isPending ? <Loading /> : policies.isError ? <ErrorNotice /> : !policies.data?.length ?
                <Typography>{t("No policies found.")}</Typography> : policies.data.map(item => <Box key={item.property.propertyId} sx={{ mb: 2 }}>
                    <Typography variant="subtitle1" sx={{ fontWeight: "bold" }}>{item.property.address}</Typography>
                    <Typography>{t("Policy status")}: {item.policy?.active ? t("Active") : t("No active policy")}</Typography>
                    <Typography>{t("Policy limit")}: {item.policy ? money(item.policy.policyLimit) : "—"}</Typography>
                    <Typography>{t("Claim status")}: {label(item.report?.status ?? "SAFE")}</Typography>
                    <Typography>{t("Calculated coverage")}: {money(item.report?.totalCompensation ?? 0)}</Typography>
                    <Divider sx={{ my: 2 }} />
                </Box>)}
        </PortalCard>
        <PortalCard title={t("Payment history")}>
            {payments.isPending ? <Loading /> : payments.isError ? <ErrorNotice /> : <PaymentHistory payments={payments.data ?? []} />}
        </PortalCard>
    </>;
}

function AgentHome() {
    useLanguage();
    const clients = useQuery({ queryKey: ["agent-clients"], queryFn: portalApi.agentClients });
    const properties = useQuery({ queryKey: ["agent-properties"], queryFn: portalApi.agentProperties });
    const payments = useQuery({ queryKey: ["agent-payments"], queryFn: portalApi.agentPayments });
    const [selectedClient, setSelectedClient] = useState<string | null>(null);
    const details = useQuery({
        queryKey: ["agent-client", selectedClient],
        queryFn: () => portalApi.agentClient(selectedClient!),
        enabled: selectedClient !== null,
    });
    return <>
        <Typography variant="h4" sx={{ mb: 2 }}>{t("Agent portal")}</Typography>
        <PortalCard title={t("Insurance clients")}>
            {clients.isPending ? <Loading /> : clients.isError ? <ErrorNotice /> : <Box className="table-scroll"><table>
                <thead><tr><th>{t("Full name")}</th><th>{t("Email")}</th><th>{t("Risk profile")}</th><th>{t("Properties")}</th><th /></tr></thead>
                <tbody>{clients.data?.map(client => <tr key={client.clientId}>
                    <td>{client.fullName}</td><td>{client.email}</td><td>{label(client.riskProfile)}</td><td>{client.propertiesCount}</td>
                    <td><Button onClick={() => setSelectedClient(client.clientId)}>{t("View details")}</Button></td>
                </tr>)}</tbody>
            </table></Box>}
            {selectedClient && <Box sx={{ mt: 2 }}>
                <Typography variant="h6">{t("Client details")}</Typography>
                {details.isPending ? <Loading /> : details.isError ? <ErrorNotice /> : <>
                    <Typography>{details.data?.profile.fullName} · {details.data?.profile.email}</Typography>
                    {details.data?.policies.map(item => <Typography key={item.property.propertyId}>
                        <Link to={`/properties/${item.property.propertyId}`}>{item.property.address}</Link>
                        {" · "}{t("Policy status")}: {item.policy?.active ? t("Active") : t("No active policy")}
                        {" · "}{t("Claim status")}: {label(item.report?.status ?? "SAFE")}
                        {" · "}{t("Calculated coverage")}: {money(item.report?.totalCompensation ?? 0)}
                    </Typography>)}
                </>}
            </Box>}
        </PortalCard>
        <PortalCard title={t("All insured properties")}>
            {properties.isPending ? <Loading /> : properties.isError ? <ErrorNotice /> : <Box className="table-scroll"><table>
                <thead><tr><th>{t("Property")}</th><th>{t("Owner & strategy")}</th><th>{t("Status")}</th><th>{t("Coverage")}</th></tr></thead>
                <tbody>{properties.data?.map(row => <tr key={row.propertyId}>
                    <td><Link to={`/properties/${row.propertyId}`}>{row.address}</Link></td>
                    <td>{row.clientName}</td><td>{label(row.status)}</td><td>{money(row.totalCompensation)}</td>
                </tr>)}</tbody>
            </table></Box>}
        </PortalCard>
        <PortalCard title={t("All payments")}>
            {payments.isPending ? <Loading /> : payments.isError ? <ErrorNotice /> : <PaymentHistory payments={payments.data ?? []} />}
        </PortalCard>
        <Button component={Link} to="/simulation" variant="outlined">{t("Simulation lab")}</Button>
    </>;
}

export function CustomerPortal() { return <RoleGuard role="CUSTOMER"><CustomerHome /></RoleGuard>; }
export function AgentPortal() { return <RoleGuard role="AGENT"><AgentHome /></RoleGuard>; }
