package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulingConfig {
    @Bean(name = "linkUpdateCheckExecutorService", destroyMethod = "shutdown")
    public ExecutorService linkUpdateCheckExecutorService(SchedulerProperties schedulerProperties) {
        int parallelism = Math.max(1, schedulerProperties.getLinkCheckParallelism());
        AtomicInteger threadCounter = new AtomicInteger(1);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("link-update-check-" + threadCounter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };

        return Executors.newFixedThreadPool(parallelism, threadFactory);
    }
}
