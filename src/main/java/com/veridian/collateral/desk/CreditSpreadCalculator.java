package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class CreditSpreadCalculator {
    public static final class SpreadQuote {
        public String cusip;
        public double benchmarkYield;
        public double bondYield;
        public double spreadBps;
    }

    public SpreadQuote compute(String cusip, double bondYield, double benchmarkYield) {
        SpreadQuote q = new SpreadQuote();
        q.cusip = cusip;
        q.bondYield = bondYield;
        q.benchmarkYield = benchmarkYield;
        q.spreadBps = (bondYield - benchmarkYield) * 10_000.0;
        return q;
    }

    public List<SpreadQuote> computeBatch(List<String> cusips, List<Double> bondYields, double benchmark) {
        List<SpreadQuote> out = new ArrayList<>();
        for (int i = 0; i < cusips.size() && i < bondYields.size(); i++) {
            out.add(compute(cusips.get(i), bondYields.get(i), benchmark));
        }
        return out;
    }

    public double weightedAverageSpread(List<SpreadQuote> quotes, List<Double> weights) {
        double num = 0;
        double den = 0;
        for (int i = 0; i < quotes.size(); i++) {
            double w = i < weights.size() ? weights.get(i) : 1.0;
            num += quotes.get(i).spreadBps * w;
            den += w;
        }
        return den <= 0 ? 0 : num / den;
    }
}
