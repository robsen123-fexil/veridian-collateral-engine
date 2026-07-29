package com.veridian.collateral.ledger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PositionRollupEngine {
    public static final class RollupNode {
        public String key;
        public long quantity;
        public double marketValue;
        public final Map<String, RollupNode> children = new HashMap<>();
    }

    public RollupNode rollupByAccountAndSymbol(List<PositionLedger.Position> positions) {
        RollupNode root = new RollupNode();
        root.key = "ROOT";
        for (PositionLedger.Position pos : positions) {
            RollupNode account = root.children.computeIfAbsent(pos.accountId, k -> node(k));
            RollupNode symbol = account.children.computeIfAbsent(pos.symbol, k -> node(k));
            symbol.quantity += pos.quantity;
            symbol.marketValue += pos.marketValue;
            account.quantity += pos.quantity;
            account.marketValue += pos.marketValue;
            root.quantity += pos.quantity;
            root.marketValue += pos.marketValue;
        }
        return root;
    }

    private RollupNode node(String key) {
        RollupNode n = new RollupNode();
        n.key = key;
        return n;
    }

    public List<RollupNode> flattenAccounts(RollupNode root) {
        List<RollupNode> out = new ArrayList<>();
        for (RollupNode child : root.children.values()) {
            out.add(child);
        }
        return out;
    }

    public double totalMarketValue(RollupNode root) {
        return root.marketValue;
    }

    public RollupNode findAccount(RollupNode root, String accountId) {
        return root.children.get(accountId);
    }
}
