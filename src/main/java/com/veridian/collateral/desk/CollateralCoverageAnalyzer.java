package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class CollateralCoverageAnalyzer {
    public static final class CoverageInput {
        public double requiredCollateral;
        public double pledgedCollateral;
        public double inTransitCollateral;
        public double pendingHaircutAdjustment;
    }

    public static final class CoverageResult {
        public double totalAvailable;
        public double surplus;
        public double deficit;
        public double coverageRatio;
        public boolean sufficient;
    }

    public CoverageResult analyze(CoverageInput input) {
        CoverageResult result = new CoverageResult();
        result.totalAvailable = input.pledgedCollateral + input.inTransitCollateral - input.pendingHaircutAdjustment;
        result.surplus = result.totalAvailable - input.requiredCollateral;
        result.deficit = result.surplus < 0 ? -result.surplus : 0;
        result.coverageRatio = input.requiredCollateral <= 0 ? 1.0 : result.totalAvailable / input.requiredCollateral;
        result.sufficient = result.surplus >= 0;
        return result;
    }

    public List<CoverageResult> analyzeBatch(List<CoverageInput> inputs) {
        List<CoverageResult> out = new ArrayList<>();
        for (CoverageInput input : inputs) {
            out.add(analyze(input));
        }
        return out;
    }

    public int countDeficient(List<CoverageResult> results) {
        int n = 0;
        for (CoverageResult r : results) {
            if (!r.sufficient) {
                n++;
            }
        }
        return n;
    }
}
