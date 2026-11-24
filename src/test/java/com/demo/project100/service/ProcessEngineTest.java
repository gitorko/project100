package com.demo.project100.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.demo.project100.domain.OpenOrder;
import com.demo.project100.domain.SellType;
import com.demo.project100.domain.SettledOrder;
import com.demo.project100.domain.SettlementSummary;
import com.demo.project100.domain.Status;
import com.demo.project100.repo.OpenOrderRepository;
import com.demo.project100.repo.SettledOrderRepository;
import com.demo.project100.repo.SettlementSummaryRepository;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "stock-exchange.matching.mode=WHOLE_ORDER")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ProcessEngineTest extends BaseTest {

    private static final String STOCK_TICKER_1 = "MYSTK";

    @Autowired
    ApplicationContext context;

    @Autowired
    OpenOrderRepository openOrderRepository;

    @Autowired
    SettlementSummaryRepository settlementSummaryRepository;

    @Autowired
    SettledOrderRepository settledOrderRepository;

    @BeforeEach
    public void before() {
        settlementSummaryRepository.deleteAll();
        settlementSummaryRepository.deleteAll();
    }

    @SneakyThrows
    @Test
    public void test01_buy_samePriceBucket_sequentialTime() {
        //Setup
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder order1 = createSell(10.0, 100, processEngine);
        OpenOrder order2 = createSell(10.0, 200, processEngine);
        OpenOrder order3 = createSell(10.0, 300, processEngine);

        //Test
        OpenOrder executeOrder = createBuy(10.0, 300, processEngine);
        boolean status = processEngine.process(executeOrder);

        //Validate
        Assertions.assertThat(status).isTrue();
        SettledOrder order = settledOrderRepository.findById(executeOrder.getId()).get();
        Assertions.assertThat(order.getStatus()).isEqualTo(Status.COMPLETED);
        List<SettlementSummary> settlementSummary = settlementSummaryRepository.findByBuyOrderIdOrderBySellOrderId(executeOrder.getId());
        List<Long> soldTo = settlementSummary.stream().map(m -> m.getSellOrderId()).collect(Collectors.toList());
        Double salePrice = settlementSummary.stream().mapToDouble(m -> m.getSale()).sum();
        Assertions.assertThat(soldTo).isEqualTo(Arrays.asList(order1.getId(), order2.getId()));
        Assertions.assertThat(salePrice).isEqualTo(3000.0);
    }

    @SneakyThrows
    @Test
    public void test01_sell_samePriceBucket_sequentialTime() {
        //Setup
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder order1 = createBuy(10.0, 100, processEngine);
        OpenOrder order2 = createBuy(10.0, 200, processEngine);
        OpenOrder order3 = createBuy(10.0, 300, processEngine);

        //Test
        OpenOrder executeOrder = createSell(10.0, 300, processEngine);
        boolean status = processEngine.process(executeOrder);

        //Validate
        Assertions.assertThat(status).isTrue();
        SettledOrder order = settledOrderRepository.findById(executeOrder.getId()).get();
        Assertions.assertThat(order.getStatus()).isEqualTo(Status.COMPLETED);
        List<SettlementSummary> settlementSummary = settlementSummaryRepository.findBySellOrderIdOrderByBuyOrderId(executeOrder.getId());
        List<Long> soldTo = settlementSummary.stream().map(m -> m.getBuyOrderId()).collect(Collectors.toList());
        Double salePrice = settlementSummary.stream().mapToDouble(m -> m.getSale()).sum();
        Assertions.assertThat(soldTo).isEqualTo(Arrays.asList(order1.getId(), order2.getId()));
        Assertions.assertThat(salePrice).isEqualTo(3000.0);
    }

    @SneakyThrows
    @Test
    public void test02_buy_samePriceBucket_sequentialTime_preferenceToOrderFulfillment() {
        //Setup
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder order1 = createSell(10.0, 100, processEngine);
        OpenOrder order2 = createSell(10.0, 200, processEngine);
        OpenOrder order3 = createSell(10.0, 300, processEngine);

        //Test
        OpenOrder executeOrder = createBuy(10.0, 400, processEngine);
        boolean status = processEngine.process(executeOrder);

        //Validate
        Assertions.assertThat(status).isTrue();
        SettledOrder order = settledOrderRepository.findById(executeOrder.getId()).get();
        Assertions.assertThat(order.getStatus()).isEqualTo(Status.COMPLETED);
        List<SettlementSummary> settlementSummary = settlementSummaryRepository.findByBuyOrderIdOrderBySellOrderId(executeOrder.getId());
        List<Long> soldTo = settlementSummary.stream().map(m -> m.getSellOrderId()).collect(Collectors.toList());
        Double salePrice = settlementSummary.stream().mapToDouble(m -> m.getSale()).sum();
        Assertions.assertThat(soldTo).isEqualTo(Arrays.asList(order1.getId(), order3.getId()));
        Assertions.assertThat(salePrice).isEqualTo(4000.0);
        Assertions.assertThat(openOrderRepository.existsById(order2.getId())).isTrue();
        Assertions.assertThat(openOrderRepository.existsById(order1.getId())).isFalse();
        Assertions.assertThat(openOrderRepository.existsById(order3.getId())).isFalse();
        Assertions.assertThat(openOrderRepository.existsById(executeOrder.getId())).isFalse();
    }

    @SneakyThrows
    @Test
    public void test02_sell_samePriceBucket_sequentialTime_preferenceToOrderFulfillment() {
        //Setup
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder order1 = createBuy(10.0, 100, processEngine);
        OpenOrder order2 = createBuy(10.0, 200, processEngine);
        OpenOrder order3 = createBuy(10.0, 300, processEngine);

        //Test
        OpenOrder executeOrder = createSell(10.0, 400, processEngine);
        boolean status = processEngine.process(executeOrder);

        //Validate
        Assertions.assertThat(status).isTrue();
        SettledOrder order = settledOrderRepository.findById(executeOrder.getId()).get();
        Assertions.assertThat(order.getStatus()).isEqualTo(Status.COMPLETED);
        List<SettlementSummary> settlementSummary = settlementSummaryRepository.findBySellOrderIdOrderByBuyOrderId(executeOrder.getId());
        List<Long> soldTo = settlementSummary.stream().map(m -> m.getBuyOrderId()).collect(Collectors.toList());
        Double salePrice = settlementSummary.stream().mapToDouble(m -> m.getSale()).sum();
        Assertions.assertThat(soldTo).isEqualTo(Arrays.asList(order1.getId(), order3.getId()));
        Assertions.assertThat(salePrice).isEqualTo(4000.0);
    }

    @SneakyThrows
    @Test
    public void test03_buy_firstOrderTooSmallBlocksOther() {
        //Setup
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder order1 = createSell(10.0, 100, processEngine);
        OpenOrder order2 = createSell(10.0, 200, processEngine);
        OpenOrder order3 = createSell(10.0, 300, processEngine);

        //Test
        OpenOrder executeOrder = createBuy(10.0, 500, processEngine);
        boolean status = processEngine.process(executeOrder);

        //Validate
        Assertions.assertThat(status).isTrue();
        SettledOrder order = settledOrderRepository.findById(executeOrder.getId()).get();
        Assertions.assertThat(order.getStatus()).isEqualTo(Status.COMPLETED);
        List<SettlementSummary> settlementSummary = settlementSummaryRepository.findByBuyOrderIdOrderBySellOrderId(executeOrder.getId());
        List<Long> soldTo = settlementSummary.stream().map(m -> m.getSellOrderId()).collect(Collectors.toList());
        Double salePrice = settlementSummary.stream().mapToDouble(m -> m.getSale()).sum();
        Assertions.assertThat(soldTo).isEqualTo(Arrays.asList(order2.getId(), order3.getId()));
        Assertions.assertThat(salePrice).isEqualTo(5000.0);
    }


    @SneakyThrows
    @Test
    public void test04_buy_firstOrderTooBigBlocksOther() {
        //Setup
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder order1 = createSell(10.0, 10000, processEngine);
        OpenOrder order2 = createSell(10.0, 200, processEngine);
        OpenOrder order3 = createSell(10.0, 300, processEngine);

        //Test
        OpenOrder executeOrder = createBuy(10.0, 500, processEngine);
        boolean status = processEngine.process(executeOrder);

        //Validate
        Assertions.assertThat(status).isTrue();
        SettledOrder order = settledOrderRepository.findById(executeOrder.getId()).get();
        Assertions.assertThat(order.getStatus()).isEqualTo(Status.COMPLETED);
        List<SettlementSummary> settlementSummary = settlementSummaryRepository.findByBuyOrderIdOrderBySellOrderId(executeOrder.getId());
        List<Long> soldTo = settlementSummary.stream().map(m -> m.getSellOrderId()).collect(Collectors.toList());
        Double salePrice = settlementSummary.stream().mapToDouble(m -> m.getSale()).sum();
        Assertions.assertThat(soldTo).isEqualTo(Arrays.asList(order2.getId(), order3.getId()));
        Assertions.assertThat(salePrice).isEqualTo(5000.0);
    }

    @SneakyThrows
    @Test
    public void test05_buy_tooBigOrderInMiddleBlocksOther() {
        //Setup
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder order1 = createSell(10.0, 100, processEngine);
        OpenOrder order2 = createSell(10.0, 2000, processEngine);
        OpenOrder order3 = createSell(10.0, 300, processEngine);

        //Test
        OpenOrder executeOrder = createBuy(10.0, 400, processEngine);
        boolean status = processEngine.process(executeOrder);

        //Validate
        Assertions.assertThat(status).isTrue();
        SettledOrder order = settledOrderRepository.findById(executeOrder.getId()).get();
        Assertions.assertThat(order.getStatus()).isEqualTo(Status.COMPLETED);
        List<SettlementSummary> settlementSummary = settlementSummaryRepository.findByBuyOrderIdOrderBySellOrderId(executeOrder.getId());
        List<Long> soldTo = settlementSummary.stream().map(m -> m.getSellOrderId()).collect(Collectors.toList());
        Double salePrice = settlementSummary.stream().mapToDouble(m -> m.getSale()).sum();
        Assertions.assertThat(soldTo).isEqualTo(Arrays.asList(order1.getId(), order3.getId()));
        Assertions.assertThat(salePrice).isEqualTo(4000.0);
    }

    @SneakyThrows
    @Test
    public void test06_buy_differentPrice() {
        //Setup
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder order1 = createSell(10.0, 100, processEngine);
        OpenOrder order2 = createSell(10.0, 200, processEngine);
        OpenOrder order3 = createSell(9.0, 300, processEngine);

        //Test
        OpenOrder executeOrder = createBuy(10.0, 400, processEngine);
        boolean status = processEngine.process(executeOrder);

        //Validate
        Assertions.assertThat(status).isTrue();
        SettledOrder order = settledOrderRepository.findById(executeOrder.getId()).get();
        Assertions.assertThat(order.getStatus()).isEqualTo(Status.COMPLETED);
        List<SettlementSummary> settlementSummary = settlementSummaryRepository.findByBuyOrderIdOrderBySellOrderId(executeOrder.getId());
        List<Long> soldTo = settlementSummary.stream().map(m -> m.getSellOrderId()).collect(Collectors.toList());
        Double salePrice = settlementSummary.stream().mapToDouble(m -> m.getSale()).sum();
        Assertions.assertThat(soldTo).isEqualTo(Arrays.asList(order1.getId(), order3.getId()));
        Assertions.assertThat(salePrice).isEqualTo(3700.0);
    }

    @SneakyThrows
    @Test
    public void test07_buy_differentPrice() {
        //Setup
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder order1 = createSell(10.0, 100, processEngine);
        OpenOrder order2 = createSell(9.0, 200, processEngine);
        OpenOrder order3 = createSell(8.0, 300, processEngine);

        //Test
        OpenOrder executeOrder = createBuy(10.0, 400, processEngine);
        boolean status = processEngine.process(executeOrder);

        //Validate
        Assertions.assertThat(status).isTrue();
        SettledOrder order = settledOrderRepository.findById(executeOrder.getId()).get();
        Assertions.assertThat(order.getStatus()).isEqualTo(Status.COMPLETED);
        List<SettlementSummary> settlementSummary = settlementSummaryRepository.findByBuyOrderIdOrderBySellOrderId(executeOrder.getId());
        List<Long> soldTo = settlementSummary.stream().map(m -> m.getSellOrderId()).collect(Collectors.toList());
        Double salePrice = settlementSummary.stream().mapToDouble(m -> m.getSale()).sum();
        Assertions.assertThat(soldTo).isEqualTo(Arrays.asList(order1.getId(), order3.getId()));
        Assertions.assertThat(salePrice).isEqualTo(3400.0);
    }

    @SneakyThrows
    @Test
    public void test08_buy_differentPrice_timePreference() {
        //Setup
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder order1 = createSell(10.0, 100, processEngine);
        OpenOrder order2 = createSell(9.0, 200, processEngine);
        OpenOrder order3 = createSell(8.0, 300, processEngine);

        //Test
        OpenOrder executeOrder = createBuy(10.0, 300, processEngine);
        boolean status = processEngine.process(executeOrder);

        //Validate
        Assertions.assertThat(status).isTrue();
        SettledOrder order = settledOrderRepository.findById(executeOrder.getId()).get();
        Assertions.assertThat(order.getStatus()).isEqualTo(Status.COMPLETED);
        List<SettlementSummary> settlementSummary = settlementSummaryRepository.findByBuyOrderIdOrderBySellOrderId(executeOrder.getId());
        List<Long> soldTo = settlementSummary.stream().map(m -> m.getSellOrderId()).collect(Collectors.toList());
        Double salePrice = settlementSummary.stream().mapToDouble(m -> m.getSale()).sum();
        Assertions.assertThat(soldTo).isEqualTo(Arrays.asList(order1.getId(), order2.getId()));
        Assertions.assertThat(salePrice).isEqualTo(2800.0);
    }

    @SneakyThrows
    @Test
    public void test09_buy_orderCantBeFulfilled() {
        //Setup
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder order1 = createSell(10.0, 100, processEngine);
        OpenOrder order2 = createSell(10.0, 200, processEngine);
        OpenOrder order3 = createSell(10.0, 300, processEngine);

        //Test
        OpenOrder executeOrder = createBuy(10.0, 50, processEngine);
        boolean status = processEngine.process(executeOrder);

        //Validate
        Assertions.assertThat(status).isFalse();
        Optional<SettledOrder> order = settledOrderRepository.findById(executeOrder.getId());
        Assertions.assertThat(order).isEmpty();
        //Ensure the un-settled item makes it into the data structure for future processing.
        List<SettlementSummary> settlementSummary = settlementSummaryRepository.findByBuyOrderIdOrderBySellOrderId(executeOrder.getId());
        Assertions.assertThat(settlementSummary.size()).isEqualTo(0);
    }

    @SneakyThrows
    @Test
    public void test10_buy_samePrice_sameTime() {
        //Setup
        LocalDateTime now = LocalDateTime.now();
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder order1 = createSell(10.0, 200, now, processEngine);
        OpenOrder order2 = createSell(10.0, 100, now, processEngine);
        OpenOrder order3 = createSell(10.0, 200, now, processEngine);

        //Test
        OpenOrder executeOrder = createBuy(10.0, 400, processEngine);
        boolean status = processEngine.process(executeOrder);

        //Validate
        Assertions.assertThat(status).isTrue();
        SettledOrder order = settledOrderRepository.findById(executeOrder.getId()).get();
        Assertions.assertThat(order.getStatus()).isEqualTo(Status.COMPLETED);
        List<SettlementSummary> settlementSummary = settlementSummaryRepository.findByBuyOrderIdOrderBySellOrderId(executeOrder.getId());
        List<Long> soldTo = settlementSummary.stream().map(m -> m.getSellOrderId()).collect(Collectors.toList());
        Double salePrice = settlementSummary.stream().mapToDouble(m -> m.getSale()).sum();
        Assertions.assertThat(soldTo).isEqualTo(Arrays.asList(order1.getId(), order3.getId()));
        Assertions.assertThat(salePrice).isEqualTo(4000.0);
    }

    @SneakyThrows
    @Test
    public void test11_buy_differentPrice_sameTime() {
        //Setup
        LocalDateTime now = LocalDateTime.now();
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder order1 = createSell(10.0, 200, now, processEngine);
        OpenOrder order2 = createSell(9.0, 100, now, processEngine);
        OpenOrder order3 = createSell(8.0, 200, now, processEngine);

        //Test
        OpenOrder executeOrder = createBuy(10.0, 400, processEngine);
        boolean status = processEngine.process(executeOrder);

        //Validate
        Assertions.assertThat(status).isTrue();
        SettledOrder order = settledOrderRepository.findById(executeOrder.getId()).get();
        Assertions.assertThat(order.getStatus()).isEqualTo(Status.COMPLETED);
        List<SettlementSummary> settlementSummary = settlementSummaryRepository.findByBuyOrderIdOrderBySellOrderId(executeOrder.getId());
        List<Long> soldTo = settlementSummary.stream().map(m -> m.getSellOrderId()).collect(Collectors.toList());
        Double salePrice = settlementSummary.stream().mapToDouble(m -> m.getSale()).sum();
        Assertions.assertThat(soldTo).isEqualTo(Arrays.asList(order1.getId(), order3.getId()));
        Assertions.assertThat(salePrice).isEqualTo(3600.0);
    }

    @SneakyThrows
    @Test
    public void test12_buy_matchInMiddle() {
        //Setup
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder order1 = createSell(10.0, 100, processEngine);
        OpenOrder order2 = createSell(10.0, 200, processEngine);
        OpenOrder order3 = createSell(10.0, 300, processEngine);

        //Test
        OpenOrder executeOrder = createBuy(10.0, 200, processEngine);
        boolean status = processEngine.process(executeOrder);

        //Validate
        Assertions.assertThat(status).isTrue();
        SettledOrder order = settledOrderRepository.findById(executeOrder.getId()).get();
        Assertions.assertThat(order.getStatus()).isEqualTo(Status.COMPLETED);
        List<SettlementSummary> settlementSummary = settlementSummaryRepository.findByBuyOrderIdOrderBySellOrderId(executeOrder.getId());
        List<Long> soldTo = settlementSummary.stream().map(m -> m.getSellOrderId()).collect(Collectors.toList());
        Double salePrice = settlementSummary.stream().mapToDouble(m -> m.getSale()).sum();
        Assertions.assertThat(soldTo).isEqualTo(Arrays.asList(order2.getId()));
        Assertions.assertThat(salePrice).isEqualTo(2000.0);
    }

    @SneakyThrows
    @Test
    public void test13_lotOfOrders() {
        //Setup
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder order1 = createSell(8.0, 5, processEngine);
        OpenOrder order2 = createSell(10.0, 1, processEngine);
        OpenOrder order3 = createSell(10.0, 1, processEngine);
        OpenOrder order4 = createSell(10.0, 5, processEngine);
        OpenOrder order5 = createSell(10.0, 100, processEngine);
        OpenOrder order6 = createSell(10.0, 1000, processEngine);
        OpenOrder order7 = createSell(9.0, 200, processEngine);
        OpenOrder order8 = createSell(9.0, 500, processEngine);
        OpenOrder order9 = createSell(8.0, 300, processEngine);

        //Test
        OpenOrder executeOrder = createBuy(10.0, 505, processEngine);
        boolean status = processEngine.process(executeOrder);

        //Validate
        Assertions.assertThat(status).isTrue();
        SettledOrder order = settledOrderRepository.findById(executeOrder.getId()).get();
        Assertions.assertThat(order.getStatus()).isEqualTo(Status.COMPLETED);
        List<SettlementSummary> settlementSummary = settlementSummaryRepository.findByBuyOrderIdOrderBySellOrderId(executeOrder.getId());
        List<Long> soldTo = settlementSummary.stream().map(m -> m.getSellOrderId()).collect(Collectors.toList());
        Double salePrice = settlementSummary.stream().mapToDouble(m -> m.getSale()).sum();
        Assertions.assertThat(soldTo).isEqualTo(Arrays.asList(order4.getId(), order7.getId(), order9.getId()));
        Assertions.assertThat(salePrice).isEqualTo(4250.0);
    }

    @Test
    public void test14_laterSellMatchesRestingBuyAtTheSamePrice() {
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder restingBuy = createBuy(10.0, 200, processEngine);
        OpenOrder incomingSell = createSell(10.0, 200, processEngine);

        boolean matched = processEngine.process(incomingSell);

        Assertions.assertThat(matched).isTrue();
        Assertions.assertThat(settledOrderRepository.findById(restingBuy.getId())).isPresent();
        Assertions.assertThat(settledOrderRepository.findById(incomingSell.getId())).isPresent();
        Assertions.assertThat(openOrderRepository.existsById(restingBuy.getId())).isFalse();
        Assertions.assertThat(openOrderRepository.existsById(incomingSell.getId())).isFalse();
        List<SettlementSummary> summaries = settlementSummaryRepository
                .findBySellOrderIdOrderByBuyOrderId(incomingSell.getId());
        Assertions.assertThat(summaries).hasSize(1);
        Assertions.assertThat(summaries.get(0).getBuyOrderId()).isEqualTo(restingBuy.getId());
        Assertions.assertThat(summaries.get(0).getQuantity()).isEqualTo(200);
    }

    @Test
    public void test15_noPriceOverlapLeavesOrdersOpenThenLaterSellMatchesBuy() {
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder expensiveSell = createSell(10.0, 100, processEngine);
        OpenOrder restingBuy = createBuy(9.0, 100, processEngine);

        Assertions.assertThat(processEngine.process(restingBuy)).isFalse();
        Assertions.assertThat(openOrderRepository.existsById(expensiveSell.getId())).isTrue();
        Assertions.assertThat(openOrderRepository.existsById(restingBuy.getId())).isTrue();

        OpenOrder incomingSell = createSell(9.0, 100, processEngine);
        Assertions.assertThat(processEngine.process(incomingSell)).isTrue();

        Assertions.assertThat(settledOrderRepository.findById(restingBuy.getId())).isPresent();
        Assertions.assertThat(settledOrderRepository.findById(incomingSell.getId())).isPresent();
        Assertions.assertThat(openOrderRepository.existsById(expensiveSell.getId())).isTrue();
        Assertions.assertThat(openOrderRepository.existsById(restingBuy.getId())).isFalse();
        Assertions.assertThat(openOrderRepository.existsById(incomingSell.getId())).isFalse();
    }

    @Test
    public void test16_unselectedOrdersStayOpenForLaterOrders() {
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder firstSell = createSell(10.0, 100, processEngine);
        OpenOrder unselectedSell = createSell(10.0, 200, processEngine);
        OpenOrder thirdSell = createSell(10.0, 300, processEngine);
        OpenOrder incomingBuy = createBuy(10.0, 400, processEngine);

        Assertions.assertThat(processEngine.process(incomingBuy)).isTrue();

        Assertions.assertThat(openOrderRepository.existsById(unselectedSell.getId())).isTrue();
        Assertions.assertThat(openOrderRepository.existsById(firstSell.getId())).isFalse();
        Assertions.assertThat(openOrderRepository.existsById(thirdSell.getId())).isFalse();
        Assertions.assertThat(openOrderRepository.existsById(incomingBuy.getId())).isFalse();

        OpenOrder laterBuy = createBuy(10.0, 200, processEngine);
        Assertions.assertThat(processEngine.process(laterBuy)).isTrue();
        Assertions.assertThat(settledOrderRepository.findById(unselectedSell.getId())).isPresent();
        Assertions.assertThat(settledOrderRepository.findById(laterBuy.getId())).isPresent();
    }

    @Test
    public void test17_acceptanceOrderBreaksSameTimestampAndPriceTie() {
        LocalDateTime acceptedAt = LocalDateTime.now();
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder acceptedFirst = createSell(10.0, 100, acceptedAt, processEngine);
        OpenOrder acceptedSecond = createSell(10.0, 200, acceptedAt, processEngine);
        OpenOrder incomingBuy = createBuy(10.0, 100, processEngine);

        Assertions.assertThat(processEngine.process(incomingBuy)).isTrue();

        List<SettlementSummary> summaries = settlementSummaryRepository
                .findByBuyOrderIdOrderBySellOrderId(incomingBuy.getId());
        Assertions.assertThat(summaries).hasSize(1);
        Assertions.assertThat(summaries.get(0).getSellOrderId()).isEqualTo(acceptedFirst.getId());
        Assertions.assertThat(openOrderRepository.existsById(acceptedSecond.getId())).isTrue();
        Assertions.assertThat(openOrderRepository.existsById(acceptedFirst.getId())).isFalse();
    }

    @Test
    public void test18_ordersForDifferentTickersDoNotMatch() {
        ProcessEngine abcEngine = getProcessEngine("ABC");
        ProcessEngine xyzEngine = getProcessEngine("XYZ");
        OpenOrder abcSell = createTickerOrder("ABC", SellType.SELL, 10.0, 100, abcEngine);
        OpenOrder xyzBuy = createTickerOrder("XYZ", SellType.BUY, 10.0, 100, xyzEngine);

        Assertions.assertThat(xyzEngine.process(xyzBuy)).isFalse();
        Assertions.assertThat(openOrderRepository.existsById(abcSell.getId())).isTrue();
        Assertions.assertThat(openOrderRepository.existsById(xyzBuy.getId())).isTrue();

        OpenOrder abcBuy = createTickerOrder("ABC", SellType.BUY, 10.0, 100, abcEngine);
        Assertions.assertThat(abcEngine.process(abcBuy)).isTrue();
        Assertions.assertThat(settledOrderRepository.findById(abcSell.getId())).isPresent();
        Assertions.assertThat(openOrderRepository.existsById(xyzBuy.getId())).isTrue();
    }

    @Test
    public void test19_settlementUsesEachMatchedSellersAskingPrice() {
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder tenDollarSell = createSell(10.0, 100, processEngine);
        OpenOrder nineDollarSell = createSell(9.0, 100, processEngine);
        OpenOrder incomingBuy = createBuy(10.0, 200, processEngine);

        Assertions.assertThat(processEngine.process(incomingBuy)).isTrue();

        List<SettlementSummary> summaries = settlementSummaryRepository
                .findByBuyOrderIdOrderBySellOrderId(incomingBuy.getId());
        Assertions.assertThat(summaries).hasSize(2);
        Assertions.assertThat(summaries).extracting(SettlementSummary::getSellOrderId)
                .containsExactly(tenDollarSell.getId(), nineDollarSell.getId());
        Assertions.assertThat(summaries).extracting(SettlementSummary::getPrice)
                .containsExactly(10.0, 9.0);
        Assertions.assertThat(summaries).extracting(SettlementSummary::getSale)
                .containsExactly(1000.0, 900.0);
    }

    @Test
    public void test20_orderCannotBePartiallyFilled() {
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder sell = createSell(10.0, 200, processEngine);
        OpenOrder buy = createBuy(10.0, 100, processEngine);

        Assertions.assertThat(processEngine.process(buy)).isFalse();

        Assertions.assertThat(openOrderRepository.existsById(sell.getId())).isTrue();
        Assertions.assertThat(openOrderRepository.existsById(buy.getId())).isTrue();
        Assertions.assertThat(settledOrderRepository.findById(sell.getId())).isEmpty();
        Assertions.assertThat(settledOrderRepository.findById(buy.getId())).isEmpty();
        Assertions.assertThat(settlementSummaryRepository
                .findByBuyOrderIdOrderBySellOrderId(buy.getId())).isEmpty();
    }

    @Test
    public void test21_sellSkipsFirstBuyThatIsTooSmall() {
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder firstBuy = createBuy(10.0, 100, processEngine);
        OpenOrder secondBuy = createBuy(10.0, 200, processEngine);
        OpenOrder thirdBuy = createBuy(10.0, 300, processEngine);
        OpenOrder incomingSell = createSell(10.0, 500, processEngine);

        Assertions.assertThat(processEngine.process(incomingSell)).isTrue();

        List<SettlementSummary> summaries = settlementSummaryRepository
                .findBySellOrderIdOrderByBuyOrderId(incomingSell.getId());
        Assertions.assertThat(summaries).extracting(SettlementSummary::getBuyOrderId)
                .containsExactly(secondBuy.getId(), thirdBuy.getId());
        Assertions.assertThat(openOrderRepository.existsById(firstBuy.getId())).isTrue();
    }

    @Test
    public void test22_sellSkipsFirstBuyThatIsTooLarge() {
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder firstBuy = createBuy(10.0, 10000, processEngine);
        OpenOrder secondBuy = createBuy(10.0, 200, processEngine);
        OpenOrder thirdBuy = createBuy(10.0, 300, processEngine);
        OpenOrder incomingSell = createSell(10.0, 500, processEngine);

        Assertions.assertThat(processEngine.process(incomingSell)).isTrue();

        List<SettlementSummary> summaries = settlementSummaryRepository
                .findBySellOrderIdOrderByBuyOrderId(incomingSell.getId());
        Assertions.assertThat(summaries).extracting(SettlementSummary::getBuyOrderId)
                .containsExactly(secondBuy.getId(), thirdBuy.getId());
        Assertions.assertThat(openOrderRepository.existsById(firstBuy.getId())).isTrue();
    }

    @Test
    public void test23_sellSkipsOversizedMiddleBuy() {
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder firstBuy = createBuy(10.0, 100, processEngine);
        OpenOrder oversizedBuy = createBuy(10.0, 2000, processEngine);
        OpenOrder thirdBuy = createBuy(10.0, 300, processEngine);
        OpenOrder incomingSell = createSell(10.0, 400, processEngine);

        Assertions.assertThat(processEngine.process(incomingSell)).isTrue();

        List<SettlementSummary> summaries = settlementSummaryRepository
                .findBySellOrderIdOrderByBuyOrderId(incomingSell.getId());
        Assertions.assertThat(summaries).extracting(SettlementSummary::getBuyOrderId)
                .containsExactly(firstBuy.getId(), thirdBuy.getId());
        Assertions.assertThat(openOrderRepository.existsById(oversizedBuy.getId())).isTrue();
    }

    @Test
    public void test24_sellCanUseWholeOrdersAcrossEligibleBidPrices() {
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder nineDollarBuy = createBuy(9.0, 100, processEngine);
        OpenOrder tenDollarBuy = createBuy(10.0, 200, processEngine);
        OpenOrder incomingSell = createSell(9.0, 300, processEngine);

        Assertions.assertThat(processEngine.process(incomingSell)).isTrue();

        List<SettlementSummary> summaries = settlementSummaryRepository
                .findBySellOrderIdOrderByBuyOrderId(incomingSell.getId());
        Assertions.assertThat(summaries).extracting(SettlementSummary::getBuyOrderId)
                .containsExactly(nineDollarBuy.getId(), tenDollarBuy.getId());
        Assertions.assertThat(summaries).extracting(SettlementSummary::getPrice)
                .containsExactly(9.0, 9.0);
        Assertions.assertThat(summaries).extracting(SettlementSummary::getSale)
                .containsExactly(900.0, 1800.0);
    }

    @Test
    public void test25_buyMatchesEligibleLowerAskWhenNoExactPriceBucketExists() {
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder lowerAsk = createSell(9.0, 100, processEngine);
        OpenOrder incomingBuy = createBuy(10.0, 100, processEngine);

        Assertions.assertThat(processEngine.process(incomingBuy)).isTrue();

        List<SettlementSummary> summaries = settlementSummaryRepository
                .findByBuyOrderIdOrderBySellOrderId(incomingBuy.getId());
        Assertions.assertThat(summaries).hasSize(1);
        Assertions.assertThat(summaries.get(0).getSellOrderId()).isEqualTo(lowerAsk.getId());
        Assertions.assertThat(summaries.get(0).getPrice()).isEqualTo(9.0);
    }

    @Test
    public void test26_sellMatchesEligibleHigherBidWhenNoExactPriceBucketExists() {
        ProcessEngine processEngine = getProcessEngine(STOCK_TICKER_1);
        OpenOrder higherBid = createBuy(10.0, 100, processEngine);
        OpenOrder incomingSell = createSell(9.0, 100, processEngine);

        Assertions.assertThat(processEngine.process(incomingSell)).isTrue();

        List<SettlementSummary> summaries = settlementSummaryRepository
                .findBySellOrderIdOrderByBuyOrderId(incomingSell.getId());
        Assertions.assertThat(summaries).hasSize(1);
        Assertions.assertThat(summaries.get(0).getBuyOrderId()).isEqualTo(higherBid.getId());
        Assertions.assertThat(summaries.get(0).getPrice()).isEqualTo(9.0);
    }

    private ProcessEngine getProcessEngine(String ticker) {
        ProcessEngine processEngine = (ProcessEngine) context.getBean("processEngine", ticker);
        processEngine.reset();
        Assertions.assertThat(processEngine.getMatchingMode()).isEqualTo(MatchingMode.WHOLE_ORDER);
        return processEngine;
    }

    @SneakyThrows
    private OpenOrder createSell(Double price, Integer quantity, ProcessEngine processEngine) {
        OpenOrder order = openOrderRepository.save(OpenOrder.builder().price(price).quantity(quantity).type(SellType.SELL).ticker(STOCK_TICKER_1).orderDate(LocalDateTime.now()).build());
        processEngine.build(order);
        TimeUnit.NANOSECONDS.sleep(10);
        return order;
    }

    @SneakyThrows
    private OpenOrder createBuy(Double price, Integer quantity, ProcessEngine processEngine) {
        OpenOrder order = openOrderRepository.save(OpenOrder.builder().price(price).quantity(quantity).type(SellType.BUY).ticker(STOCK_TICKER_1).orderDate(LocalDateTime.now()).build());
        processEngine.build(order);
        TimeUnit.NANOSECONDS.sleep(10);
        return order;
    }

    @SneakyThrows
    private OpenOrder createSell(Double price, Integer quantity, LocalDateTime localDateTime, ProcessEngine processEngine) {
        OpenOrder order = openOrderRepository.save(OpenOrder.builder().price(price).quantity(quantity).type(SellType.SELL).ticker(STOCK_TICKER_1).orderDate(localDateTime).build());
        processEngine.build(order);
        return order;
    }

    @SneakyThrows
    private OpenOrder createBuy(Double price, Integer quantity, LocalDateTime localDateTime, ProcessEngine processEngine) {
        OpenOrder order = openOrderRepository.save(OpenOrder.builder().price(price).quantity(quantity).type(SellType.BUY).ticker(STOCK_TICKER_1).orderDate(localDateTime).build());
        processEngine.build(order);
        return order;
    }

    private OpenOrder createTickerOrder(String ticker, SellType type, Double price, Integer quantity,
                                        ProcessEngine processEngine) {
        OpenOrder order = openOrderRepository.save(OpenOrder.builder()
                .ticker(ticker)
                .price(price)
                .quantity(quantity)
                .type(type)
                .orderDate(LocalDateTime.now())
                .build());
        processEngine.build(order);
        return order;
    }

}
