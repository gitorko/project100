package com.demo.project100.controller;

import com.demo.project100.domain.OpenOrder;
import com.demo.project100.domain.SellType;
import com.demo.project100.domain.SettledOrder;
import com.demo.project100.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/api/order")
    public OpenOrder placeOrder(@RequestBody OpenOrder orderItem) {
        return orderService.placeOrder(orderItem, true);
    }

    @GetMapping("/api/open-order")
    public Page<OpenOrder> getOpen(Pageable pageable) {
        return orderService.findOpenOrdersForDay(pageable);
    }

    @GetMapping("/api/settled-order")
    public Page<SettledOrder> getSettled(Pageable pageable) {
        return orderService.findSettledOrdersForDay(pageable);
    }

    @DeleteMapping("/api/order")
    public void reset() {
        orderService.reset();
    }

    @GetMapping("/api/simulate-random/{records}")
    public void simulationRandom(@PathVariable int records) {
        orderService.simulationRandom(records);
    }

    @GetMapping("/api/simulate-sell/{records}")
    public void simulateSell(@PathVariable int records) {
        orderService.simulate(records, SellType.SELL);
    }

    @GetMapping("/api/simulate-buy/{records}")
    public void simulateBuy(@PathVariable int records) {
        orderService.simulate(records, SellType.BUY);
    }
}
