package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class LiquidityStressEngine {
    public static final class LiquidityBucket {
        public String assetClass;
        public double marketValue;
        public int liquidationDays;
        public double hairCut;
    }

    public static final class StressResult {
        public double immediateLiquidity;
        public double delayedLiquidity;
        public double illiquidRemainder;
        public int maxLiquidationDays;
    }

    public StressResult evaluate(List<LiquidityBucket> buckets, double cashNeed, int horizonDays) {
        StressResult result = new StressResult();
        buckets.sort((a, b) -> Integer.compare(a.liquidationDays, b.liquidationDays));
        double remaining = cashNeed;
        for (LiquidityBucket b : buckets) {
            double available = b.marketValue * (1.0 - b.hairCut);
            if (b.liquidationDays <= 1) {
                double take = Math.min(available, remaining);
                result.immediateLiquidity += take;
                remaining -= take;
            } else if (b.liquidationDays <= horizonDays) {
                double take = Math.min(available, remaining);
                result.delayedLiquidity += take;
                remaining -= take;
            } else {
                result.illiquidRemainder += available;
            }
            result.maxLiquidationDays = Math.max(result.maxLiquidationDays, b.liquidationDays);
        }
        return result;
    }

    public boolean meetsCashNeed(StressResult result, double cashNeed) {
        return result.immediateLiquidity + result.delayedLiquidity >= cashNeed;
    }
}
