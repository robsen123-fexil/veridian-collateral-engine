package com.veridian.collateral.ledger;

import java.util.ArrayList;
import java.util.List;

public final class AuditSpool {
    public static final class AuditRecord {
        public long id;
        public String module;
        public String action;
        public String payloadDigest;
        public long timestamp;
    }

    private final List<AuditRecord> records = new ArrayList<>();
    private long nextId = 1;

    public long append(String module, String action, String payloadDigest) {
        AuditRecord rec = new AuditRecord();
        rec.id = nextId++;
        rec.module = module;
        rec.action = action;
        rec.payloadDigest = payloadDigest;
        rec.timestamp = System.currentTimeMillis();
        records.add(rec);
        return rec.id;
    }

    public List<AuditRecord> queryByModule(String module, int limit) {
        List<AuditRecord> out = new ArrayList<>();
        for (int i = records.size() - 1; i >= 0 && out.size() < limit; i--) {
            AuditRecord r = records.get(i);
            if (r.module.equals(module)) {
                out.add(r);
            }
        }
        return out;
    }

    public int size() {
        return records.size();
    }
}
