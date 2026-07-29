package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class CollateralRecallEngine {
    public static final class RecallRequest {
        public String id;
        public String accountId;
        public String cusip;
        public long quantity;
        public String reason;
        public boolean fulfilled;
    }

    private final List<RecallRequest> queue = new ArrayList<>();
    private long seq;

    public RecallRequest submit(String accountId, String cusip, long qty, String reason) {
        RecallRequest req = new RecallRequest();
        req.id = "RCL-" + (++seq);
        req.accountId = accountId;
        req.cusip = cusip;
        req.quantity = qty;
        req.reason = reason;
        queue.add(req);
        return req;
    }

    public List<RecallRequest> openByAccount(String accountId) {
        List<RecallRequest> out = new ArrayList<>();
        for (RecallRequest req : queue) {
            if (!req.fulfilled && req.accountId.equals(accountId)) {
                out.add(req);
            }
        }
        return out;
    }

    public boolean fulfill(String id, long returnedQty) {
        for (RecallRequest req : queue) {
            if (req.id.equals(id)) {
                req.quantity -= returnedQty;
                if (req.quantity <= 0) {
                    req.fulfilled = true;
                    req.quantity = 0;
                }
                return true;
            }
        }
        return false;
    }

    public long openQuantity(String cusip) {
        long sum = 0;
        for (RecallRequest req : queue) {
            if (!req.fulfilled && req.cusip.equals(cusip)) {
                sum += req.quantity;
            }
        }
        return sum;
    }
}
