package com.demo.project100.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.transaction.Transactional;

import com.demo.project100.domain.OpenOrder;
import com.demo.project100.domain.SellType;
import com.demo.project100.domain.SettledOrder;
import com.demo.project100.domain.SettlementSummary;
import com.demo.project100.domain.Status;
import com.demo.project100.pojo.NodeItem;
import com.demo.project100.repo.OpenOrderRepository;
import com.demo.project100.repo.SettledOrderRepository;
import com.demo.project100.repo.SettlementSummaryRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * No need of spring bean annotation, this is injected as a prototype spring bean
 */
@Slf4j
@Data
public class ProcessEngine {

    private String ticker;
    private Map<Double, NodeItem> sellPriceMap;
    private Map<Double, NodeItem> buyPriceMap;

    @Autowired
    private SettledOrderRepository settledOrderRepository;

    @Autowired
    private SettlementSummaryRepository settlementSummaryRepository;

    @Autowired
    private OpenOrderRepository openOrderRepository;

    public ProcessEngine(String ticker) {
        this.ticker = ticker;
        sellPriceMap = new TreeMap<>(Collections.reverseOrder());
        buyPriceMap = new TreeMap<>();
    }

    public void reset() {
        sellPriceMap = new TreeMap<>(Collections.reverseOrder());
        buyPriceMap = new TreeMap<>();
    }

    /**
     * Method is synchronized to avoid data structure corruption
     * Single thread of execution per stock ticker to ensure order fulfillment is accurate
     */
    public synchronized void build(OpenOrder orderItem) {
        Double key = orderItem.getPrice();
        if (orderItem.getType().equals(SellType.SELL)) {
            if (sellPriceMap.containsKey(key)) {
                NodeItem currNode = sellPriceMap.get(key).getCurr();
                NodeItem newNode = new NodeItem(orderItem, currNode, null);
                currNode.setNext(newNode);
                sellPriceMap.get(key).setCurr(newNode);
            } else {
                NodeItem currNode = new NodeItem(orderItem, null, null);
                currNode.setCurr(currNode);
                sellPriceMap.put(key, currNode);
            }
        } else {
            if (buyPriceMap.containsKey(key)) {
                NodeItem currNode = buyPriceMap.get(key).getCurr();
                NodeItem newNode = new NodeItem(orderItem, currNode, null);
                currNode.setNext(newNode);
                buyPriceMap.get(key).setCurr(newNode);
            } else {
                NodeItem currNode = new NodeItem(orderItem, null, null);
                currNode.setCurr(currNode);
                buyPriceMap.put(key, currNode);
            }
        }
    }

    /**
     * Method is synchronized to avoid data structure corruption
     * Single thread of execution per stock ticker to ensure order fulfillment is accurate
     */
    public synchronized boolean process(OpenOrder orderItem) {
        if (orderItem.getType().equals(SellType.BUY)) {
            return processOrder(orderItem, sellPriceMap, buyPriceMap);
        } else {
            return processOrder(orderItem, buyPriceMap, sellPriceMap);
        }
    }

