import axios from "axios";
import { getLanguage } from "./i18n";
import type { DashboardRow, Details } from "./api";

export type PortalRole = "AGENT" | "CUSTOMER";
export interface Session { username: string; role: PortalRole }
export interface Profile { clientId: string; fullName: string; email: string; riskProfile: string }
export interface PaymentView {
    paymentId: string;
    propertyId: string;
    compensationResultId: string;
    amount: number;
    status: string;
    initiatedAt: string;
    completedAt: string | null;
}
export interface AgentClient {
    clientId: string;
    fullName: string;
    email: string;
    riskProfile: string;
    propertiesCount: number;
}
export interface AgentClientDetails { profile: Profile; policies: Details[] }

const http = axios.create({ baseURL: "/api", timeout: 15000 });
http.interceptors.request.use((config) => {
    config.headers.set("Accept-Language", getLanguage());
    return config;
});

// The login request also requires a CSRF token; never disable Spring CSRF.
async function csrfHeaders() {
    const response = await http.get<{ token: string }>("/auth/csrf");
    return { "X-XSRF-TOKEN": response.data.token };
}

export const portalApi = {
    session: async (): Promise<Session> => (await http.get<Session>("/auth/me")).data,
    login: async (username: string, password: string): Promise<Session> => {
        const headers = await csrfHeaders();
        await http.post("/auth/login", new URLSearchParams({ username, password }), {
            headers: { ...headers, "Content-Type": "application/x-www-form-urlencoded" },
        });
        return (await http.get<Session>("/auth/me")).data;
    },
    logout: async (): Promise<void> => {
        const headers = await csrfHeaders();
        await http.post("/auth/logout", undefined, { headers });
    },
    customerProfile: async (): Promise<Profile> => (await http.get("/customer/profile")).data,
    updateProfile: async (details: { fullName: string; email: string }): Promise<Profile> =>
        (await http.put("/customer/profile", details, { headers: await csrfHeaders() })).data,
    customerPolicies: async (): Promise<Details[]> => (await http.get("/customer/policies")).data,
    customerPolicy: async (id: string): Promise<Details> =>
        (await http.get(`/customer/policies/${encodeURIComponent(id)}`)).data,
    customerPayments: async (): Promise<PaymentView[]> => (await http.get("/customer/payments")).data,
    agentClients: async (): Promise<AgentClient[]> => (await http.get("/agent/clients")).data,
    agentClient: async (id: string): Promise<AgentClientDetails> =>
        (await http.get(`/agent/clients/${encodeURIComponent(id)}`)).data,
    agentProperties: async (): Promise<DashboardRow[]> => (await http.get("/agent/properties")).data,
    agentPayments: async (): Promise<PaymentView[]> => (await http.get("/agent/payments")).data,
};
