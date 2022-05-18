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

    public void addFixedRateSchedule(String taskName, Runnable task, Duration period) {
        tasks.put(taskName, taskScheduler.scheduleAtFixedRate(task, period));
    }

    public void updateFixedRateSchedule(String taskName, Runnable task, Duration period) {
        tasks.computeIfPresent(taskName, (t, schedule) -> {
            schedule.cancel(true);
            return taskScheduler.scheduleAtFixedRate(task, period);
        });
    }

    public void removeSchedule(String taskName) {
        if (isScheduledTask(taskName)) {
            tasks.remove(taskName).cancel(true);
        }
    }

    public boolean isScheduledTask(String taskName) {
        return tasks.containsKey(taskName);
    }

}
