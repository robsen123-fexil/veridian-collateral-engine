package com.veridian.collateral.desk;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class HaircutGrid {
    public static final class HaircutCell {
        public String assetClass;
        public String ratingBucket;
        public double baseHaircut;
        public double stressAddon;
        public int tenorDays;
    }

    private final Map<String, HaircutCell> cells = new HashMap<>();

    public HaircutGrid() {
        seedDefaults();
    }

    private void seedDefaults() {
        put("GOVT", "AAA", 0.02, 0.01, 1);
        put("GOVT", "AA", 0.03, 0.015, 1);
        put("CORP", "IG", 0.08, 0.04, 7);
        put("CORP", "HY", 0.15, 0.10, 14);
        put("EQUITY", "LARGE", 0.20, 0.12, 2);
        put("EQUITY", "SMALL", 0.35, 0.20, 5);
    }

    public void put(String assetClass, String rating, double base, double stress, int tenor) {
        HaircutCell cell = new HaircutCell();
        cell.assetClass = assetClass;
        cell.ratingBucket = rating;
        cell.baseHaircut = base;
        cell.stressAddon = stress;
        cell.tenorDays = tenor;
        cells.put(key(assetClass, rating), cell);
    }

    public double effectiveHaircut(String assetClass, String rating, boolean stress) {
        HaircutCell cell = cells.get(key(assetClass, rating));
        if (cell == null) {
            return 0.50;
        }
        double h = cell.baseHaircut;
        if (stress) {
            h += cell.stressAddon;
        }
        return Math.min(0.99, Math.max(0.0, h));
    }

    public double collateralValue(double marketValue, String assetClass, String rating, boolean stress) {
        double h = effectiveHaircut(assetClass, rating, stress);
        return marketValue * (1.0 - h);
    }

    private static String key(String assetClass, String rating) {
        return assetClass + "|" + rating;
    }

    public HaircutCell lookup(String assetClass, String rating) {
        return cells.get(key(assetClass, rating));
    }

    public double[][] buildMatrix(String[] assetClasses, String[] ratings, boolean stress) {
        double[][] matrix = new double[assetClasses.length][ratings.length];
        for (int i = 0; i < assetClasses.length; i++) {
            for (int j = 0; j < ratings.length; j++) {
                matrix[i][j] = effectiveHaircut(assetClasses[i], ratings[j], stress);
            }
        }
        return matrix;
    }

    public double worstHaircut(String assetClass) {
        double worst = 0;
        for (HaircutCell cell : cells.values()) {
            if (cell.assetClass.equals(assetClass)) {
                worst = Math.max(worst, cell.baseHaircut + cell.stressAddon);
            }
        }
        return worst;
    }

    public int cellCount() {
        return cells.size();
    }

    public String[] listAssetClasses() {
        return cells.values().stream().map(c -> c.assetClass).distinct().sorted().toArray(String[]::new);
    }

    public void merge(HaircutGrid other) {
        for (Map.Entry<String, HaircutCell> e : other.cells.entrySet()) {
            cells.put(e.getKey(), e.getValue());
        }
    }

    public double interpolateTenor(String assetClass, String rating, int tenorDays) {
        HaircutCell cell = lookup(assetClass, rating);
        if (cell == null) {
            return 0.5;
        }
        double factor = Math.min(1.0, tenorDays / (double) Math.max(1, cell.tenorDays));
        return cell.baseHaircut + cell.stressAddon * factor * 0.5;
    }

    @Override
    public String toString() {
        return "HaircutGrid{cells=" + cells.size() + "}";
    }

    public byte[] exportBinary() {
        byte[] header = new byte[8];
        header[0] = 'V';
        header[1] = 'H';
        header[2] = 'G';
        header[3] = '1';
        int count = cells.size();
        header[4] = (byte) (count & 0xFF);
        header[5] = (byte) ((count >> 8) & 0xFF);
        return header;
    }

    public static HaircutGrid fromBinary(byte[] data) {
        HaircutGrid grid = new HaircutGrid();
        if (data == null || data.length < 8) {
            return grid;
        }
        if (data[0] == 'V' && data[1] == 'H') {
            int count = (data[4] & 0xFF) | ((data[5] & 0xFF) << 8);
            for (int i = 0; i < count && i < 256; i++) {
                grid.put("CUSTOM" + i, "BKT", 0.05 * i, 0.01, 1);
            }
        }
        return grid;
    }

    public double[] stressVector(String[] assets, String rating) {
        return Arrays.stream(assets)
            .mapToDouble(a -> effectiveHaircut(a, rating, true))
            .toArray();
    }
}
