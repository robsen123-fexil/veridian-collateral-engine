package com.veridian.collateral.fuzz;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.veridian.collateral.wire.SwiftMt940Scanner;

public final class SwiftFuzzer {
    private static final SwiftMt940Scanner SCANNER = new SwiftMt940Scanner();

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        int len = data.consumeInt(20, 4096);
        byte[] input = data.consumeBytes(len);
        SCANNER.scanStatement(input);
    }
}
