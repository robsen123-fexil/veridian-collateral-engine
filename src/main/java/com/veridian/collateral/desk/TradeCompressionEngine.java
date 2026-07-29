package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TradeCompressionEngine {
    public static final class TradeLeg {
        public String tradeId;
        public String symbol;
        public double notional;
        public double rate;
        public String direction;
    }

    public static final class CompressedBundle {
        public String bundleId;
        public String symbol;
        public double netNotional;
        public double weightedRate;
        public int legCount;
    }

    public List<CompressedBundle> compressBySymbol(List<TradeLeg> legs) {
        Map<String, List<TradeLeg>> bySymbol = new HashMap<>();
        for (TradeLeg leg : legs) {
            bySymbol.computeIfAbsent(leg.symbol, k -> new ArrayList<>()).add(leg);
        }
        List<CompressedBundle> out = new ArrayList<>();
        int bundleSeq = 1;
        for (Map.Entry<String, List<TradeLeg>> e : bySymbol.entrySet()) {
            CompressedBundle bundle = new CompressedBundle();
            bundle.bundleId = "BND-" + (bundleSeq++);
            bundle.symbol = e.getKey();
            double pay = 0;
            double recv = 0;
            double payRateWeighted = 0;
            double recvRateWeighted = 0;
            for (TradeLeg leg : e.getValue()) {
                if ("PAY".equals(leg.direction)) {
                    pay += leg.notional;
                    payRateWeighted += leg.notional * leg.rate;
                } else {
                    recv += leg.notional;
                    recvRateWeighted += leg.notional * leg.rate;
                }
            }
            bundle.netNotional = recv - pay;
            double total = pay + recv;
            bundle.weightedRate = total <= 0 ? 0 : (payRateWeighted + recvRateWeighted) / total;
            bundle.legCount = e.getValue().size();
            out.add(bundle);
        }
        return out;
    }

    public double compressionRatio(List<TradeLeg> legs, List<CompressedBundle> bundles) {
        if (bundles.isEmpty()) {
            return 0;
        }
        return 1.0 - ((double) bundles.size() / (double) legs.size());
    }
}
