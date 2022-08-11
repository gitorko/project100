package com.demo.project100.service;

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

    /**
     * Different number of buy and sell orders
     */
    public void simulationRandom(int records) {
        log.info("Random Simulation for: {}!", records);
        Random random = new Random();
        for (int i = 0; i < records; i++) {
            boolean sell = random.nextBoolean();
            if (sell) {
                eventProcessor.simulationRandom(this, SellType.SELL);
            } else {
                eventProcessor.simulationRandom(this, SellType.BUY);
            }
        }
    }

    /**
     * Simulate orders
     */
    public void simulate(int records, SellType sellType) {
        log.info("Simulate for: {}!", records);
        for (int i = 0; i < records; i++) {
            eventProcessor.simulate(this, sellType);
        }
    }
}
