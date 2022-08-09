package com.demo.project100.config;

import java.util.HashMap;
import java.util.Map;

import com.demo.project100.service.ProcessEngine;
import lombok.Data;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@Data
public class MyConfig {

    Map<String, ProcessEngine> cache = new HashMap<>();

    /**
     * Each stock ticker gets its own ProcessEngine bean
     * Since data structure is per ProcessEngine there is less conflict from other stock ticker transactions
     * A single stock ticker transaction is isolated to the individual ProcessEngine
     *
     * ProcessEngine is created only once when a stock ticker comes into existence.
     */
    @Bean(autowireCandidate = false)
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public synchronized ProcessEngine processEngine(String ticker) {
        if (!cache.containsKey(ticker)) {
            ProcessEngine processEngine = new ProcessEngine(ticker);
            cache.put(ticker, processEngine);
        }
        return cache.get(ticker);
    }
}
