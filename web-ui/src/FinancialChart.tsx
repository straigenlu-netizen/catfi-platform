import { useEffect, useRef } from "react";
import * as echarts from "echarts/core";
import { BarChart } from "echarts/charts";
import { GridComponent, TooltipComponent } from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import { money } from "./api";
import { t, useLanguage, getLanguage } from "./i18n";
echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer]);
export default function FinancialChart({
    damage,
    compensation,
    payout,
}: {
    damage: number;
    compensation: number;
    payout: number;
}) {
    const language = useLanguage();
    const element = useRef<HTMLDivElement>(null);
    useEffect(() => {
        if (!element.current) return;
        const chart = echarts.init(element.current);
        chart.setOption({
            grid: { left: 80, right: 25, top: 25, bottom: 35 },
            tooltip: {
                trigger: "axis",
                valueFormatter: (value: number) => money(value),
            },
            xAxis: {
                type: "value",
                axisLabel: {
                    formatter: (value: number) =>
                        `€${value.toLocaleString(getLanguage() === "ja" ? "ja-JP" : "en")}`,
                },
                splitLine: { lineStyle: { color: "#edf0ea" } },
            },
            yAxis: {
                type: "category",
                data: [t("Payout"), t("Coverage"), t("Damage")],
                axisTick: { show: false },
                axisLine: { show: false },
            },
            series: [
                {
                    type: "bar",
                    barWidth: 30,
                    data: [
                        { value: payout, itemStyle: { color: "#245e4d" } },
                        {
                            value: compensation,
                            itemStyle: { color: "#8fae9b" },
                        },
                        { value: damage, itemStyle: { color: "#e9b66a" } },
                    ],
                },
            ],
        });
        const observer = new ResizeObserver(() => chart.resize());
        observer.observe(element.current);
        return () => {
            observer.disconnect();
            chart.dispose();
        };
    }, [damage, compensation, payout, language]);
    return (
        <div
            ref={element}
            className="financial-chart"
            role="img"
            aria-label={language === "ja"
                ? `${t("Damage")} ${money(damage)}, ${t("Coverage")} ${money(compensation)}, ${t("Payout")} ${money(payout)}`
                : `Damage ${money(damage)}, compensation ${money(compensation)}, payout ${money(payout)}`}
        />
    );
}
