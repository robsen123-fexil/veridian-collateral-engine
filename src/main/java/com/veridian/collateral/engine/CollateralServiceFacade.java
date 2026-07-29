package com.veridian.collateral.engine;

import com.veridian.collateral.desk.CollateralReportBuilder;
import com.veridian.collateral.desk.CollateralWorkflowRegistry;
import com.veridian.collateral.ledger.CustodyAuditTrail;

public final class CollateralServiceFacade {
    private final CollateralOrchestrator orchestrator = new CollateralOrchestrator();
    private final CollateralWorkflowRegistry workflows = new CollateralWorkflowRegistry();
    private final CustodyAuditTrail audit = new CustodyAuditTrail();
    private final CollateralReportBuilder reports = new CollateralReportBuilder();

    public int processWireBatch(byte[] input, String accountId) {
        audit.record("facade", "wire_batch", accountId, String.valueOf(input == null ? 0 : input.length));
        return orchestrator.pipeline().runCollateralPipeline(input);
    }

    public CollateralWorkflowRegistry.CollateralWorkflowContext startPledgeWorkflow(String accountId, double amount) {
        CollateralWorkflowRegistry.CollateralWorkflowContext ctx = new CollateralWorkflowRegistry.CollateralWorkflowContext();
        ctx.accountId = accountId;
        ctx.type = CollateralWorkflowRegistry.WorkflowType.PLEDGE;
        ctx.amount = amount;
        workflows.start(ctx);
        audit.record("facade", "pledge_start", accountId, String.valueOf(amount));
        return ctx;
    }

    public String renderCoverageReport(String accountId, double pledged, double required) {
        CollateralReportBuilder.CollateralReport report = reports.newReport(accountId, "2026-07-29");
        reports.addSummary(report, pledged, required, pledged - required);
        return reports.renderText(report);
    }
}
