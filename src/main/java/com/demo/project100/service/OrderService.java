package com.demo.project100.service;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Random;

import com.demo.project100.config.MyConfig;
import com.demo.project100.domain.OpenOrder;
import com.demo.project100.domain.SellType;
import com.demo.project100.domain.SettledOrder;
import com.demo.project100.repo.OpenOrderRepository;
import com.demo.project100.repo.SettledOrderRepository;
import com.demo.project100.repo.SettlementSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final SettledOrderRepository settledOrderRepository;
    private final SettlementSummaryRepository settlementSummaryRepository;
    private final OpenOrderRepository openOrderRepository;
    private final EventProcessor eventProcessor;
    private final ApplicationContext context;
    private final MyConfig myConfig;

    private static final String STOCK_TICKER_2 = "AMZN";

    /**
     * Save the order to db.
     * Then queue the order for settlement i.e find a matching order to complete it.
     */
    public OpenOrder placeOrder(OpenOrder orderItem, Boolean settle) {
        orderItem.setOrderDate(LocalDateTime.now());
        OpenOrder savedOrder = openOrderRepository.save(orderItem);
        if (settle) {
            orderItem.setSettle(true);
        }
        eventProcessor.queueOrder(savedOrder);
        return savedOrder;
    }

    /**
     * Get all the active orders from the db, to load them to in-memory data structure.
     * This can happen when system crashes and needs to restart
     */
    public Page<OpenOrder> findOpenOrdersForDay(Pageable pageable) {
        return openOrderRepository.findAllByOrderDateBetween(LocalDate.now().atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay(), pageable);
    }

    public Page<SettledOrder> findSettledOrdersForDay(Pageable pageable) {
        return settledOrderRepository.findAllByOrderDateBetween(LocalDate.now().atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay(), pageable);
    }

    public void reset() {
        log.info("Resetting!");
        settledOrderRepository.deleteAll();
        openOrderRepository.deleteAll();
        settlementSummaryRepository.deleteAll();
        myConfig.setCache(new HashMap<>());
    }

    public void simulationRandom(int records) {
        log.info("Random Simulation for: {}!", records);
        Random random = new Random();
        int minQty = 10;
        int maxQty = 100;
        double minPrice = 45.0;
        double maxPrice = 50.0;
        for (int i = 0; i < records; i++) {
            int quantity = random.nextInt(maxQty - minQty) + minQty;
            DecimalFormat df = new DecimalFormat("#.##");
            double price = Double.parseDouble(df.format((random.nextDouble(maxPrice - minPrice) + minPrice)));
            boolean sell = random.nextBoolean();
            OpenOrder orderItem;
            if (sell) {
                orderItem = OpenOrder.builder()
                        .ticker(STOCK_TICKER_2)
                        .price(price)
                        .quantity(quantity)
                        .type(SellType.SELL)
                        .build();
            } else {
                orderItem = OpenOrder.builder()
                        .ticker(STOCK_TICKER_2)
                        .price(price)
                        .quantity(quantity)
                        .type(SellType.BUY)
                        .build();
            }
            placeOrder(orderItem, true);
        }
    }

    /**
     * Same number of buy and sell order
     */
    public void simulateBalanced(int records) {
        log.info("Balanced Simulation for: {}!", records);
        for (int i = 0; i < records; i++) {
            OpenOrder orderItem = OpenOrder.builder()
                    .ticker(STOCK_TICKER_2)
                    .price(10.0)
                    .quantity(100)
                    .type(SellType.SELL)
                    .build();
            placeOrder(orderItem, true);
        }
        for (int i = 0; i < records; i++) {
            OpenOrder orderItem = OpenOrder.builder()
                    .ticker(STOCK_TICKER_2)
                    .price(10.0)
                    .quantity(100)
                    .type(SellType.BUY)
                    .build();
            placeOrder(orderItem, true);
        }
    }
}
