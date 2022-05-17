package com.kubernetes.trafficmaker.target;

public record TrafficTargetSpec(HttpTargetSpec http, String rate) {
}
