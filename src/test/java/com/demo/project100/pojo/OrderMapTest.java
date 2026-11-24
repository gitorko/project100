package com.demo.project100.pojo;

import com.demo.project100.domain.OpenOrder;
import com.demo.project100.domain.SellType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapTest {

    @Test
    void keepsPriceBucketHeadAndTailCorrectWhenOrdersAreRemovedAndAdded() {
        OrderMap orderMap = new OrderMap();
        OpenOrder first = order(1L, 100);
        OpenOrder second = order(2L, 200);
        OpenOrder third = order(3L, 300);
        OpenOrder fourth = order(4L, 400);

        orderMap.add(first);
        orderMap.add(second);
        orderMap.add(third);
        OrderChain firstNode = orderMap.getPriceMap().get(10.0);
        OrderChain secondNode = firstNode.getNext();
        OrderChain thirdNode = secondNode.getNext();

        orderMap.remove(firstNode);
        orderMap.add(fourth);

        assertThat(orderMap.getPriceMap().get(10.0)).isSameAs(secondNode);
        assertThat(secondNode.getPrevious()).isNull();
        assertThat(secondNode.getNext()).isSameAs(thirdNode);
        assertThat(thirdNode.getNext().getItem()).isSameAs(fourth);
        assertThat(orderMap.getLastNodeMap().get(10.0).getItem()).isSameAs(fourth);
    }

    @Test
    void findsAndRemovesTheIncomingOrderInsteadOfTheOldestOrderAtThatPrice() {
        OrderMap orderMap = new OrderMap();
        OpenOrder olderOpenOrder = order(1L, 100);
        OpenOrder incomingOrder = order(2L, 200);
        orderMap.add(olderOpenOrder);
        orderMap.add(incomingOrder);

        OrderChain incomingNode = orderMap.find(incomingOrder);
        orderMap.remove(incomingNode);

        assertThat(orderMap.getPriceMap().get(10.0).getItem()).isSameAs(olderOpenOrder);
        assertThat(orderMap.getLastNodeMap().get(10.0).getItem()).isSameAs(olderOpenOrder);
        assertThat(olderOpenOrder.getQuantity()).isEqualTo(100);
    }

    private OpenOrder order(Long id, Integer quantity) {
        return OpenOrder.builder()
                .id(id)
                .ticker("MYSTK")
                .price(10.0)
                .type(SellType.SELL)
                .quantity(quantity)
                .build();
    }
}
