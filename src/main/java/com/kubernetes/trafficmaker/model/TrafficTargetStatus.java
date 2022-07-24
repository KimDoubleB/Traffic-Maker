package com.kubernetes.trafficmaker.model;

public record TrafficTargetStatus(State state) {
    public enum State {

        SCHEDULING,
        FAILURE,
        ERROR,

    }

}
