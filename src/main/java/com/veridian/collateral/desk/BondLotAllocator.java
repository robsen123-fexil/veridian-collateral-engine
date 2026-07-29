package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BondLotAllocator {
    public static final class Lot {
        public String lotId;
        public String cusip;
        public long quantity;
        public double costBasis;
    }

    public static final class AllocationRequest {
        public String targetAccount;
        public long requestedQty;
        public long allocatedQty;
    }

    public List<AllocationRequest> allocateFifo(List<Lot> lots, List<AllocationRequest> requests) {
        List<Lot> sorted = new ArrayList<>(lots);
        sorted.sort(Comparator.comparing(l -> l.lotId));
        List<AllocationRequest> out = new ArrayList<>();
        for (AllocationRequest req : requests) {
            AllocationRequest copy = new AllocationRequest();
            copy.targetAccount = req.targetAccount;
            copy.requestedQty = req.requestedQty;
            copy.allocatedQty = consumeFifo(sorted, req.requestedQty);
            out.add(copy);
        }
        return out;
    }

    private long consumeFifo(List<Lot> lots, long qty) {
        long remaining = qty;
        long allocated = 0;
        for (Lot lot : lots) {
            if (remaining <= 0) {
                break;
            }
            if (lot.quantity <= 0) {
                continue;
            }
            long take = Math.min(lot.quantity, remaining);
            lot.quantity -= take;
            remaining -= take;
            allocated += take;
        }
        return allocated;
    }

    public double remainingCostBasis(List<Lot> lots) {
        double sum = 0;
        for (Lot lot : lots) {
            sum += lot.quantity * lot.costBasis;
        }
        return sum;
    }
}
