package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VaultInventoryScanner {
    public static final class InventoryLine {
        public String vaultId;
        public String cusip;
        public long quantity;
        public double marketPrice;
        public double marketValue;
        public String assetClass;
        public boolean eligible;
    }

    public static final class ScanResult {
        public final List<InventoryLine> lines = new ArrayList<>();
        public double totalValue;
        public int ineligibleCount;
        public final Map<String, Double> valueByVault = new HashMap<>();
    }

    private final EligibleAssetRegistry registry = new EligibleAssetRegistry();
    private final HaircutGrid haircuts = new HaircutGrid();

    public VaultInventoryScanner() {
        registry.register("US912828XW86", "GOVT", "AAA", "UST", 0.99);
        registry.register("CorpIG1", "CORP", "IG", "ACME", 0.85);
        registry.register("EqLarge1", "EQUITY", "LARGE", "BIGCO", 0.75);
    }

    public ScanResult scan(List<InventoryLine> input) {
        ScanResult result = new ScanResult();
        for (InventoryLine line : input) {
            InventoryLine copy = copyLine(line);
            EligibleAssetRegistry.AssetRecord rec = registry.lookup(copy.cusip);
            copy.eligible = rec != null && rec.eligible;
            copy.marketValue = copy.quantity * copy.marketPrice;
            if (!copy.eligible) {
                result.ineligibleCount++;
            }
            result.lines.add(copy);
            result.totalValue += copy.marketValue;
            result.valueByVault.merge(copy.vaultId, copy.marketValue, Double::sum);
        }
        return result;
    }

    public double collateralizedValue(ScanResult result, boolean stress) {
        double sum = 0;
        for (InventoryLine line : result.lines) {
            if (!line.eligible) {
                continue;
            }
            String assetClass = line.assetClass == null ? "CORP" : line.assetClass;
            sum += haircuts.collateralValue(line.marketValue, assetClass, "IG", stress);
        }
        return sum;
    }

    private InventoryLine copyLine(InventoryLine src) {
        InventoryLine copy = new InventoryLine();
        copy.vaultId = src.vaultId;
        copy.cusip = src.cusip;
        copy.quantity = src.quantity;
        copy.marketPrice = src.marketPrice;
        copy.assetClass = src.assetClass;
        return copy;
    }
}
