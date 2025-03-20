package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class SpanMarginCalculator {
    public static final class ScanScenario {
        public final String name;
        public final double priceShift;
        public final double volShift;

        public ScanScenario(String name, double priceShift, double volShift) {
            this.name = name;
            this.priceShift = priceShift;
            this.volShift = volShift;
        }
    }

    public static final class PortfolioLeg {
        public String symbol;
        public double delta;
        public double gamma;
        public double vega;
        public double price;
        public double quantity;
    }

    private final List<ScanScenario> scenarios = new ArrayList<>();

    public SpanMarginCalculator() {
        scenarios.add(new ScanScenario("UP_1", 0.01, 0.0));
        scenarios.add(new ScanScenario("DOWN_1", -0.01, 0.0));
        scenarios.add(new ScanScenario("UP_2", 0.02, 0.0));
        scenarios.add(new ScanScenario("DOWN_2", -0.02, 0.0));
        scenarios.add(new ScanScenario("VOL_UP", 0.0, 0.05));
        scenarios.add(new ScanScenario("VOL_DOWN", 0.0, -0.05));
    }

    public double scanningRisk(List<PortfolioLeg> legs) {
        double worst = Double.NEGATIVE_INFINITY;
        for (ScanScenario scenario : scenarios) {
            double pnl = scenarioPnl(legs, scenario);
            worst = Math.max(worst, -pnl);
        }
        return Math.max(0, worst);
    }

    private double scenarioPnl(List<PortfolioLeg> legs, ScanScenario scenario) {
        double pnl = 0;
        for (PortfolioLeg leg : legs) {
            double shifted = leg.price * (1.0 + scenario.priceShift);
            double value = leg.quantity * shifted * leg.delta;
            double volAddon = leg.vega * scenario.volShift;
            double gammaAddon = 0.5 * leg.gamma * leg.quantity * Math.pow(leg.price * scenario.priceShift, 2);
            pnl += value + volAddon + gammaAddon;
        }
        return pnl;
    }

    public double totalMargin(List<PortfolioLeg> legs, double collateral) {
        return scanningRisk(legs) - collateral;
    }
}
