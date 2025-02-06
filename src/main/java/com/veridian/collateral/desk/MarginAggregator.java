package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MarginAggregator {
    public static final class MarginLeg {
        public String symbol;
        public double notional;
        public double delta;
        public double vega;
        public double spanScanningRisk;
    }

    public static final class MarginResult {
        public double initialMargin;
        public double variationMargin;
        public double totalRequirement;
        public double excess;
        public final List<String> details = new ArrayList<>();
    }

    private final HaircutGrid haircutGrid = new HaircutGrid();
    private final Map<String, Double> assetMultipliers = new HashMap<>();

    public MarginAggregator() {
        assetMultipliers.put("EQUITY", 1.15);
        assetMultipliers.put("GOVT", 1.02);
        assetMultipliers.put("CORP", 1.08);
    }

    public MarginResult aggregate(List<MarginLeg> legs, double collateralValue, boolean stress) {
        MarginResult result = new MarginResult();
        double scanRisk = 0;
        for (MarginLeg leg : legs) {
            double legRisk = computeLegRisk(leg, stress);
            scanRisk += legRisk;
            result.details.add(leg.symbol + "=" + legRisk);
        }
        result.initialMargin = scanRisk;
        result.variationMargin = computeVariation(legs);
        result.totalRequirement = result.initialMargin + result.variationMargin;
        result.excess = collateralValue - result.totalRequirement;
        return result;
    }

    private double computeLegRisk(MarginLeg leg, boolean stress) {
        double base = Math.abs(leg.notional * leg.delta);
        double volAddon = Math.abs(leg.vega) * 0.10;
        double span = leg.spanScanningRisk > 0 ? leg.spanScanningRisk : base * 0.12;
        double mult = assetMultipliers.getOrDefault(guessAssetClass(leg.symbol), 1.10);
        double h = haircutGrid.effectiveHaircut(guessAssetClass(leg.symbol), "IG", stress);
        return (base + volAddon + span) * mult * (1.0 + h);
    }

    private double computeVariation(List<MarginLeg> legs) {
        double var = 0;
        for (MarginLeg leg : legs) {
            var += leg.notional * 0.002;
        }
        return var;
    }

    private String guessAssetClass(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            return "CORP";
        }
        if (symbol.startsWith("UST") || symbol.startsWith("GOVT")) {
            return "GOVT";
        }
        if (symbol.endsWith("EQ")) {
            return "EQUITY";
        }
        return "CORP";
    }

    public boolean isMarginCall(MarginResult result) {
        return result.excess < 0;
    }

    public double callAmount(MarginResult result) {
        return result.excess < 0 ? -result.excess : 0;
    }
}
