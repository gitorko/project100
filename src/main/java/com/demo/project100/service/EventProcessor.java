package com.demo.project100.service;

import com.demo.project100.domain.OpenOrder;
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

}
