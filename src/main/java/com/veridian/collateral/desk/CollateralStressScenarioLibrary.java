package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class CollateralStressScenarioLibrary {
    public static final class Scenario {
        public String name;
        public double equityShock;
        public double rateShockBps;
        public double fxShockPct;
        public String fxCurrency;
    }

    public List<Scenario> standardScenarios() {
        List<Scenario> out = new ArrayList<>();
        out.add(scenario("BASE", 0, 0, 0, "USD"));
        out.add(scenario("EQ_DOWN_5", -0.05, 0, 0, "USD"));
        out.add(scenario("EQ_DOWN_10", -0.10, 0, 0, "USD"));
        out.add(scenario("RATES_UP_100", 0, 100, 0, "USD"));
        out.add(scenario("RATES_UP_200", 0, 200, 0, "USD"));
        out.add(scenario("FX_USD_DOWN_5", 0, 0, -0.05, "USD"));
        out.add(scenario("COMBINED_STRESS", -0.08, 150, -0.03, "USD"));
        return out;
    }

    private Scenario scenario(String name, double eq, double rates, double fx, String ccy) {
        Scenario s = new Scenario();
        s.name = name;
        s.equityShock = eq;
        s.rateShockBps = rates;
        s.fxShockPct = fx;
        s.fxCurrency = ccy;
        return s;
    }

    public double runScenario(Scenario scenario, List<ExposureCalculator.ExposureLine> lines) {
        ExposureCalculator calc = new ExposureCalculator();
        return calc.combinedStress(lines, scenario.equityShock, scenario.rateShockBps,
            scenario.fxCurrency, scenario.fxShockPct);
    }

    public Scenario worstScenario(List<Scenario> scenarios, List<ExposureCalculator.ExposureLine> lines) {
        Scenario worst = null;
        double worstPnl = Double.POSITIVE_INFINITY;
        for (Scenario s : scenarios) {
            double pnl = runScenario(s, lines);
            if (pnl < worstPnl) {
                worstPnl = pnl;
                worst = s;
            }
        }
        return worst;
    }
}
