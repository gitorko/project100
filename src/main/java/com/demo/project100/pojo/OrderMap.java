package com.demo.project100.pojo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

import com.demo.project100.domain.OpenOrder;
import lombok.Data;

@Data
public class OrderMap {
    private NavigableMap<Double, OrderChain> priceMap;
    private Map<Double, OrderChain> lastNodeMap;
    boolean reserveOrder;

    public OrderMap() {
        this.priceMap = new TreeMap<>();
        this.lastNodeMap = new HashMap<>();
    }

    public OrderMap(boolean reserveOrder) {
        if (reserveOrder) {
            this.priceMap = new TreeMap<>(Collections.reverseOrder());
            this.lastNodeMap = new HashMap<>();
        } else {
            this.priceMap = new TreeMap<>();
            this.lastNodeMap = new HashMap<>();
        }
    }

    /** Append an order to its price bucket, preserving FIFO within that price. */
    public void add(OpenOrder item) {
        Double price = item.getPrice();
        OrderChain tail = lastNodeMap.get(price);
        OrderChain node = new OrderChain(item, tail, null);

        if (tail == null) {
            priceMap.put(price, node);
        } else {
            tail.setNext(node);
        }
        lastNodeMap.put(price, node);
    }

    /** Find the exact order node, preferring identity and then the persisted id. */
    public OrderChain find(OpenOrder item) {
        for (OrderChain node = lastNodeMap.get(item.getPrice()); node != null; node = node.getPrevious()) {
            if (node.getItem() == item
                    || (item.getId() != null && Objects.equals(item.getId(), node.getItem().getId()))) {
                return node;
            }
        }
        return null;
    }

    /** Remove one order while keeping the price-bucket head and tail in sync. */
    public void remove(OrderChain node) {
        Double price = node.getItem().getPrice();
        OrderChain previous = node.getPrevious();
        OrderChain next = node.getNext();

        if (previous == null && next == null) {
            priceMap.remove(price);
            lastNodeMap.remove(price);
        } else {
            if (previous == null) {
                priceMap.put(price, next);
            } else {
                previous.setNext(next);
            }

            if (next == null) {
                lastNodeMap.put(price, previous);
            } else {
                next.setPrevious(previous);
            }
        }

        node.setPrevious(null);
        node.setNext(null);
    }
}
