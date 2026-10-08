import { describe, expect, it, afterEach } from "vitest";
import { formatMoney, formatStatus, getLanguage, initializeLanguage, setLanguage, t } from "./i18n";

afterEach(() => { localStorage.clear(); setLanguage("en"); });
describe("English and Japanese localization", () => {
    it("defaults to English and falls back for unknown labels", () => {
        initializeLanguage();
        expect(getLanguage()).toBe("en");
        expect(t("Your control room")).toBe("Your control room");
        expect(t("Untranslated future field")).toBe("Untranslated future field");
    });
    it("persists Japanese preference and translates status without changing its API value", () => {
        setLanguage("ja");
        expect(localStorage.getItem("catfi.language")).toBe("ja");
        expect(document.documentElement.lang).toBe("ja");
        expect(t("Your control room")).toBe("管理ダッシュボード");
        expect(formatStatus("REPORTED")).toBe("報告完了");
        initializeLanguage();
        expect(getLanguage()).toBe("ja");
    });
    it("keeps the EUR denomination in both locales", () => {
        setLanguage("en");
        const english = formatMoney(2500);
        setLanguage("ja");
        const japanese = formatMoney(2500);
        expect(english).toContain("€");
        expect(japanese).toContain("€");
        expect(typeof english).toBe("string");
        expect(typeof japanese).toBe("string");
    });
    it("ignores invalid saved locales", () => {
        localStorage.setItem("catfi.language", "fr");
        initializeLanguage();
        expect(getLanguage()).toBe("en");
    });
});
