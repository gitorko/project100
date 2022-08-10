package com.demo.project100.pojo;

import com.demo.project100.domain.OpenOrder;
import lombok.Data;

@Data
public class OrderChain {
    OpenOrder item;
    OrderChain previous;
    OrderChain next;

    public OrderChain(OpenOrder item, OrderChain previous, OrderChain next) {
        this.item = item;
        this.previous = previous;
        this.next = next;
    }

    @Override
    public String toString() {
        return "NodeItem{" +
                "item=" + item +
                '}';
    }
}
