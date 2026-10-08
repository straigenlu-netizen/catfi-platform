import axios from "axios";
export type DisasterType = "EARTHQUAKE" | "FLOOD" | "TYPHOON" | "TSUNAMI";
export interface Policy {
    policyId: string;
    coverageRate: number;
    deductible: number;
    policyLimit: number;
    active: boolean;
}
export interface DashboardRow {
    propertyId: string;
    address: string;
    clientName: string;
    riskProfile: string;
    status: string;
    totalDamage: number;
    totalCompensation: number;
    actualPayout: number;
    policy: Policy | null;
}
export interface Report {
    totalDamage: number;
    totalCompensation: number;
    actualPayout: number;
    currentPortfolio: Record<string, number>;
    status: string;
    reportGeneratedAt: string;
}
export interface Details {
    property: {
        propertyId: string;
        address: string;
        propertyType: string;
        latitude: number;
        longitude: number;
        estimatedValue: number;
    };
    policy: Policy | null;
    client: { fullName: string; riskProfile: string };
    report: Report | null;
}
export interface Trigger {
    type: DisasterType;
    intensity: number;
    latitude: number;
    longitude: number;
}
const client = axios.create({ baseURL: "/api/ui", timeout: 15000 });
let csrfToken: string | undefined;
const csrfClient = axios.create({ baseURL: "/api", timeout: 15000 });
export async function prepareCsrf(): Promise<void> {
    const response = await csrfClient.get<{ token: string }>("/auth/csrf");
    csrfToken = response.data.token;
}
client.interceptors.request.use((config) => {
    const method = config.method?.toUpperCase() ?? "GET";
    if (!["GET", "HEAD", "OPTIONS", "TRACE"].includes(method) && csrfToken) {
        config.headers.set("X-XSRF-TOKEN", csrfToken);
    }
    return config;
});
export const api = {
    dashboard: async () =>
        (await client.get<DashboardRow[]>("/dashboard")).data,
    details: async (id: string) =>
        (await client.get<Details>(`/properties/${encodeURIComponent(id)}`))
            .data,
    trigger: async (request: Trigger) =>
        (await client.post<{ eventId: string }>("/trigger-disaster", request))
            .data,
};
export const money = (value: number) =>
    new Intl.NumberFormat("en-IE", {
        style: "currency",
        currency: "EUR",
    }).format(value);
export const label = (value: string) =>
    value.toLowerCase().replaceAll("_", " ");
