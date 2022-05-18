package com.kubernetes.trafficmaker.target;

public record TrafficTargetStatus(Status status) {

    public static boolean isScheduledStatus(TrafficTargetStatus trafficTargetStatus) {
        return trafficTargetStatus != null
               && trafficTargetStatus.status() == Status.SCHEDULING;
    }

    public static boolean isFailureStatus(TrafficTargetStatus trafficTargetStatus) {
        return trafficTargetStatus != null
               && trafficTargetStatus.status() == Status.FAILURE;
    }

    public enum Status {
        SCHEDULING,
        FAILURE,
        UPDATING,
    }

}
