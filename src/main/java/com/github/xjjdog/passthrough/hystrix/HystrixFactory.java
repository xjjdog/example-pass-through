package com.github.xjjdog.passthrough.hystrix;

import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;

public class HystrixFactory {
    public void init() {
        HystrixConcurrencyStrategy delegate = HystrixPlugins.getInstance().getConcurrencyStrategy();
        if (delegate instanceof CommonHystrixConcurrencyStrategy) {
            return;
        }
        HystrixConcurrencyStrategy strategy = new CommonHystrixConcurrencyStrategy(delegate);

        HystrixCommandExecutionHook commandExecutionHook = HystrixPlugins
                .getInstance().getCommandExecutionHook();
        HystrixEventNotifier eventNotifier = HystrixPlugins.getInstance()
                .getEventNotifier();
        HystrixMetricsPublisher metricsPublisher = HystrixPlugins.getInstance()
                .getMetricsPublisher();
        HystrixPropertiesStrategy propertiesStrategy = HystrixPlugins.getInstance()
                .getPropertiesStrategy();

        HystrixPlugins.reset();//set all null

        HystrixPlugins.getInstance().registerConcurrencyStrategy(strategy);
        HystrixPlugins.getInstance()
                .registerCommandExecutionHook(commandExecutionHook);
        HystrixPlugins.getInstance().registerEventNotifier(eventNotifier);
        HystrixPlugins.getInstance().registerMetricsPublisher(metricsPublisher);
        HystrixPlugins.getInstance().registerPropertiesStrategy(propertiesStrategy);
    }
}
