package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CollateralInventoryReconciler {
    public static final class InventoryLine {
        public String vaultId;
        public String cusip;
        public long custodianQty;
        public long internalQty;
        public double custodianValue;
        public double internalValue;
    }

    public static final class BreakItem {
        public String vaultId;
        public String cusip;
        public long qtyBreak;
        public double valueBreak;
        public String breakType;
    }

    public List<BreakItem> reconcile(List<InventoryLine> lines, long qtyTolerance, double valueTolerance) {
        List<BreakItem> breaks = new ArrayList<>();
        for (InventoryLine line : lines) {
            long qtyDelta = line.custodianQty - line.internalQty;
            double valDelta = line.custodianValue - line.internalValue;
            if (Math.abs(qtyDelta) > qtyTolerance || Math.abs(valDelta) > valueTolerance) {
                BreakItem item = new BreakItem();
                item.vaultId = line.vaultId;
                item.cusip = line.cusip;
                item.qtyBreak = qtyDelta;
                item.valueBreak = valDelta;
                item.breakType = classifyBreak(qtyDelta, valDelta);
                breaks.add(item);
            }
        }
        return breaks;
    }

    private String classifyBreak(long qtyDelta, double valDelta) {
        if (qtyDelta != 0 && Math.abs(valDelta) < 0.01) {
            return "QUANTITY";
        }
        if (qtyDelta == 0 && Math.abs(valDelta) >= 0.01) {
            return "PRICING";
        }
        return "MIXED";
    }

    public Map<String, Double> valueBreakByVault(List<BreakItem> breaks) {
        Map<String, Double> out = new HashMap<>();
        for (BreakItem b : breaks) {
            out.merge(b.vaultId, Math.abs(b.valueBreak), Double::sum);
        }
        return out;
    }

    public double totalAbsoluteValueBreak(List<BreakItem> breaks) {
        double sum = 0;
        for (BreakItem b : breaks) {
            sum += Math.abs(b.valueBreak);
        }
        return sum;
    }

    public List<BreakItem> filterByType(List<BreakItem> breaks, String type) {
        List<BreakItem> out = new ArrayList<>();
        for (BreakItem b : breaks) {
            if (type.equals(b.breakType)) {
                out.add(b);
            }
        }
        return out;
    }
}
