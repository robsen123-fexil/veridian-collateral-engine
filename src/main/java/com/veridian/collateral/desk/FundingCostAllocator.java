package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class FundingCostAllocator {
    public static final class FundingLeg {
        public String legId;
        public String currency;
        public double notional;
        public double rateBps;
        public int days;
    }

    public static final class AllocationLine {
        public String accountId;
        public String legId;
        public double allocatedCost;
        public double weight;
    }

    public List<AllocationLine> allocateByNotional(List<FundingLeg> legs, List<String> accounts) {
        double totalNotional = 0;
        for (FundingLeg leg : legs) {
            totalNotional += Math.abs(leg.notional);
        }
        List<AllocationLine> out = new ArrayList<>();
        if (totalNotional <= 0 || accounts.isEmpty()) {
            return out;
        }
        double totalCost = totalFundingCost(legs);
        double perAccountWeight = 1.0 / accounts.size();
        for (String account : accounts) {
            for (FundingLeg leg : legs) {
                AllocationLine line = new AllocationLine();
                line.accountId = account;
                line.legId = leg.legId;
                line.weight = perAccountWeight * (Math.abs(leg.notional) / totalNotional);
                line.allocatedCost = totalCost * line.weight;
                out.add(line);
            }
        }
        return out;
    }

    public double totalFundingCost(List<FundingLeg> legs) {
        double sum = 0;
        for (FundingLeg leg : legs) {
            sum += leg.notional * (leg.rateBps / 10_000.0) * (leg.days / 360.0);
        }
        return sum;
    }

    public double sumForAccount(List<AllocationLine> lines, String accountId) {
        double sum = 0;
        for (AllocationLine line : lines) {
            if (line.accountId.equals(accountId)) {
                sum += line.allocatedCost;
            }
        }
        return sum;
    }
}
