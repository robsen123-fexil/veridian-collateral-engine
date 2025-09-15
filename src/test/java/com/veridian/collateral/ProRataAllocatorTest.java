package com.veridian.collateral;

import com.veridian.collateral.desk.ProRataAllocator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProRataAllocatorTest {
    @Test
    void allocates1000Shares400300300() {
        ProRataAllocator allocator = new ProRataAllocator();
        List<ProRataAllocator.AccountSlice> accounts = new ArrayList<>();
        accounts.add(slice("A", 1, 0.4));
        accounts.add(slice("B", 1, 0.3));
        accounts.add(slice("C", 1, 0.3));
        List<ProRataAllocator.AccountSlice> out = allocator.allocate(1000, accounts);
        assertEquals(1000, allocator.sumAllocated(out));
        assertEquals(400, out.get(0).allocated);
        assertEquals(300, out.get(1).allocated);
        assertEquals(300, out.get(2).allocated);
    }

    private static ProRataAllocator.AccountSlice slice(String id, long pri, double w) {
        ProRataAllocator.AccountSlice s = new ProRataAllocator.AccountSlice();
        s.accountId = id;
        s.priority = pri;
        s.weight = w;
        return s;
    }
}
