package com.kubernetes.trafficmaker.target;

public record TrafficTargetStatus(State state) {
    public enum State {
        SCHEDULING,
        FAILURE,
        UPDATING,
        INITIALIZING,
    }

}
