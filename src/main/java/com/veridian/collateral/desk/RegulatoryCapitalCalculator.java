package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class RegulatoryCapitalCalculator {
    public static final class CapitalLine {
        public String exposureClass;
        public double exposureAmount;
        public double riskWeight;
        public double rwa;
        public double capitalCharge;
    }

    public static final class CapitalResult {
        public final List<CapitalLine> lines = new ArrayList<>();
        public double totalRwa;
        public double totalCapital;
    }

    public CapitalResult calculate(List<CapitalLine> inputs, double capitalRatio) {
        CapitalResult result = new CapitalResult();
        for (CapitalLine in : inputs) {
            CapitalLine line = new CapitalLine();
            line.exposureClass = in.exposureClass;
            line.exposureAmount = in.exposureAmount;
            line.riskWeight = normalizeWeight(in.riskWeight, in.exposureClass);
            line.rwa = line.exposureAmount * line.riskWeight;
            line.capitalCharge = line.rwa * capitalRatio;
            result.lines.add(line);
            result.totalRwa += line.rwa;
            result.totalCapital += line.capitalCharge;
        }
        return result;
    }

    private double normalizeWeight(double weight, String exposureClass) {
        if (weight > 0) {
            return weight;
        }
        return switch (exposureClass) {
            case "SOVEREIGN" -> 0.0;
            case "BANK" -> 0.2;
            case "CORPORATE" -> 1.0;
            case "EQUITY" -> 1.0;
            case "SECURITIZATION" -> 1.25;
            default -> 1.0;
        };
    }

    public double leverageRatio(double tier1Capital, double totalExposure) {
        if (totalExposure <= 0) {
            return 0;
        }
        return tier1Capital / totalExposure;
    }

    public boolean meetsMinimumRatio(CapitalResult result, double minimumRatio, double tier1) {
        if (result.totalRwa <= 0) {
            return false;
        }
        return (tier1 / result.totalRwa) >= minimumRatio;
    }
}
