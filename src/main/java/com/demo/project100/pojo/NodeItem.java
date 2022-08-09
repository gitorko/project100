package com.demo.project100.pojo;

import com.demo.project100.domain.OpenOrder;
import lombok.Data;

@Data
public class NodeItem {
    OpenOrder item;
    NodeItem previous;
    NodeItem next;
    NodeItem curr;

    public NodeItem(OpenOrder item, NodeItem previous, NodeItem next) {
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
