package com.veridian.collateral.tools;

import com.veridian.collateral.desk.ConstraintAllocator;
import com.veridian.collateral.desk.HaircutGrid;
import com.veridian.collateral.desk.ProRataAllocator;
import com.veridian.collateral.engine.CollateralPipeline;

import java.util.ArrayList;
import java.util.List;

public final class VeridianCtl {
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        switch (args[0]) {
            case "allocate" -> runAllocate(args);
            case "haircut" -> runHaircut(args);
            case "pipeline" -> runPipeline(args);
            default -> printUsage();
        }
    }

    private static void runAllocate(String[] args) {
        long shares = 1000;
        if (args.length > 1) {
            shares = Long.parseLong(args[1]);
        }
        ProRataAllocator allocator = new ProRataAllocator();
        List<ProRataAllocator.AccountSlice> accounts = new ArrayList<>();
        accounts.add(slice("ACC-A", 10, 0.4));
        accounts.add(slice("ACC-B", 5, 0.3));
        accounts.add(slice("ACC-C", 1, 0.3));
        List<ProRataAllocator.AccountSlice> result = allocator.allocate(shares, accounts);
        for (ProRataAllocator.AccountSlice s : result) {
            System.out.println(s.accountId + " -> " + s.allocated);
        }
        System.out.println("total=" + allocator.sumAllocated(result));
    }

    private static void runHaircut(String[] args) {
        HaircutGrid grid = new HaircutGrid();
        double mv = args.length > 1 ? Double.parseDouble(args[1]) : 1_000_000;
        double cv = grid.collateralValue(mv, "GOVT", "AAA", false);
        System.out.println("collateral_value=" + cv);
    }

    private static void runPipeline(String[] args) {
        CollateralPipeline pipeline = new CollateralPipeline();
        byte[] sample = new byte[] {0x31, 0x42, 0x43, 0x56};
        System.out.println("digest=" + pipeline.runCollateralPipeline(sample));
    }

    private static ProRataAllocator.AccountSlice slice(String id, long pri, double w) {
        ProRataAllocator.AccountSlice s = new ProRataAllocator.AccountSlice();
        s.accountId = id;
        s.priority = pri;
        s.weight = w;
        return s;
    }

    private static void printUsage() {
        System.out.println("veridianctl allocate [shares]");
        System.out.println("veridianctl haircut [marketValue]");
        System.out.println("veridianctl pipeline");
    }
}
