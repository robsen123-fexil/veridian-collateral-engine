package com.veridian.collateral.desk;

import com.veridian.collateral.util.Fnv1a32;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class WireIngressLedger {
    public static final class IngressRecord {
        public long sequence;
        public String wireId;
        public String originator;
        public String beneficiary;
        public double amount;
        public int payloadDigest;
        public long receivedAt;
        public boolean duplicate;
    }

    private final List<IngressRecord> records = new ArrayList<>();
    private final Set<Integer> seenDigests = new HashSet<>();
    private long nextSeq = 1;

    public IngressRecord recordWire(String wireId, String originator, String beneficiary, double amount, byte[] payload) {
        IngressRecord rec = new IngressRecord();
        rec.sequence = nextSeq++;
        rec.wireId = wireId;
        rec.originator = originator;
        rec.beneficiary = beneficiary;
        rec.amount = amount;
        rec.payloadDigest = Fnv1a32.hash(payload, 0, payload.length);
        rec.receivedAt = System.currentTimeMillis();
        rec.duplicate = !seenDigests.add(rec.payloadDigest);
        records.add(rec);
        return rec;
    }

    public List<IngressRecord> listDuplicates() {
        List<IngressRecord> out = new ArrayList<>();
        for (IngressRecord rec : records) {
            if (rec.duplicate) {
                out.add(rec);
            }
        }
        return out;
    }

    public double sumByBeneficiary(String beneficiary) {
        double sum = 0;
        for (IngressRecord rec : records) {
            if (rec.beneficiary.equals(beneficiary) && !rec.duplicate) {
                sum += rec.amount;
            }
        }
        return sum;
    }

    public int size() {
        return records.size();
    }

    public IngressRecord latest() {
        if (records.isEmpty()) {
            return null;
        }
        return records.get(records.size() - 1);
    }

    public List<IngressRecord> slice(long fromSeq, int limit) {
        List<IngressRecord> out = new ArrayList<>();
        for (IngressRecord rec : records) {
            if (rec.sequence >= fromSeq && out.size() < limit) {
                out.add(rec);
            }
        }
        return out;
    }
}
