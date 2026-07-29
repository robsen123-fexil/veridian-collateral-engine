package com.veridian.collateral.ledger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class AuditQueryEngine {
    public static final class Query {
        public String module = "";
        public String action = "";
        public long afterId;
        public int limit = 100;
        public Predicate<AuditSpool.AuditRecord> filter = r -> true;
    }

    private final AuditSpool spool;

    public AuditQueryEngine(AuditSpool spool) {
        this.spool = spool;
    }

    public List<AuditSpool.AuditRecord> execute(Query query) {
        List<AuditSpool.AuditRecord> base = spool.queryByModule(query.module, query.limit);
        List<AuditSpool.AuditRecord> out = new ArrayList<>();
        for (AuditSpool.AuditRecord rec : base) {
            if (rec.id < query.afterId) {
                continue;
            }
            if (!query.action.isEmpty() && !query.action.equals(rec.action)) {
                continue;
            }
            if (!query.filter.test(rec)) {
                continue;
            }
            out.add(rec);
            if (out.size() >= query.limit) {
                break;
            }
        }
        return out;
    }

    public int countByAction(String module, String action) {
        Query q = new Query();
        q.module = module;
        q.action = action;
        q.limit = Integer.MAX_VALUE;
        return execute(q).size();
    }

    public AuditSpool.AuditRecord latestForModule(String module) {
        List<AuditSpool.AuditRecord> recs = spool.queryByModule(module, 1);
        return recs.isEmpty() ? null : recs.get(0);
    }
}
