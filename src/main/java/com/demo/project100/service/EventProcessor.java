package com.demo.project100.service;

import java.text.DecimalFormat;
import java.util.Random;

import com.demo.project100.domain.OpenOrder;
import com.demo.project100.domain.SellType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableAsync
public class EventProcessor {

    private static final String STOCK_TICKER_2 = "AMZN";

    private final ApplicationContext context;

    /**
     * There is only a single thread that runs per ticker.
     * Each ticker is getting its own dedicated single thread for processing.
     *
     * eg:
     * GOOGL: async-thread-1 permanently running and processing.
     * AMZN: async-thread-2 permanently running and processing.
     *
     * Thread pool size determine how many tickers are supported in each instance.
     *
     * Implement custom thread pool to increase pool size.
     *
     * Dont change to multi-thread as it will cause issues when threads context switch and dont honor order time.
     */
    @Async
    public void queueOrder(OpenOrder orderItem) {
        log.debug("Queuing order {}", orderItem);
        //Get the bean associated with the stock ticker
        ProcessEngine processEngine = (ProcessEngine) context.getBean("processEngine", orderItem.getTicker());
        processEngine.getOrderQueue().offer(orderItem);
        if (orderItem.isSettle() && !processEngine.isRunning()) {
            //This starts the async thread
            processEngine.startProcessing();
        }
    }

    /**
     * Different number of buy and sell orders
     */
    @Async
    public void simulationRandom(OrderService orderService, SellType sellType) {
        Random random = new Random();
        int minQty = 10;
        int maxQty = 100;
        double minPrice = 45.0;
        double maxPrice = 50.0;
        int quantity = random.nextInt(maxQty - minQty) + minQty;
        DecimalFormat df = new DecimalFormat("#.##");
        double price = Double.parseDouble(df.format((random.nextDouble(maxPrice - minPrice) + minPrice)));
        OpenOrder orderItem;

        orderItem = OpenOrder.builder()
                .ticker(STOCK_TICKER_2)
                .price(price)
                .quantity(quantity)
                .type(sellType)
                .build();

        orderService.placeOrder(orderItem, true);
    }

    /**
     * Simulate orders
     */
    @Async
    public void simulate(OrderService orderService, SellType sellType) {
        OpenOrder orderItem = OpenOrder.builder()
                .ticker(STOCK_TICKER_2)
                .price(10.0)
                .quantity(100)
                .type(sellType)
                .build();
        orderService.placeOrder(orderItem, true);
    }

}
