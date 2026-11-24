package com.demo.project100.service;

import java.util.ArrayList;
import java.util.List;

import com.demo.project100.domain.OpenOrder;
import com.demo.project100.pojo.OrderChain;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CombinationSumTest {

    private final CombinationSum combinationSum = new CombinationSum();

    @Test
    void keepsFirstDepthFirstCombination() {
        OrderChain head = chain(100, 200, 300);

        List<Integer> matchedQuantities = combinationSum.combinationSum(head, 400).stream()
                .map(node -> node.getItem().getQuantity())
                .toList();

        assertThat(matchedQuantities).containsExactly(100, 300);
    }

    @Test
    void returnsNoMatchWhenWholeOrderQuantitiesCannotReachTarget() {
        assertThat(combinationSum.combinationSum(chain(100, 200, 300), 50)).isEmpty();
    }

    @Test
    void handlesDeepOrderChainsWithoutRecursiveStackGrowth() {
        int orderCount = 5_000;
        List<Integer> quantities = new ArrayList<>(orderCount);
        for (int i = 0; i < orderCount; i++) {
            quantities.add(1);
        }

        List<OrderChain> match = combinationSum.combinationSum(chain(quantities), orderCount);

        assertThat(match).hasSize(orderCount);
    }

    private OrderChain chain(Integer... quantities) {
        return chain(List.of(quantities));
    }

    private OrderChain chain(List<Integer> quantities) {
        OrderChain head = null;
        OrderChain tail = null;
        for (Integer quantity : quantities) {
            OrderChain node = new OrderChain(OpenOrder.builder().quantity(quantity).build(), tail, null);
            if (tail == null) {
                head = node;
            } else {
                tail.setNext(node);
            }
            tail = node;
        }
        return head;
    }
}
