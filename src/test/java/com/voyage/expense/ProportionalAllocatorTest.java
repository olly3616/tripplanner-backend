package com.voyage.expense;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.voyage.expense.service.ProportionalAllocator;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ProportionalAllocatorTest {

    @Test
    void equalSplit_distributesRemainderToFirstBuckets() {
        long[] result = ProportionalAllocator.allocate(100, new long[]{1, 1, 1});
        assertArrayEquals(new long[]{34, 33, 33}, result);
        assertEquals(100, Arrays.stream(result).sum());
    }

    @Test
    void ratioSplit_isProportional() {
        long[] result = ProportionalAllocator.allocate(100, new long[]{1, 2, 1});
        assertArrayEquals(new long[]{25, 50, 25}, result);
    }

    @Test
    void alwaysSumsToTotal_evenWithAwkwardRemainders() {
        long[] result = ProportionalAllocator.allocate(10, new long[]{1, 1, 1});
        assertEquals(10, Arrays.stream(result).sum());
    }

    @Test
    void zeroWeightBucketGetsNothing() {
        long[] result = ProportionalAllocator.allocate(100, new long[]{0, 1, 1});
        assertArrayEquals(new long[]{0, 50, 50}, result);
    }

    @Test
    void allZeroWeights_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> ProportionalAllocator.allocate(100, new long[]{0, 0}));
    }
}
