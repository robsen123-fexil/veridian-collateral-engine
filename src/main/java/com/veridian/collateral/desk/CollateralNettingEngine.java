package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CollateralNettingEngine {
    public static final class NettingObligation {
        public String partyId;
        public String currency;
        public double amount;
    }

    public static final class NettingSet {
        public String setId;
        public final List<NettingObligation> obligations = new ArrayList<>();
        public double netAmount;
        public String netCurrency = "USD";
    }

    public NettingSet netByCurrency(String setId, List<NettingObligation> obligations) {
        NettingSet set = new NettingSet();
        set.setId = setId;
        set.obligations.addAll(obligations);
        Map<String, Double> byCcy = new HashMap<>();
        for (NettingObligation ob : obligations) {
            byCcy.merge(ob.currency, ob.amount, Double::sum);
        }
        double largest = 0;
        for (Map.Entry<String, Double> e : byCcy.entrySet()) {
            if (Math.abs(e.getValue()) > Math.abs(largest)) {
                largest = e.getValue();
                set.netCurrency = e.getKey();
            }
        }
        set.netAmount = largest;
        return set;
    }

    public List<NettingSet> netByParty(Map<String, List<NettingObligation>> obligationsByParty) {
        List<NettingSet> sets = new ArrayList<>();
        for (Map.Entry<String, List<NettingObligation>> e : obligationsByParty.entrySet()) {
            sets.add(netByCurrency(e.getKey(), e.getValue()));
        }
        return sets;
    }

    public double grossExposure(List<NettingObligation> obligations) {
        double sum = 0;
        for (NettingObligation ob : obligations) {
            sum += Math.abs(ob.amount);
        }
        return sum;
    }

    public double nettingBenefit(List<NettingObligation> obligations, NettingSet set) {
        return grossExposure(obligations) - Math.abs(set.netAmount);
    }
}
