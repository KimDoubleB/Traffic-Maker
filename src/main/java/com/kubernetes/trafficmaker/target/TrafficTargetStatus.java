package com.kubernetes.trafficmaker.target;

public record TrafficTargetStatus(Status status) {

    public static boolean isScheduledStatus(TrafficTargetStatus trafficTargetStatus) {
        return trafficTargetStatus != null
               && trafficTargetStatus.status() == TrafficTargetStatus.Status.SCHEDULING;
    }

    public enum Status {
        SCHEDULING,
        FAILURE,
    }

}
