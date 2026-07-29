package com.veridian.collateral.engine;

import com.veridian.collateral.desk.ComplianceRuleEngine;
import com.veridian.collateral.desk.HaircutGrid;
import com.veridian.collateral.desk.MarginAggregator;
import com.veridian.collateral.desk.PledgeEnvelopeLedger;
import com.veridian.collateral.ledger.AuditSpool;
import com.veridian.collateral.ledger.PositionLedger;
import com.veridian.collateral.ledger.VaultCheckpointStore;

import java.util.ArrayList;
import java.util.List;

public final class CollateralOrchestrator {
    public static final class RunSummary {
        public int pipelineDigest;
        public boolean complianceOk;
        public double totalPledged;
        public double marginRequirement;
        public double marginExcess;
        public int checkpointDigest;
        public final List<String> stages = new ArrayList<>();
    }

    private final CollateralPipeline pipeline = new CollateralPipeline();
    private final PipelineStageRegistry stageRegistry = new PipelineStageRegistry();
    private final ComplianceRuleEngine compliance = new ComplianceRuleEngine();
    private final HaircutGrid haircuts = new HaircutGrid();
    private final MarginAggregator margin = new MarginAggregator();
    private final PledgeEnvelopeLedger pledges = new PledgeEnvelopeLedger();
    private final PositionLedger positions = new PositionLedger();
    private final VaultCheckpointStore checkpoints = new VaultCheckpointStore();
    private final AuditSpool audit = new AuditSpool();

    public RunSummary runEndToEnd(byte[] wireInput, String accountId, List<MarginAggregator.MarginLeg> legs, double collateralValue) {
        RunSummary summary = new RunSummary();
        summary.pipelineDigest = pipeline.runCollateralPipeline(wireInput);
        summary.stages.add("pipeline:" + summary.pipelineDigest);

        PipelineStageRegistry.PipelineContext ctx = new PipelineStageRegistry.PipelineContext();
        ctx.input = wireInput;
        int stageDigest = stageRegistry.run(ctx);
        summary.stages.add("stages:" + stageDigest);

        ComplianceRuleEngine.ComplianceContext cctx = new ComplianceRuleEngine.ComplianceContext();
        cctx.accountId = accountId;
        cctx.marketValue = collateralValue;
        cctx.assetClass = "CORP";
        cctx.rating = "IG";
        cctx.haircut = haircuts.effectiveHaircut("CORP", "IG", false);
        summary.complianceOk = compliance.runAll(cctx);
        summary.stages.add("compliance:" + summary.complianceOk);

        PledgeEnvelopeLedger.Envelope env = pledges.open(accountId);
        pledges.pledge(env.id, collateralValue, List.of("CUSIP-SEED"));
        summary.totalPledged = pledges.totalPledged();
        summary.stages.add("pledge:" + summary.totalPledged);

        MarginAggregator.MarginResult mres = margin.aggregate(legs, collateralValue, false);
        summary.marginRequirement = mres.totalRequirement;
        summary.marginExcess = mres.excess;
        summary.stages.add("margin:" + summary.marginRequirement);

        positions.applyFill(accountId, "BOND-UST", 1000, 99.5);
        VaultCheckpointStore.Checkpoint cp = checkpoints.create("eod", List.of(env.id));
        summary.checkpointDigest = cp.digest;
        summary.stages.add("checkpoint:" + summary.checkpointDigest);

        audit.append("orchestrator", "complete", String.valueOf(summary.pipelineDigest));
        return summary;
    }

    public CollateralPipeline pipeline() { return pipeline; }
    public PositionLedger positions() { return positions; }
    public AuditSpool audit() { return audit; }
}
