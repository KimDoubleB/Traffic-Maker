package com.kubernetes.trafficmaker.model;

import com.kubernetes.trafficmaker.model.TrafficTargetStatus.State;
import com.kubernetes.trafficmaker.model.cron.CronTrafficTarget;
import com.kubernetes.trafficmaker.model.rate.RateTrafficTarget;
import lombok.Getter;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;

@Getter
public class TrafficTarget {

    private final String name;
    private final HttpTargetSpec http;
    private final Trigger trigger;
    private final State state;

    private TrafficTarget(String name, HttpTargetSpec http, Trigger trigger, State state) {
        this.name = name;
        this.http = http;
        this.trigger = trigger;
        this.state = state;
    }

    public static TrafficTarget fromRate(RateTrafficTarget rateTrafficTarget) {
        var name = rateTrafficTarget.getMetadata().getName();
        var rateTrafficTargetSpec = rateTrafficTarget.getSpec();
        var periodicTrigger = new PeriodicTrigger(rateTrafficTargetSpec.rate());
        var state = rateTrafficTarget.getStatus() != null
                    ? rateTrafficTarget.getStatus().state() : null;
        return new TrafficTarget(name, rateTrafficTargetSpec.http(), periodicTrigger, state);
    }

    public static TrafficTarget fromCron(CronTrafficTarget cronTrafficTarget) {
        var name = cronTrafficTarget.getMetadata().getName();
        var cronTrafficTargetSpec = cronTrafficTarget.getSpec();
        var cronTrigger = new CronTrigger(cronTrafficTargetSpec.cron());
        var state = cronTrafficTarget.getStatus() != null
                    ? cronTrafficTarget.getStatus().state() : null;
        return new TrafficTarget(name, cronTrafficTargetSpec.http(), cronTrigger, state);
    }

}
