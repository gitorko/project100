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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ProcessEngineTest {

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
        OpenOrder order8 = createSell(9.0, 50, processEngine);
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

    private ProcessEngine getProcessEngine(String ticker) {
        ProcessEngine processEngine = (ProcessEngine) context.getBean("processEngine", ticker);
        processEngine.reset();
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

}
