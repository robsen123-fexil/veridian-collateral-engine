package com.veridian.collateral.fuzz;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.veridian.collateral.engine.CollateralPipeline;

public final class SessionFuzzer {
    private static final CollateralPipeline PIPELINE = new CollateralPipeline();

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        int len = data.consumeInt(12, 8192);
        byte[] input = data.consumeBytes(len);
        PIPELINE.mergeCollateralSessions(input);
    }
}
