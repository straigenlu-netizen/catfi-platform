import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { vi } from "vitest";
import { Dashboard, Simulation } from "./App";
import { api, prepareCsrf } from "./api";
vi.mock("./api", async (importOriginal) => ({
    ...(await importOriginal<typeof import("./api")>()),
    api: { dashboard: vi.fn(), details: vi.fn(), trigger: vi.fn() },
    prepareCsrf: vi.fn().mockResolvedValue(undefined),
}));
vi.mock("./FinancialChart", () => ({
    default: () => <div data-testid="financial-chart" />,
}));
function mount(component: React.ReactNode) {
    const client = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    });
    return render(
        <QueryClientProvider client={client}>
            <MemoryRouter>{component}</MemoryRouter>
        </QueryClientProvider>,
    );
}
beforeEach(() => vi.resetAllMocks());
it("shows properties and their reporting status", async () => {
    vi.mocked(api.dashboard).mockResolvedValue([
        {
            propertyId: "p1",
            address: "Chiyoda, Tokyo",
            clientName: "Demo Client",
            riskProfile: "MODERATE",
            status: "REPORTED",
            totalDamage: 20000,
            totalCompensation: 13000,
            actualPayout: 6500,
            policy: null,
        },
    ]);
    mount(<Dashboard />);
    expect(await screen.findByText("Chiyoda, Tokyo")).toHaveAttribute(
        "href",
        "/properties/p1",
    );
    expect(screen.getByText("reported")).toBeInTheDocument();
});
it("shows an actionable backend error", async () => {
    vi.mocked(api.dashboard).mockRejectedValue(new Error("Unavailable"));
    mount(<Dashboard />);
    expect(await screen.findByRole("alert")).toHaveTextContent(
        "Could not reach",
    );
});
it("submits numeric disaster parameters and confirms publication", async () => {
    vi.mocked(api.trigger).mockResolvedValue({ eventId: "event-123" });
    mount(<Simulation />);
    await userEvent.click(
        screen.getByRole("button", { name: "Run simulation" }),
    );
    await waitFor(() =>
        expect(api.trigger).toHaveBeenCalledWith(
            {
                type: "EARTHQUAKE",
                intensity: 7,
                latitude: 35.6762,
                longitude: 139.6503,
            },
        ),
    );
    expect(await screen.findByText(/Event ID: event-123/)).toBeInTheDocument();
    expect(prepareCsrf).toHaveBeenCalled();
});
it("reports Kafka publication failures", async () => {
    vi.mocked(api.trigger).mockRejectedValue(new Error("Unavailable"));
    mount(<Simulation />);
    await userEvent.click(
        screen.getByRole("button", { name: "Run simulation" }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
        "could not be published",
    );
});
