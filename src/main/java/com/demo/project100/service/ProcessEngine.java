package com.demo.project100.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import javax.transaction.Transactional;

import com.demo.project100.domain.OpenOrder;
import com.demo.project100.domain.SellType;
import com.demo.project100.domain.SettledOrder;
import com.demo.project100.domain.SettlementSummary;
import com.demo.project100.domain.Status;
import com.demo.project100.pojo.OrderChain;
import com.demo.project100.pojo.OrderMap;
import com.demo.project100.repo.OpenOrderRepository;
import com.demo.project100.repo.SettledOrderRepository;
import com.demo.project100.repo.SettlementSummaryRepository;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * No need of spring bean annotation, this is injected as a prototype spring bean
 */
@Slf4j
@Data
public class ProcessEngine {

    //Unbounded blocking queue, will take as many orders as permitted by memory.
    private BlockingQueue<OpenOrder> orderQueue = new LinkedBlockingDeque<>();
    private volatile boolean running;

    private String ticker;
    private OrderMap sellMap;
    private OrderMap buyMap;

    @Autowired
    private SettledOrderRepository settledOrderRepository;

    @Autowired
    private SettlementSummaryRepository settlementSummaryRepository;

    @Autowired
    private OpenOrderRepository openOrderRepository;

    @SneakyThrows
    public void startProcessing() {
        //Double check locking to avoid running thread more than once.
        if (!running) {
            synchronized (this) {
                if (!running) {
                    running = true;
                    while (true) {
                        OpenOrder orderItem = orderQueue.take();
                        log.info("Processing order {}", orderItem);
                        build(orderItem);
                        if (orderItem.isSettle()) {
                            //Triggers the matching process to find the relevant match order
                            boolean status = process(orderItem);
                            log.info("Status of order: {}, {}", orderItem.getId(), status);
                        }
                    }
                }
            }
        }
    }

    public ProcessEngine(String ticker) {
        this.ticker = ticker;
        sellMap = new OrderMap(true);
        buyMap = new OrderMap();
    }

    public synchronized void reset() {
        sellMap = new OrderMap(true);
        buyMap = new OrderMap();
    }

    /**
     * Method is not synchronized as its a single thread execution model.
     * If its multi-thread then there will be data structure corruption
     * Single thread of execution per stock ticker to ensure order fulfillment is accurate.
     */
    public void build(OpenOrder orderItem) {
        Double key = orderItem.getPrice();
        if (orderItem.getType().equals(SellType.SELL)) {
            OrderChain newNode;
            if (sellMap.getPriceMap().containsKey(key)) {
                //already exists
                OrderChain currNode = sellMap.getCurrMap().get(key);
                newNode = new OrderChain(orderItem, currNode, null);
                currNode.setNext(newNode);
                sellMap.getCurrMap().put(key, newNode);
            } else {
                //New node
                newNode = new OrderChain(orderItem, null, null);
                sellMap.getCurrMap().put(key, newNode);
                sellMap.getPriceMap().put(key, newNode);
            }
        } else {
            OrderChain newNode;
            if (buyMap.getPriceMap().containsKey(key)) {
                //already exists
                OrderChain currNode = buyMap.getCurrMap().get(key);
                newNode = new OrderChain(orderItem, currNode, null);
                currNode.setNext(newNode);
                buyMap.getCurrMap().put(key, newNode);
            } else {
                //New node
                newNode = new OrderChain(orderItem, null, null);
                buyMap.getCurrMap().put(key, newNode);
                buyMap.getPriceMap().put(key, newNode);
            }
        }
    }

    /**
     * Method is not synchronized as its a single thread execution model.
     * If its multi-thread then there will be data structure corruption
     * Single thread of execution per stock ticker to ensure order fulfillment is accurate.
     */
    public boolean process(OpenOrder orderItem) {
        if (orderItem.getType().equals(SellType.BUY)) {
            return processOrder(orderItem, sellMap, buyMap, SellType.BUY);
        } else {
            return processOrder(orderItem, buyMap, sellMap, SellType.SELL);
        }
    }

