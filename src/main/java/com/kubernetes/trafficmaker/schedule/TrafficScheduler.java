package com.kubernetes.trafficmaker.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrafficScheduler {

    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private final TaskScheduler taskScheduler;

    public boolean addFixedRateSchedule(String taskName, Runnable task, Duration period) {
        if (isScheduled(taskName)) {
            log.warn("Task {} is already scheduled.", taskName);
            return false;
        }

        tasks.put(taskName, taskScheduler.scheduleAtFixedRate(task, period));
        return true;
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
