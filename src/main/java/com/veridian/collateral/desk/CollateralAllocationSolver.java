package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CollateralAllocationSolver {
    public static final class AccountNeed {
        public String accountId;
        public double requiredCollateral;
        public int priority;
    }

    public static final class AssetSupply {
        public String cusip;
        public String assetClass;
        public String rating;
        public double marketValue;
        public double availableValue;
    }

    public static final class Allocation {
        public String accountId;
        public String cusip;
        public double allocatedValue;
        public double postHaircutValue;
    }

    private final HaircutGrid haircuts = new HaircutGrid();

    public List<Allocation> solve(List<AccountNeed> needs, List<AssetSupply> supplies) {
        List<AccountNeed> sortedNeeds = new ArrayList<>(needs);
        sortedNeeds.sort(Comparator.comparingInt((AccountNeed n) -> n.priority).reversed());
        List<AssetSupply> sortedSupply = new ArrayList<>(supplies);
        sortedSupply.sort(Comparator.comparingDouble((AssetSupply s) -> s.availableValue).reversed());
        List<Allocation> allocations = new ArrayList<>();
        for (AccountNeed need : sortedNeeds) {
            double remaining = need.requiredCollateral;
            for (AssetSupply supply : sortedSupply) {
                if (remaining <= 0) {
                    break;
                }
                if (supply.availableValue <= 0) {
                    continue;
                }
                double h = haircuts.effectiveHaircut(supply.assetClass, supply.rating, false);
                double postHaircutUnit = supply.marketValue <= 0 ? 0 : (1.0 - h);
                double take = Math.min(supply.availableValue, remaining / Math.max(0.0001, postHaircutUnit));
                Allocation alloc = new Allocation();
                alloc.accountId = need.accountId;
                alloc.cusip = supply.cusip;
                alloc.allocatedValue = take;
                alloc.postHaircutValue = take * postHaircutUnit;
                allocations.add(alloc);
                supply.availableValue -= take;
                remaining -= alloc.postHaircutValue;
            }
        }
        return allocations;
    }

    public double totalAllocatedPostHaircut(List<Allocation> allocations) {
        double sum = 0;
        for (Allocation a : allocations) {
            sum += a.postHaircutValue;
        }
        return sum;
    }

    public double unmetNeed(List<AccountNeed> needs, List<Allocation> allocations) {
        double required = 0;
        for (AccountNeed n : needs) {
            required += n.requiredCollateral;
        }
        return Math.max(0, required - totalAllocatedPostHaircut(allocations));
    }
}
