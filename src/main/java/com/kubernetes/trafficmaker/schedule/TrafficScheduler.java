package com.kubernetes.trafficmaker.schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
@RequiredArgsConstructor
public class TrafficScheduler {

    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private final TaskScheduler taskScheduler;

    public void add(String taskName, Runnable task, Duration period) {
        var  scheduledFuture = taskScheduler.scheduleAtFixedRate(task, period);
        tasks.put(taskName, scheduledFuture);
    }

    public void remove(String taskName) {
        tasks.get(taskName).cancel(true);
        tasks.remove(taskName);
    }

}
