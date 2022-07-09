package com.kubernetes.trafficmaker.model.rate;

import com.kubernetes.trafficmaker.model.HttpTargetSpec;

public record RateTrafficTargetSpec(HttpTargetSpec http, Long rate) {
}