    private boolean processOrder(OpenOrder orderItem, Map<Double, NodeItem> priceMap1, Map<Double, NodeItem> priceMap2) {
        List<NodeItem> nodeItems = new ArrayList<>();
        //Create list of all orders
        for (Map.Entry<Double, NodeItem> entry : priceMap1.entrySet()) {
            if (entry.getKey() <= orderItem.getPrice()) {
                NodeItem nodeHead = entry.getValue();
                while (nodeHead != null) {
                    nodeItems.add(nodeHead);
                    nodeHead = nodeHead.getNext();
                }
            }
        }
        List<NodeItem> resultNodeItems = new CombinationSum().combinationSum(nodeItems, orderItem.getQuantity());

        if (resultNodeItems.size() > 0) {

            //Clean the Map2
            NodeItem orderItemNode = priceMap2.get(orderItem.getPrice());
            if (orderItemNode != null) {
                if (orderItemNode.getPrevious() == null && orderItemNode.getNext() == null) {
                    //If its the only node then delete the map key
                    priceMap2.remove(orderItemNode.getItem().getPrice());
                } else if (orderItemNode.getPrevious() == null && orderItemNode.getNext() != null) {
                    //If its the first node then point head to next node.
                    NodeItem newHead = orderItemNode.getNext();
                    newHead.setPrevious(null);
                    orderItemNode.setNext(null);
                    priceMap2.put(newHead.getItem().getPrice(), newHead);
                    //Set the currNode
                    priceMap2.get(newHead.getItem().getPrice()).setCurr(newHead);
                } else if (orderItemNode.getPrevious() != null && orderItemNode.getNext() != null) {
                    //If node in middle, break both links
                    NodeItem newNext = orderItemNode.getNext();
                    NodeItem newPrevious = orderItemNode.getPrevious();
                    newPrevious.setNext(newNext);
                    newNext.setPrevious(newPrevious);
                    orderItemNode.setPrevious(null);
                    orderItemNode.setNext(null);
                } else if (orderItemNode.getPrevious() != null && orderItemNode.getNext() == null) {
                    //Last node
                    NodeItem previousNode = orderItemNode.getPrevious();
                    previousNode.setNext(null);
                    orderItemNode.setPrevious(null);
                    //Set the currNode
                    priceMap2.get(previousNode.getItem().getPrice()).setCurr(previousNode);
                }
            }

            //Break the links & clean Map1
            for (NodeItem nodeItem : resultNodeItems) {
                if (nodeItem.getPrevious() == null && nodeItem.getNext() == null) {
                    //If its the only node then delete the map key
                    priceMap1.remove(nodeItem.getItem().getPrice());
                } else if (nodeItem.getPrevious() == null && nodeItem.getNext() != null) {
                    //If its the first node then point head to next node.
                    NodeItem newHead = nodeItem.getNext();
                    newHead.setPrevious(null);
                    nodeItem.setNext(null);
                    priceMap1.put(newHead.getItem().getPrice(), newHead);
                    //Set the currNode
                    priceMap1.get(newHead.getItem().getPrice()).setCurr(newHead);
                } else if (nodeItem.getPrevious() != null && nodeItem.getNext() != null) {
                    //If node in middle, break both links
                    NodeItem newNext = nodeItem.getNext();
                    NodeItem newPrevious = nodeItem.getPrevious();
                    newPrevious.setNext(newNext);
                    newNext.setPrevious(newPrevious);
                    nodeItem.setPrevious(null);
                    nodeItem.setNext(null);
                } else if (nodeItem.getPrevious() != null && nodeItem.getNext() == null) {
                    //Last node
                    NodeItem previousNode = nodeItem.getPrevious();
                    previousNode.setNext(null);
                    nodeItem.setPrevious(null);
                    //Set the currNode
                    priceMap1.get(previousNode.getItem().getPrice()).setCurr(previousNode);
                }
            }

            List<OpenOrder> result = new ArrayList<>();
            for (NodeItem nodeItem : resultNodeItems) {
                result.add(nodeItem.getItem());
            }
            completeOrder(orderItem, result);
            return true;
        }
        return false;
    }

    @Transactional
    public void completeOrder(OpenOrder openOrder, List<OpenOrder> resultOrders) {
        List<SettledOrder> completeItems = new ArrayList<>();
        List<SettlementSummary> settlementSummaries = new ArrayList<>();
        List<Long> deleteOrderIds = new ArrayList<>();
        deleteOrderIds.add(openOrder.getId());

        SettledOrder settledOrder = SettledOrder.builder()
                .id(openOrder.getId())
                .ticker(openOrder.getTicker())
                .price(openOrder.getPrice())
                .type(openOrder.getType())
                .quantity(openOrder.getQuantity())
                .orderDate(openOrder.getOrderDate())
                .executedDate(LocalDateTime.now())
                .status(Status.COMPLETED)
                .build();
        completeItems.add(settledOrder);

        for (OpenOrder item : resultOrders) {
            deleteOrderIds.add(item.getId());
            SettledOrder localOrderItem = SettledOrder.builder()
                    .id(item.getId())
                    .ticker(item.getTicker())
                    .price(item.getPrice())
                    .type(item.getType())
                    .quantity(item.getQuantity())
                    .orderDate(item.getOrderDate())
                    .executedDate(LocalDateTime.now())
                    .status(Status.COMPLETED)
                    .build();
            completeItems.add(localOrderItem);
        }
        log.debug("Found Match {}", completeItems);

        if (settledOrder.getType().equals(SellType.BUY)) {
            for (SettledOrder item : completeItems) {
                if (!item.getType().equals(SellType.BUY)) {
                    //Its ok for seller to get above asking price but not go below the asking price.
                    settlementSummaries.add(SettlementSummary.builder()
                            .buyOrderId(settledOrder.getId())
                            .sellOrderId(item.getId())
                            .buyPrice(settledOrder.getPrice() * item.getQuantity())
                            .build());
                }
            }
        } else {
            for (SettledOrder item : completeItems) {
                if (!item.getType().equals(SellType.SELL)) {
                    //Its ok for buyer to get below asking price but not go above the asking price.
                    settlementSummaries.add(SettlementSummary.builder()
                            .buyOrderId(item.getId())
                            .sellOrderId(settledOrder.getId())
                            .buyPrice(item.getPrice() * settledOrder.getQuantity())
                            .build());
                }
            }
        }
        settledOrderRepository.saveAll(completeItems);
        settlementSummaryRepository.saveAll(settlementSummaries);
        openOrderRepository.deleteAllById(deleteOrderIds);
    }

}
