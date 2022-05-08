package com.kubernetes.trafficmaker.target;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("bb.traffic-maker.com")
@Version("v1alpha1")
public class TrafficTarget
        extends CustomResource<TrafficTargetSpec, TrafficTargetStatus>
        implements Namespaced {
}
