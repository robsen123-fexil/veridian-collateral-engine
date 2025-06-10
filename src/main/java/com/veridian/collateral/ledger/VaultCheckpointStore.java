package com.veridian.collateral.ledger;

import com.veridian.collateral.util.Fnv1a32;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class VaultCheckpointStore {
    public static final class Checkpoint {
        public long id;
        public int digest;
        public String label;
        public long createdAt;
        public final List<String> refs = new ArrayList<>();
    }

    private final List<Checkpoint> checkpoints = new ArrayList<>();
    private long nextId = 1;

    public Checkpoint create(String label, List<String> refs) {
        Checkpoint cp = new Checkpoint();
        cp.id = nextId++;
        cp.label = label;
        cp.createdAt = System.currentTimeMillis();
        if (refs != null) {
            cp.refs.addAll(refs);
        }
        cp.digest = computeDigest(cp);
        checkpoints.add(cp);
        return cp;
    }

    private int computeDigest(Checkpoint cp) {
        StringBuilder sb = new StringBuilder();
        sb.append(cp.label);
        for (String ref : cp.refs) {
            sb.append('|').append(ref);
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.US_ASCII);
        return Fnv1a32.hash(bytes, 0, bytes.length);
    }

    public Checkpoint latest() {
        if (checkpoints.isEmpty()) {
            return null;
        }
        return checkpoints.get(checkpoints.size() - 1);
    }

    public int count() {
        return checkpoints.size();
    }
}