    private boolean processOrder(OpenOrder orderItem, OrderMap orderMap1, OrderMap orderMap2, SellType sellType) {
        List<OrderChain> resultOrderChains = new ArrayList<>();
        if (orderMap1.getPriceMap().size() > 0) {
            //Short circuit and link all nodes in one long continuous chain.
            List<OrderChain> revertList = new ArrayList<>();

            OrderChain previous = null;
            for (Map.Entry<Double, OrderChain> entry : orderMap1.getPriceMap().entrySet()) {
                if (previous != null) {
                    revertList.add(previous);
                    previous.setNext(orderMap1.getPriceMap().get(entry.getKey()));
                }
                if (entry.getKey() <= orderItem.getPrice()) {
                    previous = orderMap1.getCurrMap().get(entry.getKey());
                }
            }

            //Find if order can be fulfilled
            resultOrderChains = new CombinationSum().combinationSum(orderMap1.getPriceMap().get(orderItem.getPrice()), orderItem.getQuantity());

            //Reset the short circuiting.
            for (OrderChain revertItem : revertList) {
                revertItem.setNext(null);
            }
        }

        if (resultOrderChains.size() > 0) {

            //Clean the Map2
            OrderChain orderItemNode = orderMap2.getPriceMap().get(orderItem.getPrice());
            if (orderItemNode != null) {
                if (orderItemNode.getPrevious() == null && orderItemNode.getNext() == null) {
                    //If its the only node then delete the map key
                    orderMap2.getPriceMap().remove(orderItemNode.getItem().getPrice());
                    orderMap2.getCurrMap().remove(orderItemNode.getItem().getPrice());
                } else if (orderItemNode.getPrevious() == null && orderItemNode.getNext() != null) {
                    //If its the first node then point head to next node.
                    OrderChain newHead = orderItemNode.getNext();
                    newHead.setPrevious(null);
                    orderItemNode.setNext(null);
                    orderMap2.getPriceMap().put(newHead.getItem().getPrice(), newHead);
                    //Set the currNode
                    orderMap2.getCurrMap().put(newHead.getItem().getPrice(), newHead);
                } else if (orderItemNode.getPrevious() != null && orderItemNode.getNext() != null) {
                    //If node in middle, break both links
                    OrderChain newNext = orderItemNode.getNext();
                    OrderChain newPrevious = orderItemNode.getPrevious();
                    newPrevious.setNext(newNext);
                    newNext.setPrevious(newPrevious);
                    orderItemNode.setPrevious(null);
                    orderItemNode.setNext(null);
                } else if (orderItemNode.getPrevious() != null && orderItemNode.getNext() == null) {
                    //Last node
                    OrderChain previousNode = orderItemNode.getPrevious();
                    previousNode.setNext(null);
                    orderItemNode.setPrevious(null);
                    //Set the currNode
                    orderMap2.getCurrMap().put(previousNode.getItem().getPrice(), previousNode);
                }
            }

            //Break the links & clean Map1
            for (OrderChain orderChain : resultOrderChains) {
                if (orderChain.getPrevious() == null && orderChain.getNext() == null) {
                    //If its the only node then delete the map key
                    orderMap1.getPriceMap().remove(orderChain.getItem().getPrice());
                    orderMap1.getCurrMap().remove(orderChain.getItem().getPrice());
                } else if (orderChain.getPrevious() == null && orderChain.getNext() != null) {
                    //If its the first node then point head to next node.
                    OrderChain newHead = orderChain.getNext();
                    newHead.setPrevious(null);
                    orderChain.setNext(null);
                    orderMap1.getPriceMap().put(newHead.getItem().getPrice(), newHead);
                    //Set the currNode
                    orderMap1.getCurrMap().put(newHead.getItem().getPrice(), newHead);
                } else if (orderChain.getPrevious() != null && orderChain.getNext() != null) {
                    //If node in middle, break both links
                    OrderChain newNext = orderChain.getNext();
                    OrderChain newPrevious = orderChain.getPrevious();
                    newPrevious.setNext(newNext);
                    newNext.setPrevious(newPrevious);
                    orderChain.setPrevious(null);
                    orderChain.setNext(null);
                } else if (orderChain.getPrevious() != null && orderChain.getNext() == null) {
                    //Last node
                    OrderChain previousNode = orderChain.getPrevious();
                    previousNode.setNext(null);
                    orderChain.setPrevious(null);
                    //Set the currNode
                    orderMap1.getCurrMap().put(previousNode.getItem().getPrice(), previousNode);
                }
            }

            List<OpenOrder> result = new ArrayList<>();
            for (OrderChain orderChain : resultOrderChains) {
                result.add(orderChain.getItem());
            }
            completeOrder(orderItem, result, sellType);
            return true;
        }
        return false;
    }

    @Transactional
    public void completeOrder(OpenOrder openOrder, List<OpenOrder> resultOrders, SellType sellType) {
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
                            .price(item.getPrice())
                            .quantity(item.getQuantity())
                            .sale(item.getPrice() * item.getQuantity())
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
                            .price(settledOrder.getPrice())
                            .quantity(item.getQuantity())
                            .sale(settledOrder.getPrice() * item.getQuantity())
                            .build());
                }
            }
        }
        settledOrderRepository.saveAll(completeItems);
        settlementSummaryRepository.saveAll(settlementSummaries);
        openOrderRepository.deleteAllById(deleteOrderIds);
    }

}
