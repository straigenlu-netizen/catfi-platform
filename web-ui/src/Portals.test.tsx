import { render, screen, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, expect, it, vi } from "vitest";
import { AgentPortal, CustomerPortal } from "./Portals";
import { portalApi } from "./portalApi";
import { setLanguage } from "./i18n";

vi.mock("./portalApi", () => ({
    portalApi: {
        session: vi.fn(), login: vi.fn(), logout: vi.fn(),
        customerProfile: vi.fn(), updateProfile: vi.fn(), customerPolicies: vi.fn(),
        customerPayments: vi.fn(), agentClients: vi.fn(), agentClient: vi.fn(),
        agentProperties: vi.fn(), agentPayments: vi.fn(),
    },
}));

function mount(page: ReactNode) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(<QueryClientProvider client={queryClient}>
        <MemoryRouter>{page}</MemoryRouter>
    </QueryClientProvider>);
}

beforeEach(() => {
    vi.resetAllMocks();
    setLanguage("en");
});

it("blocks customers from the agent portal before fetching protected content", async () => {
    vi.mocked(portalApi.session).mockResolvedValue({ username: "demo1@example.com", role: "CUSTOMER" });
    mount(<AgentPortal />);
    expect(await screen.findByText("You do not have access to this portal.")).toBeInTheDocument();
    expect(portalApi.agentClients).not.toHaveBeenCalled();
});

it("shows a customer's policies and payments without exposing other clients", async () => {
    vi.mocked(portalApi.session).mockResolvedValue({ username: "demo1@example.com", role: "CUSTOMER" });
    vi.mocked(portalApi.customerProfile).mockResolvedValue({
        clientId: "client1", fullName: "Demo Client", email: "demo1@example.com", riskProfile: "MODERATE",
    });
    vi.mocked(portalApi.customerPolicies).mockResolvedValue([]);
    vi.mocked(portalApi.customerPayments).mockResolvedValue([]);
    mount(<CustomerPortal />);
    expect(await screen.findByText("My policies and claims")).toBeInTheDocument();
    expect(await screen.findByText("No payments yet.")).toBeInTheDocument();
    expect(portalApi.agentClients).not.toHaveBeenCalled();
});

it("submits only editable profile fields", async () => {
    vi.mocked(portalApi.session).mockResolvedValue({ username: "demo1@example.com", role: "CUSTOMER" });
    const profile = { clientId: "client1", fullName: "Demo Client", email: "demo1@example.com", riskProfile: "MODERATE" };
    vi.mocked(portalApi.customerProfile).mockResolvedValue(profile);
    vi.mocked(portalApi.customerPolicies).mockResolvedValue([]);
    vi.mocked(portalApi.customerPayments).mockResolvedValue([]);
    vi.mocked(portalApi.updateProfile).mockResolvedValue({ ...profile, fullName: "New Name" });
    mount(<CustomerPortal />);
    await userEvent.clear(await screen.findByRole("textbox", { name: "Full name" }));
    await userEvent.type(screen.getByRole("textbox", { name: "Full name" }), "New Name");
    await userEvent.click(screen.getByRole("button", { name: "Save changes" }));
    await waitFor(() => expect(portalApi.updateProfile).toHaveBeenCalledWith({
        fullName: "New Name", email: "demo1@example.com",
    }));
});
