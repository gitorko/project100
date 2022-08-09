package com.demo.project100.pojo;

import java.util.PriorityQueue;

import com.demo.project100.domain.SettledOrder;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HeapOrderItem {
    SettledOrder orderItem;
    PriorityQueue<SettledOrder> pq;
}
