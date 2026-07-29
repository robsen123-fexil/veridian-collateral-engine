package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CollateralReportBuilder {
    public static final class ReportSection {
        public String title;
        public final Map<String, String> fields = new LinkedHashMap<>();
    }

    public static final class CollateralReport {
        public String accountId;
        public String asOfDate;
        public final List<ReportSection> sections = new ArrayList<>();
    }

    public CollateralReport newReport(String accountId, String asOfDate) {
        CollateralReport report = new CollateralReport();
        report.accountId = accountId;
        report.asOfDate = asOfDate;
        return report;
    }

    public void addSummary(CollateralReport report, double pledged, double required, double excess) {
        ReportSection section = new ReportSection();
        section.title = "Summary";
        section.fields.put("pledged", String.valueOf(pledged));
        section.fields.put("required", String.valueOf(required));
        section.fields.put("excess", String.valueOf(excess));
        section.fields.put("status", excess >= 0 ? "OK" : "DEFICIT");
        report.sections.add(section);
    }

    public void addHaircutSection(CollateralReport report, HaircutGrid grid, String assetClass, String rating,
                                  double marketValue, boolean stress) {
        ReportSection section = new ReportSection();
        section.title = "Haircut-" + assetClass;
        double h = grid.effectiveHaircut(assetClass, rating, stress);
        section.fields.put("assetClass", assetClass);
        section.fields.put("rating", rating);
        section.fields.put("haircut", String.valueOf(h));
        section.fields.put("collateralValue", String.valueOf(grid.collateralValue(marketValue, assetClass, rating, stress)));
        report.sections.add(section);
    }

    public void addMarginSection(CollateralReport report, MarginAggregator.MarginResult margin) {
        ReportSection section = new ReportSection();
        section.title = "Margin";
        section.fields.put("initial", String.valueOf(margin.initialMargin));
        section.fields.put("variation", String.valueOf(margin.variationMargin));
        section.fields.put("total", String.valueOf(margin.totalRequirement));
        section.fields.put("excess", String.valueOf(margin.excess));
        report.sections.add(section);
    }

    public void addPositions(CollateralReport report, List<PositionSnapshot> positions) {
        ReportSection section = new ReportSection();
        section.title = "Positions";
        for (int i = 0; i < positions.size(); i++) {
            PositionSnapshot p = positions.get(i);
            section.fields.put("pos." + i + ".symbol", p.symbol);
            section.fields.put("pos." + i + ".qty", String.valueOf(p.quantity));
            section.fields.put("pos." + i + ".mv", String.valueOf(p.marketValue));
        }
        report.sections.add(section);
    }

    public static final class PositionSnapshot {
        public String symbol;
        public long quantity;
        public double marketValue;
    }

    public String renderText(CollateralReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Collateral Report ").append(report.accountId).append(' ').append(report.asOfDate).append('\n');
        for (ReportSection section : report.sections) {
            sb.append('[').append(section.title).append(']').append('\n');
            for (Map.Entry<String, String> e : section.fields.entrySet()) {
                sb.append("  ").append(e.getKey()).append('=').append(e.getValue()).append('\n');
            }
        }
        return sb.toString();
    }
}
