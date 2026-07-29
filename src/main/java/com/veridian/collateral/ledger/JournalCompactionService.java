package com.veridian.collateral.ledger;

import java.util.ArrayList;
import java.util.List;

public final class JournalCompactionService {
    public static final class CompactionResult {
        public int removed;
        public int retained;
        public long oldestKeptSequence;
    }

    public CompactionResult compactPositionJournal(PositionLedger ledger, long keepAfterSequence) {
        CompactionResult result = new CompactionResult();
        List<PositionLedger.LedgerEntry> slice = ledger.sliceJournal(keepAfterSequence, Integer.MAX_VALUE);
        result.retained = slice.size();
        result.oldestKeptSequence = keepAfterSequence;
        result.removed = (int) Math.max(0, ledger.journalSize() - result.retained);
        return result;
    }

    public CompactionResult compactAuditSpool(AuditSpool spool, String module, int keepLatest) {
        CompactionResult result = new CompactionResult();
        List<AuditSpool.AuditRecord> records = spool.queryByModule(module, keepLatest);
        result.retained = records.size();
        result.removed = Math.max(0, spool.size() - result.retained);
        return result;
    }
}
