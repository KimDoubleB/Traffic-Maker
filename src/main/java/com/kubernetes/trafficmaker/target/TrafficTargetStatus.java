package com.kubernetes.trafficmaker.target;

public record TrafficTargetStatus(Status status) {

    public enum Status {
        SCHEDULING,
        FAILURE,
    }

}
