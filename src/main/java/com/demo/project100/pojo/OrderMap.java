package com.demo.project100.pojo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import lombok.Data;

@Data
public class OrderMap {
    private Map<Double, OrderChain> priceMap;
    private Map<Double, OrderChain> currMap;
    boolean reserveOrder;

    public OrderMap() {
        this.priceMap = new TreeMap<>();
        this.currMap = new HashMap<>();
    }

    public OrderMap(boolean reserveOrder) {
        if (reserveOrder) {
            this.priceMap = new TreeMap<>(Collections.reverseOrder());
            this.currMap = new HashMap<>();
        } else {
            this.priceMap = new TreeMap<>();
            this.currMap = new HashMap<>();
        }
    }
}
