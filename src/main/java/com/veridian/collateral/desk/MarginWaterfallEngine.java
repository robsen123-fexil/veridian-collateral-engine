package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class MarginWaterfallEngine {
    public static final class WaterfallTier {
        public String name;
        public double cap;
        public double filled;
    }

    public List<WaterfallTier> apply(double totalMargin, List<WaterfallTier> tiers) {
        List<WaterfallTier> out = new ArrayList<>();
        double remaining = totalMargin;
        for (WaterfallTier tier : tiers) {
            WaterfallTier copy = new WaterfallTier();
            copy.name = tier.name;
            copy.cap = tier.cap;
            double fill = Math.min(tier.cap, Math.max(0, remaining));
            copy.filled = fill;
            remaining -= fill;
            out.add(copy);
        }
        return out;
    }

    public double unfilled(List<WaterfallTier> tiers) {
        double sum = 0;
        for (WaterfallTier tier : tiers) {
            sum += Math.max(0, tier.cap - tier.filled);
        }
        return sum;
    }
}
