package com.voyage.expense.service;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.stream.IntStream;

/**
 * Splits an integer total across weighted buckets so the parts sum <b>exactly</b>
 * to the total (largest-remainder / Hamilton method). This keeps split amounts
 * and settlement balances free of rounding drift, so every net balance sums to 0.
 */
public final class ProportionalAllocator {

    private ProportionalAllocator() {
    }

    /**
     * @param total   non-negative amount to distribute (minor units)
     * @param weights per-bucket weights; at least one must be positive
     * @return allocations, same length as weights, summing to {@code total}
     */
    public static long[] allocate(long total, long[] weights) {
        if (total < 0) {
            throw new IllegalArgumentException("total must be non-negative");
        }
        long weightSum = 0;
        for (long w : weights) {
            if (w < 0) {
                throw new IllegalArgumentException("weights must be non-negative");
            }
            weightSum += w;
        }
        if (weightSum == 0) {
            throw new IllegalArgumentException("at least one weight must be positive");
        }

        long[] allocation = new long[weights.length];
        long[] remainderNumerator = new long[weights.length];
        BigInteger totalBig = BigInteger.valueOf(total);
        BigInteger weightSumBig = BigInteger.valueOf(weightSum);
        long allocated = 0;
        for (int i = 0; i < weights.length; i++) {
            BigInteger product = totalBig.multiply(BigInteger.valueOf(weights[i]));
            BigInteger[] divRem = product.divideAndRemainder(weightSumBig);
            allocation[i] = divRem[0].longValueExact();
            remainderNumerator[i] = divRem[1].longValueExact();
            allocated += allocation[i];
        }

        long leftover = total - allocated; // 0 .. weights.length-1
        // Give the +1 units to the largest fractional remainders (ties: lower index first).
        int[] order = IntStream.range(0, weights.length)
                .boxed()
                .sorted(Comparator.<Integer>comparingLong(i -> remainderNumerator[i]).reversed()
                        .thenComparingInt(i -> i))
                .mapToInt(Integer::intValue)
                .toArray();
        for (int k = 0; k < leftover; k++) {
            allocation[order[k]]++;
        }
        return allocation;
    }
}
