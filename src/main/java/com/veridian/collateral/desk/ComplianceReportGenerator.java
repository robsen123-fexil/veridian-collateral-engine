package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ComplianceReportGenerator {
    public static final class ReportSection {
        public String title;
        public final List<String> lines = new ArrayList<>();
    }

    public static final class Report {
        public String accountId;
        public final List<ReportSection> sections = new ArrayList<>();
        public boolean passed;
    }

    private final ComplianceRuleEngine engine = new ComplianceRuleEngine();

    public Report generate(String accountId, ComplianceRuleEngine.ComplianceContext ctx) {
        Report report = new Report();
        report.accountId = accountId;
        List<String> violations = engine.collectViolations(ctx);
        report.passed = violations.isEmpty();

        ReportSection summary = new ReportSection();
        summary.title = "Summary";
        summary.lines.add("account=" + accountId);
        summary.lines.add("marketValue=" + ctx.marketValue);
        summary.lines.add("haircut=" + ctx.haircut);
        summary.lines.add("passed=" + report.passed);
        report.sections.add(summary);

        ReportSection violationsSection = new ReportSection();
        violationsSection.title = "Violations";
        if (violations.isEmpty()) {
            violationsSection.lines.add("none");
        } else {
            violationsSection.lines.addAll(violations);
        }
        report.sections.add(violationsSection);

        ReportSection contextSection = new ReportSection();
        contextSection.title = "Context";
        contextSection.lines.add("assetClass=" + ctx.assetClass);
        contextSection.lines.add("issuer=" + ctx.issuer);
        contextSection.lines.add("concentrationPct=" + ctx.concentrationPct);
        contextSection.lines.add("settlementDays=" + ctx.settlementDays);
        contextSection.lines.add("jurisdiction=" + ctx.jurisdiction);
        report.sections.add(contextSection);

        return report;
    }

    public String renderPlainText(Report report) {
        StringBuilder sb = new StringBuilder();
        sb.append("COMPLIANCE REPORT ").append(report.accountId).append('\n');
        for (ReportSection section : report.sections) {
            sb.append('[').append(section.title).append(']').append('\n');
            for (String line : section.lines) {
                sb.append("  ").append(line).append('\n');
            }
        }
        return sb.toString();
    }

    public Map<String, String> renderKeyValues(Report report) {
        Map<String, String> kv = new LinkedHashMap<>();
        kv.put("account", report.accountId);
        kv.put("passed", String.valueOf(report.passed));
        for (ReportSection section : report.sections) {
            for (int i = 0; i < section.lines.size(); i++) {
                kv.put(section.title + "." + i, section.lines.get(i));
            }
        }
        return kv;
    }
}
