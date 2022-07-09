package com.kubernetes.trafficmaker.target;

import org.springframework.scheduling.support.CronTrigger;

import java.time.ZoneId;

public record CronTrafficTargetSpec(HttpTargetSpec http, String cron, String timezone) {

    public CronTrigger cronTrigger() {
        if (timezone != null) {
            return new CronTrigger(cron, ZoneId.of(timezone));
        }
        return new CronTrigger(cron);
    }

}
