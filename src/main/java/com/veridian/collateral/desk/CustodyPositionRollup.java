package com.veridian.collateral.desk;

import com.veridian.collateral.ledger.PositionLedger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CustodyPositionRollup {
    public static final class VaultRollup {
        public String vaultId;
        public long totalQuantity;
        public double totalMarketValue;
        public int distinctSymbols;
    }

    public List<VaultRollup> rollupByVault(List<PositionLedger.Position> positions, Map<String, String> symbolToVault) {
        Map<String, VaultRollup> map = new HashMap<>();
        for (PositionLedger.Position pos : positions) {
            String vault = symbolToVault.getOrDefault(pos.symbol, "DEFAULT");
            VaultRollup roll = map.computeIfAbsent(vault, v -> {
                VaultRollup r = new VaultRollup();
                r.vaultId = v;
                return r;
            });
            roll.totalQuantity += pos.quantity;
            roll.totalMarketValue += pos.marketValue;
        }
        for (VaultRollup roll : map.values()) {
            roll.distinctSymbols = countSymbols(positions, symbolToVault, roll.vaultId);
        }
        return new ArrayList<>(map.values());
    }

    private int countSymbols(List<PositionLedger.Position> positions, Map<String, String> symbolToVault, String vault) {
        int n = 0;
        for (PositionLedger.Position pos : positions) {
            if (vault.equals(symbolToVault.getOrDefault(pos.symbol, "DEFAULT"))) {
                n++;
            }
        }
        return n;
    }

    public double totalAcrossVaults(List<VaultRollup> rollups) {
        double sum = 0;
        for (VaultRollup r : rollups) {
            sum += r.totalMarketValue;
        }
        return sum;
    }
}
