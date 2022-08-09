package com.demo.project100.config;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import com.demo.project100.domain.OpenOrder;
import com.demo.project100.domain.SellType;
import com.demo.project100.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeedData implements CommandLineRunner {

    private final OrderService orderService;
    private static final String STOCK_TICKER_1 = "GOOGL";

    /**
     * Load the orders to the datastructures in case of system crash and restart.
     * It only loads the orders but doesnt trigger a settlement
     */
    @Override
    public void run(String... args) throws Exception {
        //In case of a server restart the data structure needs to be build again.
        Page<OpenOrder> allTodayOrder = orderService.findOpenOrdersForDay(Pageable.unpaged());
        for (OpenOrder orderItem : allTodayOrder.getContent()) {
            log.info("Seeding order {} from db", orderItem);
            orderService.placeOrder(orderItem, false);
        }

        //In an empty database, seed test data.
        if (allTodayOrder.getTotalElements() == 0) {
            log.info("Seeding pre-market opening orders!");
            orderService.placeOrder(OpenOrder.builder().price(10.0).quantity(100).type(SellType.SELL).ticker(STOCK_TICKER_1).orderDate(LocalDateTime.now()).build(), false);
            TimeUnit.SECONDS.sleep(1);
            orderService.placeOrder(OpenOrder.builder().price(10.0).quantity(200).type(SellType.SELL).ticker(STOCK_TICKER_1).orderDate(LocalDateTime.now()).build(), false);
            TimeUnit.SECONDS.sleep(1);
            orderService.placeOrder(OpenOrder.builder().price(10.0).quantity(300).type(SellType.SELL).ticker(STOCK_TICKER_1).orderDate(LocalDateTime.now()).build(), false);
        }

    }
}
