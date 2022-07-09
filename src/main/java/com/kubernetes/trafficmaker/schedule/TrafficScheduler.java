package com.kubernetes.trafficmaker.schedule;

import com.kubernetes.trafficmaker.model.HttpTargetSpec;
import com.kubernetes.trafficmaker.model.TrafficTargetStatus.State;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@SuppressWarnings("ReactiveStreamsUnusedPublisher")
@Component
@RequiredArgsConstructor
@Slf4j
public class TrafficScheduler {

    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private final TaskScheduler taskScheduler;
    private final WebClient client;

    public boolean schedule(String taskName, HttpTargetSpec target, Trigger trigger, State currentTrafficState) {
        var httpRequest = target.toRequestMono(client);

        if (isScheduledTask(taskName)) {
            if (currentTrafficState == State.SCHEDULING) {
                updateTask(taskName, taskScheduler.schedule(httpRequest::subscribe, trigger));
            } else {
                log.error("Task {} is already scheduled task", taskName);
                return false;
            }
        } else {
            addTask(taskName, taskScheduler.schedule(httpRequest::subscribe, trigger));
        }
        return true;
    }

    private void addTask(String taskName, ScheduledFuture<?> schedule) {
        log.info("Task {} will be scheduled from now on", taskName);
        tasks.put(taskName, schedule);
    }

    private void updateTask(String taskName, ScheduledFuture<?> schedule) {
        log.info("Task {} scheduling is updated", taskName);
        tasks.computeIfPresent(taskName, (t, s) -> {
            s.cancel(true);
            return schedule;
        });
    }

    public void removeTask(String taskName) {
        tasks.computeIfPresent(taskName, (t, schedule) -> {
            schedule.cancel(true);
            return tasks.remove(t);
        });
    }

    public boolean isScheduledTask(String taskName) {
        return tasks.containsKey(taskName);
    }

}
