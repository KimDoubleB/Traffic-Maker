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
        if (isScheduled(taskName)) {
            throw new AlreadyScheduledException(taskName);
        }

        tasks.put(taskName, taskScheduler.scheduleAtFixedRate(task, period));
    }

    public void remove(String taskName) {
        if (isScheduled(taskName)) {
            tasks.remove(taskName).cancel(true);
        }
    }

    private boolean isScheduled(String taskName) {
        return tasks.containsKey(taskName);
    }

}
