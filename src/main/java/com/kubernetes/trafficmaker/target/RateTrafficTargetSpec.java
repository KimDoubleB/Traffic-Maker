package com.kubernetes.trafficmaker.target;

public record RateTrafficTargetSpec(HttpTargetSpec http, Long rate) {
}
