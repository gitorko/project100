package com.demo.project100.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import jakarta.transaction.Transactional;

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

    private static final CombinationSum COMBINATION_SUM = new CombinationSum();

    //Unbounded blocking queue, will take as many orders as permitted by memory.
    private BlockingQueue<OpenOrder> orderQueue = new LinkedBlockingDeque<>();
    private volatile boolean running;

    private String ticker;
    private MatchingMode matchingMode = MatchingMode.WHOLE_ORDER;
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

    public ProcessEngine(String ticker, String matchingMode) {
        this(ticker);
        this.matchingMode = MatchingMode.valueOf(matchingMode.trim().toUpperCase());
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
        OrderMap orderMap = orderItem.getType().equals(SellType.SELL) ? sellMap : buyMap;
        orderMap.add(orderItem);
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
        if (matchingMode == MatchingMode.PARTIAL) {
            return processPartialOrder(orderItem, orderMap1, orderMap2);
        }

        OrderChain startingNode = orderMap1.getPriceMap().get(orderItem.getPrice());
        List<OrderChain> resultOrderChains = startingNode == null
                ? List.of()
                : COMBINATION_SUM.combinationSum(startingNode, orderItem.getQuantity());
        if (resultOrderChains.isEmpty()) {
            // Search only prices after the starting bucket in the book's
            // configured order. Build a view of candidates instead of
            // temporarily rewiring the live FIFO chains.
            List<OrderChain> candidateOrders = new ArrayList<>();
            for (OrderChain priceLevelHead : orderMap1.getPriceMap()
                    .tailMap(orderItem.getPrice(), true).values()) {
                for (OrderChain node = priceLevelHead; node != null; node = node.getNext()) {
                    candidateOrders.add(node);
                }
            }
            resultOrderChains = COMBINATION_SUM.combinationSum(candidateOrders, orderItem.getQuantity());
        }

        if (resultOrderChains.size() > 0) {
            OrderChain incomingNode = orderMap2.find(orderItem);
            if (incomingNode != null) {
                orderMap2.remove(incomingNode);
            }

            for (OrderChain orderChain : resultOrderChains) {
                orderMap1.remove(orderChain);
            }

            List<OpenOrder> result = new ArrayList<>(resultOrderChains.size());
            for (OrderChain orderChain : resultOrderChains) {
                result.add(orderChain.getItem());
            }
            completeOrder(orderItem, result, sellType);
            return true;
        }
        return false;
    }

    /** Match in conventional best-price then arrival order, allowing residual quantities. */
    private boolean processPartialOrder(OpenOrder incomingOrder, OrderMap restingBook, OrderMap incomingBook) {
        OrderChain incomingNode = incomingBook.find(incomingOrder);
        List<OrderChain> candidates = new ArrayList<>();
        for (OrderChain priceLevelHead : restingBook.getPriceMap()
                .tailMap(incomingOrder.getPrice(), true).values()) {
            for (OrderChain node = priceLevelHead; node != null; node = node.getNext()) {
                candidates.add(node);
            }
        }

        // The legacy books use comparators that preserve the original whole-order
        // examples. Partial matching uses the standard best ask / best bid order.
        Comparator<OrderChain> bestPriceFirst = incomingOrder.getType() == SellType.BUY
                ? Comparator.comparingDouble(node -> node.getItem().getPrice())
                : Comparator.comparingDouble((OrderChain node) -> node.getItem().getPrice()).reversed();
        candidates.sort(bestPriceFirst); // List.sort is stable, retaining FIFO at equal prices.

        int incomingRemaining = incomingOrder.getQuantity();
        List<Fill> fills = new ArrayList<>();
        List<OrderChain> completedRestingNodes = new ArrayList<>();
        List<OpenOrder> partiallyFilledRestingOrders = new ArrayList<>();
        for (OrderChain candidate : candidates) {
            if (incomingRemaining == 0) {
                break;
            }

            OpenOrder restingOrder = candidate.getItem();
            int fillQuantity = Math.min(incomingRemaining, restingOrder.getQuantity());
            if (restingOrder.getOriginalQuantity() == null) {
                restingOrder.setOriginalQuantity(restingOrder.getQuantity());
            }
            if (incomingOrder.getOriginalQuantity() == null) {
                incomingOrder.setOriginalQuantity(incomingOrder.getQuantity());
            }

            incomingRemaining -= fillQuantity;
            incomingOrder.setQuantity(incomingRemaining);
            restingOrder.setQuantity(restingOrder.getQuantity() - fillQuantity);
            fills.add(new Fill(restingOrder, fillQuantity));
            if (restingOrder.getQuantity() == 0) {
                completedRestingNodes.add(candidate);
            } else {
                partiallyFilledRestingOrders.add(restingOrder);
            }
        }

        if (fills.isEmpty()) {
            return false;
        }

        LocalDateTime executedDate = LocalDateTime.now();
        List<SettlementSummary> summaries = new ArrayList<>(fills.size());
        for (Fill fill : fills) {
            OpenOrder restingOrder = fill.restingOrder();
            OpenOrder buyOrder = incomingOrder.getType() == SellType.BUY ? incomingOrder : restingOrder;
            OpenOrder sellOrder = incomingOrder.getType() == SellType.SELL ? incomingOrder : restingOrder;
            summaries.add(SettlementSummary.builder()
                    .buyOrderId(buyOrder.getId())
                    .sellOrderId(sellOrder.getId())
                    .price(sellOrder.getPrice())
                    .quantity(fill.quantity())
                    .sale(sellOrder.getPrice() * fill.quantity())
                    .build());
        }

        List<SettledOrder> completedOrders = new ArrayList<>();
        List<Long> completedIds = new ArrayList<>();
        List<OpenOrder> changedOpenOrders = new ArrayList<>();
        for (OrderChain node : completedRestingNodes) {
            OpenOrder order = node.getItem();
            completedOrders.add(toSettledOrder(order, executedDate));
            completedIds.add(order.getId());
        }
        for (OrderChain node : completedRestingNodes) {
            restingBook.remove(node);
        }

        if (incomingRemaining == 0) {
            completedOrders.add(toSettledOrder(incomingOrder, executedDate));
            completedIds.add(incomingOrder.getId());
            if (incomingNode != null) {
                incomingBook.remove(incomingNode);
            }
        } else {
            changedOpenOrders.add(incomingOrder);
        }
        changedOpenOrders.addAll(partiallyFilledRestingOrders);

        settledOrderRepository.saveAll(completedOrders);
        settlementSummaryRepository.saveAll(summaries);
        openOrderRepository.saveAll(changedOpenOrders);
        openOrderRepository.deleteAllById(completedIds);
        return true;
    }

    private SettledOrder toSettledOrder(OpenOrder order, LocalDateTime executedDate) {
        return SettledOrder.builder()
                .id(order.getId())
                .ticker(order.getTicker())
                .price(order.getPrice())
                .type(order.getType())
                .quantity(order.getOriginalQuantity() == null ? order.getQuantity() : order.getOriginalQuantity())
                .orderDate(order.getOrderDate())
                .executedDate(executedDate)
                .status(Status.COMPLETED)
                .build();
    }

    private record Fill(OpenOrder restingOrder, int quantity) { }

    @Transactional
    public void completeOrder(OpenOrder openOrder, List<OpenOrder> resultOrders, SellType sellType) {
        List<SettledOrder> completeItems = new ArrayList<>(resultOrders.size() + 1);
        List<SettlementSummary> settlementSummaries = new ArrayList<>(resultOrders.size());
        List<Long> deleteOrderIds = new ArrayList<>(resultOrders.size() + 1);
        deleteOrderIds.add(openOrder.getId());
        LocalDateTime executedDate = LocalDateTime.now();

        SettledOrder settledOrder = SettledOrder.builder()
                .id(openOrder.getId())
                .ticker(openOrder.getTicker())
                .price(openOrder.getPrice())
                .type(openOrder.getType())
                .quantity(openOrder.getQuantity())
                .orderDate(openOrder.getOrderDate())
                .executedDate(executedDate)
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
                    .executedDate(executedDate)
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
