package com.demo.project100.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.demo.project100.pojo.OrderChain;

public class CombinationSum {
    List<OrderChain> result;

    public List<OrderChain> combinationSum(OrderChain orderChain, int target) {
        this.result = new ArrayList<>();
        backtrack(orderChain, new ArrayList<>(), target);
        return result;
    }

    private void backtrack(OrderChain orderChain, List<OrderChain> tempList, int remain) {
        if (remain < 0 || result.size() > 0) {
            return;
        } else if (remain == 0) {
            result = new ArrayList<>(tempList);
        } else {
            while (orderChain != null) {
                tempList.add(orderChain);
                backtrack(orderChain.getNext(), tempList, remain - orderChain.getItem().getQuantity());
                tempList.remove(tempList.size() - 1);
                if (result.size() > 0) {
                    return;
                }
                orderChain = orderChain.getNext();
            }
        }
    }
}
