package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Iterative collateral substitution solver: given a target collateral value and eligible
 * instruments, selects lots to minimize haircut drag while meeting concentration limits.
 */
public final class CollateralSubstitutionEngine {
    public static final class CandidateLot {
        public String cusip;
        public String assetClass;
        public String rating;
        public double marketValue;
        public double haircut;
        public double issuerConcentration;
    }

    public static final class SubstitutionPlan {
        public final List<CandidateLot> selected = new ArrayList<>();
        public double totalCollateralValue;
        public double totalPostHaircut;
        public boolean meetsTarget;
    }

    private final HaircutGrid haircutGrid = new HaircutGrid();

    public SubstitutionPlan solve(double targetPostHaircut, double maxConcentration, List<CandidateLot> candidates) {
        SubstitutionPlan plan = new SubstitutionPlan();
        List<CandidateLot> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingDouble(this::effectiveValue).reversed());

        double accumulated = 0;
        for (CandidateLot lot : sorted) {
            if (lot.issuerConcentration > maxConcentration) {
                continue;
            }
            double h = lot.haircut > 0 ? lot.haircut
                : haircutGrid.effectiveHaircut(lot.assetClass, lot.rating, false);
            lot.haircut = h;
            double post = lot.marketValue * (1.0 - h);
            if (post <= 0) {
                continue;
            }
            plan.selected.add(lot);
            plan.totalCollateralValue += lot.marketValue;
            accumulated += post;
            plan.totalPostHaircut = accumulated;
            if (accumulated >= targetPostHaircut) {
                plan.meetsTarget = true;
                return plan;
            }
        }
        plan.meetsTarget = accumulated >= targetPostHaircut;
        plan.totalPostHaircut = accumulated;
        return plan;
    }

    public SubstitutionPlan replaceLot(SubstitutionPlan current, String removeCusip, CandidateLot replacement,
                                     double targetPostHaircut) {
        List<CandidateLot> rebuilt = new ArrayList<>();
        for (CandidateLot lot : current.selected) {
            if (!lot.cusip.equals(removeCusip)) {
                rebuilt.add(lot);
            }
        }
        rebuilt.add(replacement);
        return solve(targetPostHaircut, 0.25, rebuilt);
    }

    private double effectiveValue(CandidateLot lot) {
        double h = lot.haircut > 0 ? lot.haircut
            : haircutGrid.effectiveHaircut(lot.assetClass, lot.rating, false);
        return lot.marketValue * (1.0 - h);
    }

    public double computeShortfall(SubstitutionPlan plan, double target) {
        return Math.max(0, target - plan.totalPostHaircut);
    }

    public List<CandidateLot> rankByLiquidityTier(List<CandidateLot> lots, int preferredTier) {
        List<CandidateLot> ranked = new ArrayList<>(lots);
        ranked.sort((a, b) -> {
            int tierA = tierForAsset(a.assetClass);
            int tierB = tierForAsset(b.assetClass);
            int distA = Math.abs(tierA - preferredTier);
            int distB = Math.abs(tierB - preferredTier);
            if (distA != distB) {
                return Integer.compare(distA, distB);
            }
            return Double.compare(effectiveValue(b), effectiveValue(a));
        });
        return ranked;
    }

    private int tierForAsset(String assetClass) {
        return switch (assetClass) {
            case "GOVT" -> 1;
            case "CORP" -> 2;
            case "EQUITY" -> 3;
            default -> 4;
        };
    }
}
