package com.demo.project100.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.demo.project100.pojo.OrderChain;

/** Finds the first whole-order combination without using the call stack. */
public class CombinationSum {

    public List<OrderChain> combinationSum(OrderChain orderChain, int target) {
        if (orderChain == null) {
            return List.of();
        }

        List<OrderChain> orders = new ArrayList<>();
        for (OrderChain node = orderChain; node != null; node = node.getNext()) {
            orders.add(node);
        }
        return combinationSum(orders, target);
    }

    public List<OrderChain> combinationSum(List<OrderChain> orders, int target) {
        if (target <= 0 || orders.isEmpty()) {
            return List.of();
        }

        // Most simple matches use the earliest orders in sequence. Return
        // those without allocating the exhaustive-search state.
        List<OrderChain> prefixMatch = new ArrayList<>();
        int prefixRemaining = target;
        for (OrderChain node : orders) {
            int quantity = node.getItem().getQuantity();
            if (quantity > prefixRemaining) {
                break;
            }
            prefixMatch.add(node);
            prefixRemaining -= quantity;
            if (prefixRemaining == 0) {
                return prefixMatch;
            }
        }

        int size = orders.size();
        long[] suffixQuantity = new long[size + 1];
        for (int i = size - 1; i >= 0; i--) {
            suffixQuantity[i] = suffixQuantity[i + 1] + orders.get(i).getItem().getQuantity();
        }
        if (suffixQuantity[0] < target) {
            return List.of();
        }

        // Each depth stores the next order index to try. This mirrors the old
        // depth-first traversal, preserving its first matching combination.
        int[] nextIndex = new int[size + 1];
        int[] chosenIndex = new int[size];
        int[] remainingAtDepth = new int[size + 1];
        remainingAtDepth[0] = target;
        int depth = 0;
        Set<Long> failedStates = new HashSet<>();

        while (depth >= 0) {
            int remaining = remainingAtDepth[depth];
            if (remaining == 0) {
                List<OrderChain> result = new ArrayList<>(depth);
                for (int i = 0; i < depth; i++) {
                    result.add(orders.get(chosenIndex[i]));
                }
                return result;
            }

            int startIndex = depth == 0 ? 0 : chosenIndex[depth - 1] + 1;
            long state = (((long) startIndex) << 32) | (remaining & 0xffffffffL);
            if (failedStates.contains(state)) {
                depth--;
                continue;
            }

            int candidate = nextIndex[depth];
            if (candidate < startIndex) {
                candidate = startIndex;
            }
            while (candidate < size) {
                int quantity = orders.get(candidate).getItem().getQuantity();
                if (quantity <= remaining && suffixQuantity[candidate + 1] >= remaining - quantity) {
                    break;
                }
                candidate++;
            }

            if (candidate == size) {
                failedStates.add(state);
                nextIndex[depth] = startIndex;
                depth--;
                continue;
            }

            nextIndex[depth] = candidate + 1;
            chosenIndex[depth] = candidate;
            depth++;
            nextIndex[depth] = candidate + 1;
            remainingAtDepth[depth] = remaining - orders.get(candidate).getItem().getQuantity();
        }

        return List.of();
    }
}
