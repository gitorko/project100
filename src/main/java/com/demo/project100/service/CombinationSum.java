package com.demo.project100.service;

import java.util.ArrayList;
import java.util.List;

import com.demo.project100.pojo.NodeItem;

public class CombinationSum {
    List<NodeItem> result;
    List<NodeItem> nodeItems = new ArrayList<>();

    public List<NodeItem> combinationSum(List<NodeItem> candidates, int target) {
        result = new ArrayList<>();
        nodeItems = candidates;
        backtrack(new ArrayList<>(), target, 0);
        return result;
    }

    private void backtrack(List<NodeItem> tempList, int remain, int start) {
        if (remain < 0) {
            return;
        } else if (remain == 0) {
            result = new ArrayList<>(tempList);
        } else {
            for (int i = start; i < nodeItems.size(); i++) {
                tempList.add(nodeItems.get(i));
                backtrack(tempList, remain - nodeItems.get(i).getItem().getQuantity(), i + 1);
                tempList.remove(tempList.size() - 1);
                if (result.size() > 0) {
                    return;
                }
            }
        }
    }
}
