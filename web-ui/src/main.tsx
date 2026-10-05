import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { CssBaseline, ThemeProvider, createTheme } from "@mui/material";
import App from "./App";
import "./styles.css";
const theme = createTheme({
    palette: {
        primary: { main: "#245e4d" },
        background: { default: "#f5f6f2", paper: "#fff" },
    },
    typography: {
        fontFamily: "Inter, Segoe UI, system-ui, sans-serif",
        h4: { fontWeight: 700, letterSpacing: "-1px" },
        button: { textTransform: "none" },
    },
    shape: { borderRadius: 12 },
    components: { MuiPaper: { defaultProps: { elevation: 0 } } },
});
const client = new QueryClient({ defaultOptions: { queries: { retry: 1 } } });
ReactDOM.createRoot(document.getElementById("root")!).render(
    <React.StrictMode>
        <ThemeProvider theme={theme}>
            <CssBaseline />
            <QueryClientProvider client={client}>
                <BrowserRouter>
                    <App />
                </BrowserRouter>
            </QueryClientProvider>
        </ThemeProvider>
    </React.StrictMode>,
);
