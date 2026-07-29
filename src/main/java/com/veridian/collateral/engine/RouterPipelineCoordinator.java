package com.veridian.collateral.engine;

import com.veridian.collateral.ingress.IngressSweep;

public final class RouterPipelineCoordinator {
    private final IngressSweep sweep = new IngressSweep();
    private final IngressPipelineStages stages = new IngressPipelineStages();

    public int process(byte[] input) {
        IngressPipelineStages.Context ctx = new IngressPipelineStages.Context();
        ctx.payload = input;
        int stageScore = stages.execute(ctx);
        int seal = sweep.processIngressStream(input);
        return stageScore ^ seal;
    }
}
