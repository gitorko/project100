package com.demo.project100.service;

import java.time.LocalDateTime;
import java.util.List;

import com.demo.project100.domain.OpenOrder;
import com.demo.project100.domain.SellType;
import com.demo.project100.domain.SettlementSummary;
import com.demo.project100.repo.OpenOrderRepository;
import com.demo.project100.repo.SettledOrderRepository;
import com.demo.project100.repo.SettlementSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "stock-exchange.matching.mode=PARTIAL")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
class PartialMatchingTest extends BaseTest {

    private static final String TICKER = "PARTIAL";

    @Autowired
    private ApplicationContext context;

    @Autowired
    private OpenOrderRepository openOrderRepository;

    @Autowired
    private SettledOrderRepository settledOrderRepository;

    @Autowired
    private SettlementSummaryRepository settlementSummaryRepository;

    private ProcessEngine engine;

    @BeforeEach
    void setUp() {
        settlementSummaryRepository.deleteAll();
        settledOrderRepository.deleteAll();
        openOrderRepository.deleteAll();
        engine = (ProcessEngine) context.getBean("processEngine", TICKER);
        engine.reset();
        assertThat(engine.getMatchingMode()).isEqualTo(MatchingMode.PARTIAL);
    }

    @Test
    void partiallyFillsAtBestPriceAndKeepsResidualOpenForLaterFill() {
        OpenOrder betterAsk = addOrder(SellType.SELL, 9.0, 50);
        OpenOrder nextAsk = addOrder(SellType.SELL, 10.0, 100);
        OpenOrder incomingBuy = addOrder(SellType.BUY, 10.0, 120);

        assertThat(engine.process(incomingBuy)).isTrue();

        List<SettlementSummary> firstFills = settlementSummaryRepository
                .findByBuyOrderIdOrderBySellOrderId(incomingBuy.getId());
        assertThat(firstFills).hasSize(2);
        assertThat(firstFills).extracting(SettlementSummary::getSellOrderId)
                .containsExactly(betterAsk.getId(), nextAsk.getId());
        assertThat(firstFills).extracting(SettlementSummary::getQuantity).containsExactly(50, 70);
        assertThat(firstFills).extracting(SettlementSummary::getPrice).containsExactly(9.0, 10.0);
        assertThat(settledOrderRepository.findById(betterAsk.getId())).isPresent();
        assertThat(settledOrderRepository.findById(incomingBuy.getId())).isPresent();
        assertThat(settledOrderRepository.findById(incomingBuy.getId()).orElseThrow().getQuantity()).isEqualTo(120);
        assertThat(openOrderRepository.findById(nextAsk.getId()).orElseThrow().getQuantity()).isEqualTo(30);

        OpenOrder laterBuy = addOrder(SellType.BUY, 10.0, 30);
        assertThat(engine.process(laterBuy)).isTrue();

        assertThat(openOrderRepository.existsById(nextAsk.getId())).isFalse();
        assertThat(settledOrderRepository.findById(nextAsk.getId()).orElseThrow().getQuantity()).isEqualTo(100);
    }

    @Test
    void partiallyFilledIncomingOrderRemainsOpenAndCanCompleteLater() {
        OpenOrder firstAsk = addOrder(SellType.SELL, 10.0, 60);
        OpenOrder incomingBuy = addOrder(SellType.BUY, 10.0, 100);

        assertThat(engine.process(incomingBuy)).isTrue();
        assertThat(openOrderRepository.findById(incomingBuy.getId()).orElseThrow().getQuantity()).isEqualTo(40);
        assertThat(openOrderRepository.existsById(firstAsk.getId())).isFalse();
        assertThat(settledOrderRepository.findById(incomingBuy.getId())).isEmpty();

        OpenOrder secondAsk = addOrder(SellType.SELL, 10.0, 40);
        assertThat(engine.process(secondAsk)).isTrue();

        assertThat(openOrderRepository.existsById(incomingBuy.getId())).isFalse();
        assertThat(settledOrderRepository.findById(incomingBuy.getId()).orElseThrow().getQuantity()).isEqualTo(100);
        List<SettlementSummary> fills = settlementSummaryRepository
                .findByBuyOrderIdOrderBySellOrderId(incomingBuy.getId());
        assertThat(fills).hasSize(2);
        assertThat(fills).extracting(SettlementSummary::getQuantity).containsExactly(60, 40);
    }

    @Test
    void incomingSellUsesHighestEligibleBidFirstAndMayPartiallyFillTheNextBid() {
        OpenOrder higherBid = addOrder(SellType.BUY, 10.0, 30);
        OpenOrder lowerBid = addOrder(SellType.BUY, 9.0, 50);
        OpenOrder incomingSell = addOrder(SellType.SELL, 9.0, 60);

        assertThat(engine.process(incomingSell)).isTrue();

        List<SettlementSummary> fills = settlementSummaryRepository
                .findBySellOrderIdOrderByBuyOrderId(incomingSell.getId());
        assertThat(fills).hasSize(2);
        assertThat(fills).extracting(SettlementSummary::getBuyOrderId)
                .containsExactly(higherBid.getId(), lowerBid.getId());
        assertThat(fills).extracting(SettlementSummary::getQuantity).containsExactly(30, 30);
        assertThat(fills).extracting(SettlementSummary::getPrice).containsExactly(9.0, 9.0);
        assertThat(openOrderRepository.findById(lowerBid.getId()).orElseThrow().getQuantity()).isEqualTo(20);
        assertThat(settledOrderRepository.findById(incomingSell.getId())).isPresent();
    }

    private OpenOrder addOrder(SellType type, double price, int quantity) {
        OpenOrder order = openOrderRepository.save(OpenOrder.builder()
                .ticker(TICKER)
                .price(price)
                .type(type)
                .quantity(quantity)
                .orderDate(LocalDateTime.now())
                .build());
        engine.build(order);
        return order;
    }
}
