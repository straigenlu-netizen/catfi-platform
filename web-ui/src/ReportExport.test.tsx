import { render, screen, fireEvent} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ReportExport } from "./ReportExport";
const config = { apiUrl: "https://exports.example", region: "eu-central-1", clientId: "client" };
afterEach(() => vi.unstubAllGlobals());
describe("Report export", () => {
    it("hides unconfigured exports", () => {
        const { container } = render(<ReportExport propertyId="p" ready config={{ apiUrl: "", region: "", clientId: "" }} />);
        expect(container).toBeEmptyDOMElement();
    });
    it("signs in, clears password and downloads a finished job", async () => {
        const fetchMock = vi.fn()
            .mockResolvedValueOnce({ ok: true, json: async () => ({ AuthenticationResult: { IdToken: "jwt" } }) })
            .mockResolvedValueOnce({ ok: true, json: async () => ({ jobId: "job", status: "PENDING" }) })
            .mockResolvedValueOnce({ ok: true, json: async () => ({ status: "SUCCEEDED" }) })
            .mockResolvedValueOnce({ ok: true, json: async () => ({ url: "https://private.s3.amazonaws.com/report" }) });
        vi.stubGlobal("fetch", fetchMock);
        render(<ReportExport propertyId="p" ready config={config} />);
        fireEvent.change(screen.getByLabelText(/Email/), { target: { value: "user@example.com" } });
        fireEvent.change(screen.getByLabelText(/Password/), { target: { value: "password" } });
        fireEvent.click(screen.getByRole("button", { name: "Sign in for exports" }));
        fireEvent.click(await screen.findByRole("button", { name: "Export JSON report" }));
        const download = await screen.findByRole("link", { name: /Download JSON/ });
        expect(download).toHaveAttribute("href", "https://private.s3.amazonaws.com/report");
        expect(fetchMock.mock.calls[1][1].headers.Authorization).toBe("Bearer jwt");
        fireEvent.click(screen.getByRole("button", { name: "Sign out" }));
        expect(screen.getByLabelText(/Password/)).toHaveValue("");
    });
    it("shows an object authorization failure without a download", async () => {
        vi.stubGlobal("fetch", vi.fn()
            .mockResolvedValueOnce({ ok: true, json: async () => ({ AuthenticationResult: { IdToken: "jwt" } }) })
            .mockResolvedValueOnce({ ok: false, status: 403 }));
        render(<ReportExport propertyId="p" ready config={config} />);
        fireEvent.change(screen.getByLabelText(/Email/), { target: { value: "user@example.com" } });
        fireEvent.change(screen.getByLabelText(/Password/), { target: { value: "password" } });
        fireEvent.click(screen.getByRole("button", { name: "Sign in for exports" }));
        fireEvent.click(await screen.findByRole("button", { name: "Export JSON report" }));
        expect(await screen.findByRole("alert")).toHaveTextContent("permission");
        expect(screen.queryByRole("link", { name: /Download JSON/ })).not.toBeInTheDocument();
    });
});
