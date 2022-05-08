package com.kubernetes.trafficmaker.target;

public record TrafficTargetSpec(String targetUri, String rate) {
}
