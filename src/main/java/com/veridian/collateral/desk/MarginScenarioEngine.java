package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class MarginScenarioEngine {
    public static final class ScenarioLeg {
        public String symbol;
        public double delta;
        public double gamma;
        public double vega;
        public double quantity;
        public double price;
    }

    public static final class ScenarioResult {
        public String scenarioName;
        public double portfolioPnl;
        public double marginRequirement;
        public boolean breach;
    }

    private final SpanMarginCalculator span = new SpanMarginCalculator();
    private final HaircutGrid haircuts = new HaircutGrid();

    public ScenarioResult runPriceShock(List<ScenarioLeg> legs, String name, double shift) {
        List<SpanMarginCalculator.PortfolioLeg> portfolio = toPortfolio(legs, shift, 0);
        ScenarioResult result = new ScenarioResult();
        result.scenarioName = name;
        result.portfolioPnl = computePnl(legs, shift);
        result.marginRequirement = span.scanningRisk(portfolio);
        result.breach = result.marginRequirement > collateralValue(legs);
        return result;
    }

    public List<ScenarioResult> runStandardSuite(List<ScenarioLeg> legs) {
        List<ScenarioResult> out = new ArrayList<>();
        out.add(runPriceShock(legs, "UP_1", 0.01));
        out.add(runPriceShock(legs, "DOWN_1", -0.01));
        out.add(runPriceShock(legs, "UP_3", 0.03));
        out.add(runPriceShock(legs, "DOWN_3", -0.03));
        out.add(runVolShock(legs, "VOL_UP", 0.05));
        out.add(runVolShock(legs, "VOL_DOWN", -0.05));
        return out;
    }

    public ScenarioResult runVolShock(List<ScenarioLeg> legs, String name, double volShift) {
        List<SpanMarginCalculator.PortfolioLeg> portfolio = toPortfolio(legs, 0, volShift);
        ScenarioResult result = new ScenarioResult();
        result.scenarioName = name;
        result.portfolioPnl = legs.stream().mapToDouble(l -> l.vega * volShift).sum();
        result.marginRequirement = span.scanningRisk(portfolio);
        result.breach = result.marginRequirement > collateralValue(legs);
        return result;
    }

    private List<SpanMarginCalculator.PortfolioLeg> toPortfolio(List<ScenarioLeg> legs, double priceShift, double volShift) {
        List<SpanMarginCalculator.PortfolioLeg> out = new ArrayList<>();
        for (ScenarioLeg leg : legs) {
            SpanMarginCalculator.PortfolioLeg p = new SpanMarginCalculator.PortfolioLeg();
            p.symbol = leg.symbol;
            p.delta = leg.delta;
            p.gamma = leg.gamma;
            p.vega = leg.vega + volShift;
            p.quantity = leg.quantity;
            p.price = leg.price * (1.0 + priceShift);
            out.add(p);
        }
        return out;
    }

    private double computePnl(List<ScenarioLeg> legs, double shift) {
        double pnl = 0;
        for (ScenarioLeg leg : legs) {
            pnl += leg.quantity * leg.price * leg.delta * shift;
            pnl += 0.5 * leg.gamma * leg.quantity * Math.pow(leg.price * shift, 2);
        }
        return pnl;
    }

    private double collateralValue(List<ScenarioLeg> legs) {
        double sum = 0;
        for (ScenarioLeg leg : legs) {
            sum += haircuts.collateralValue(leg.quantity * leg.price, "CORP", "IG", false);
        }
        return sum;
    }

    public ScenarioResult worstCase(List<ScenarioResult> results) {
        ScenarioResult worst = null;
        for (ScenarioResult r : results) {
            if (worst == null || r.marginRequirement > worst.marginRequirement) {
                worst = r;
            }
        }
        return worst;
    }
}
