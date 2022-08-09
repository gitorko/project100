package com.demo.project100.service;

import com.demo.project100.domain.OpenOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

@Service
@EnableAsync
@RequiredArgsConstructor
@Slf4j
public class EventProcessor {

    private final ApplicationContext context;

    /**
     * Processing happens in async thread
     */
    @Async
    public void processOrder(OpenOrder orderItem) {
        log.debug("Received event {}", orderItem);
        //Get the bean associated with the stock ticker
        ProcessEngine processEngine = (ProcessEngine) context.getBean("processEngine", orderItem.getTicker());
        processEngine.build(orderItem);
        if (orderItem.isSettle()) {
            //Triggers the matching process to find the relevant match order
            boolean status = processEngine.process(orderItem);
            log.info("Status of order: {}, {}", orderItem.getId(), status);
        }
    }

}
