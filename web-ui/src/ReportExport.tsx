import { useEffect, useState, useRef } from "react";
import { Alert, Box, Button, TextField, Typography } from "@mui/material";

export interface ExportConfiguration { apiUrl: string; region: string; clientId: string }
const configured: ExportConfiguration = {
    apiUrl: import.meta.env.VITE_EXPORT_API_URL ?? "",
    region: import.meta.env.VITE_COGNITO_REGION ?? "",
    clientId: import.meta.env.VITE_COGNITO_CLIENT_ID ?? "",
};

export function ReportExport({ propertyId, ready, config = configured }: {
    propertyId: string; ready: boolean; config?: ExportConfiguration;
}) {
    const requestKey = useRef("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [token, setToken] = useState("");
    const [job, setJob] = useState("");
    const [status, setStatus] = useState("");
    const [url, setUrl] = useState("");
    const [error, setError] = useState("");
    const [busy, setBusy] = useState(false);
    useEffect(() => { requestKey.current = ""; setJob(""); setUrl(""); setStatus(""); setError(""); }, [propertyId]);
    useEffect(() => {
        if (!job || !token) return;
        const controller = new AbortController();
        let timer: ReturnType<typeof setTimeout>;
        let attempts = 0;
        const poll = async () => {
            try {
                const response = await fetch(`${config.apiUrl}/exports/${job}`, {
                    headers: { Authorization: `Bearer ${token}` }, signal: controller.signal,
                });
                if (response.status === 401) {
                    setToken("");
                    setError("Please sign in again.");
                    return;
                }
                if (!response.ok) {
                    setError("Cannot read export status.");
                    return;
                }
                const data = await response.json(); setStatus(data.status);
                if (data.status === "FAILED") {
                    setError("Export failed. Please contact support.");
                    return;
                }
                if (data.status === "SUCCEEDED") {
                    const link = await fetch(`${config.apiUrl}/exports/${job}/download`, {
                        headers: { Authorization: `Bearer ${token}` }, signal: controller.signal,
                    });
                    if (!link.ok) {
                        setError("Cannot download this export.");
                        return;
                    }
                    const body = await link.json();
                    if (new URL(body.url).protocol !== "https:"){
                        setError("Invalid download link.");
                        return;
                    }
                    setUrl(body.url);
                    return;
                }
                if (++attempts >= 90) {
                    setError("Export is still processing. Try checking again later.");
                    timer = setTimeout(() => void poll(), 2000);
                }
            } catch (e) {
                if (!controller.signal.aborted) setError(e instanceof Error ? e.message : "Export unavailable.");
            }
        };
        void poll();
        return () => { controller.abort(); clearTimeout(timer); };
    }, [job, token, config.apiUrl]);
    if (!config.apiUrl || !config.region || !config.clientId) return null;
    const signIn = async () => {
        setBusy(true); setError("");
        try {
            const response = await fetch(`https://cognito-idp.${config.region}.amazonaws.com/`, {
                method: "POST", headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AWSCognitoIdentityProviderService.InitiateAuth" },
                body: JSON.stringify({ AuthFlow: "USER_PASSWORD_AUTH", ClientId: config.clientId, AuthParameters: { USERNAME: email, PASSWORD: password } }),
            });
            const body = await response.json();
            if (!response.ok || !body.AuthenticationResult?.IdToken) {
                setError("Sign-in failed. Check your credentials and account setup.");
                return;
            }            setToken(body.AuthenticationResult.IdToken);
        } catch { setError("Sign-in failed. Check your credentials and account setup."); }
        finally { setPassword(""); setBusy(false); }
    };
    const start = async () => {
        setBusy(true); setError(""); setUrl("");
        try {
            if (!requestKey.current) requestKey.current = crypto.randomUUID();
            const response = await fetch(`${config.apiUrl}/reports/${propertyId}/exports`, {
                method: "POST", headers: { Authorization: `Bearer ${token}`, "Idempotency-Key": requestKey.current },
            });
            if (response.status === 401) {
                setToken("");
                setError("Please sign in again.");
                return;
            }

            if (response.status === 403) {
                setError("You do not have permission to export this property.");
                return;
            }

            if (!response.ok) {
                setError("Cannot request this export.");
                return;
            }
            const body = await response.json(); requestKey.current = ""; setJob(body.jobId); setStatus(body.status);
        } catch (e) { setError(e instanceof Error ? e.message : "Export unavailable."); }
        finally { setBusy(false); }
    };
    return <Box component="section" aria-label="Report export" sx={{ my: 3 }}>
        <Typography variant="h6">Download report</Typography>
        {error && <Alert severity="error">{error}</Alert>}
        {!token ? <Box component="form" onSubmit={(event) => { event.preventDefault(); void signIn(); }} sx={{ display: "flex", gap: 1, flexWrap: "wrap", my: 1 }}>
            <TextField label="Email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="username" />
            <TextField label="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete="current-password" />
            <Button type="submit" disabled={busy}>Sign in for exports</Button>
        </Box> : <Box sx={{ display: "flex", gap: 1, my: 1 }}>
            <Button variant="outlined" disabled={!ready || busy || (!!job && status !== "FAILED" && status !== "SUCCEEDED")} onClick={() => void start()}>Export JSON report</Button>
            <Button onClick={() => { setToken(""); setJob(""); setUrl(""); setStatus(""); }}>Sign out</Button>
        </Box>}
        {!ready && <Typography>Export becomes available when the report is complete.</Typography>}
        {status && <Typography role="status">Export: {status.toLowerCase()}</Typography>}
        {url && <Button component="a" href={url} target="_blank" rel="noopener noreferrer">Download JSON (link valid for 5 minutes)</Button>}
    </Box>;
}
