package com.kubernetes.trafficmaker.schedule;

import com.kubernetes.trafficmaker.model.TrafficTarget;
import com.kubernetes.trafficmaker.model.TrafficTargetStatus.State;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrafficScheduler {

    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private final ThreadPoolTaskScheduler taskScheduler;
    private final WebClient client;

    public boolean schedule(TrafficTarget trafficTarget) {
        return this.schedule(trafficTarget.getName(), trafficTarget.getHttp().toRequestMono(client),
                             trafficTarget.getTrigger(), trafficTarget.getState());
    }

    public boolean schedule(String taskName, Mono<?> httpRequest, Trigger trigger, State currentTrafficState) {
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
        if (tasks.containsKey(taskName)) {
            tasks.get(taskName).cancel(true);
            addTask(taskName, schedule);
        }
    }

    public void removeTask(String taskName) {
        log.info("Task {} is deleted", taskName);
        if (tasks.containsKey(taskName)) {
            tasks.get(taskName).cancel(true);
            tasks.remove(taskName);
        }
    }

    public boolean isScheduledTask(String taskName) {
        return tasks.containsKey(taskName);
    }

    public long getActiveScheduleCount() {
        var taskCount = taskScheduler.getScheduledThreadPoolExecutor().getTaskCount();
        var completedTaskCount = taskScheduler.getScheduledThreadPoolExecutor().getCompletedTaskCount();
        return taskCount - completedTaskCount;
    }

}
