package com.kubernetes.trafficmaker.schedule;

import com.kubernetes.trafficmaker.target.TrafficTargetSpec;
import com.kubernetes.trafficmaker.target.TrafficTargetStatus.State;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.web.reactive.function.client.WebClient;

@SuppressWarnings("ReactiveStreamsUnusedPublisher")
@Slf4j
public class TrafficScheduleTask {

    private final String name;
    private final TrafficTargetSpec target;
    private final WebClient client;

    private TrafficScheduleTask(String name, TrafficTargetSpec target, WebClient client) {
        this.name = name;
        this.target = target;
        this.client = client;
    }

    public static TrafficScheduleTask of(String name, TrafficTargetSpec target, WebClient client) {
        return new TrafficScheduleTask(name, target, client);
    }

    public State register(TrafficScheduler trafficScheduler, State currentTrafficState) {
        var httpRequestMono = target.http().toRequestMono(client);
        var period = DurationStyle.detectAndParse(target.rate());

        if (trafficScheduler.isScheduledTask(name)) {
            if (currentTrafficState == State.SCHEDULING) {
                log.info("Task {} scheduling is updated", name);
                trafficScheduler.updateFixedRateSchedule(name, httpRequestMono::subscribe, period);
            } else {
                log.error("Task {} is already scheduled task", name);
                return State.FAILURE;
            }
        } else {
            log.info("Task {} will be scheduled from now on", name);
            trafficScheduler.addFixedRateSchedule(name, httpRequestMono::subscribe, period);
        }
        return State.SCHEDULING;
    }

}
