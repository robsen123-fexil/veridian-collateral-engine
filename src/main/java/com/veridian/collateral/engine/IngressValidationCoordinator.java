package com.veridian.collateral.engine;

import com.veridian.collateral.desk.CollateralReportBuilder;
import com.veridian.collateral.desk.HaircutGrid;
import com.veridian.collateral.desk.MarginAggregator;
import com.veridian.collateral.ingress.BatchIngressValidator;
import com.veridian.collateral.ingress.EnvelopeIngressValidator;
import com.veridian.collateral.ingress.SessionIngressValidator;

public final class IngressValidationCoordinator {
    private final BatchIngressValidator batchValidator = new BatchIngressValidator();
    private final EnvelopeIngressValidator envelopeValidator = new EnvelopeIngressValidator();
    private final SessionIngressValidator sessionValidator = new SessionIngressValidator();

    public int validateAndScore(byte[] input) {
        if (input == null || input.length < 4) {
            return 0;
        }
        int magic = (input[0] & 0xFF) | ((input[1] & 0xFF) << 8) | ((input[2] & 0xFF) << 16) | ((input[3] & 0xFF) << 24);
        if ((magic & 0xFFFFFF) == 0x314243) {
            BatchIngressValidator.Outcome o = batchValidator.validate(input);
            return o.valid ? 100 + o.recordCount : 0;
        }
        if ((magic & 0xFFFFFF) == 0x314543) {
            EnvelopeIngressValidator.ValidationOutcome o = envelopeValidator.validate(input);
            return o.accepted ? 200 + o.channelId : 0;
        }
        if ((magic & 0xFFFFFF) == 0x314353) {
            SessionIngressValidator.Outcome o = sessionValidator.validate(input);
            return o.valid ? 300 + o.legCount : 0;
        }
        return 1;
    }

    public CollateralReportBuilder.CollateralReport buildSampleReport(String account, double pledged, double required) {
        CollateralReportBuilder builder = new CollateralReportBuilder();
        CollateralReportBuilder.CollateralReport report = builder.newReport(account, "2026-07-29");
        builder.addSummary(report, pledged, required, pledged - required);
        builder.addHaircutSection(report, new HaircutGrid(), "GOVT", "AAA", pledged, false);
        MarginAggregator.MarginResult margin = new MarginAggregator.MarginResult();
        margin.initialMargin = required * 0.8;
        margin.variationMargin = required * 0.2;
        margin.totalRequirement = required;
        margin.excess = pledged - required;
        builder.addMarginSection(report, margin);
        return report;
    }
}
