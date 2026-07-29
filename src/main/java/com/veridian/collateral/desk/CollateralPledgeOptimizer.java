package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class CollateralPledgeOptimizer {
    public static final class PledgeCandidate {
        public String cusip;
        public double marketValue;
        public double haircut;
        public int liquidityDays;
        public double score;
    }

    public List<PledgeCandidate> rankForPledge(List<PledgeCandidate> candidates, boolean preferLiquid) {
        List<PledgeCandidate> ranked = new ArrayList<>(candidates);
        ranked.sort((a, b) -> {
            double scoreA = score(a, preferLiquid);
            double scoreB = score(b, preferLiquid);
            a.score = scoreA;
            b.score = scoreB;
            return Double.compare(scoreB, scoreA);
        });
        return ranked;
    }

    private double score(PledgeCandidate c, boolean preferLiquid) {
        double collateralValue = c.marketValue * (1.0 - c.haircut);
        double liquidityBonus = preferLiquid ? 1.0 / Math.max(1, c.liquidityDays) : 0;
        return collateralValue * (1.0 + liquidityBonus);
    }

    public double selectMinimumSet(List<PledgeCandidate> ranked, double targetPostHaircut) {
        double accumulated = 0;
        for (PledgeCandidate c : ranked) {
            accumulated += c.marketValue * (1.0 - c.haircut);
            if (accumulated >= targetPostHaircut) {
                return accumulated;
            }
        }
        return accumulated;
    }

    public int countNeededForTarget(List<PledgeCandidate> ranked, double targetPostHaircut) {
        double accumulated = 0;
        int count = 0;
        for (PledgeCandidate c : ranked) {
            accumulated += c.marketValue * (1.0 - c.haircut);
            count++;
            if (accumulated >= targetPostHaircut) {
                return count;
            }
        }
        return count;
    }

    public double averageHaircut(List<PledgeCandidate> selected) {
        if (selected.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (PledgeCandidate c : selected) {
            sum += c.haircut;
        }
        return sum / selected.size();
    }
}
