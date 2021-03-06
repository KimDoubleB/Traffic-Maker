package com.kubernetes.trafficmaker.model.rate;

import com.kubernetes.trafficmaker.model.TrafficTargetStatus;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.extern.slf4j.Slf4j;

@Group("bb.com")
@Version("v1alpha1")
@Slf4j
public class RateTrafficTarget
    extends CustomResource<RateTrafficTargetSpec, TrafficTargetStatus>
    implements Namespaced {

    public void updateTrafficTargetState(TrafficTargetStatus.State state) {
        setStatus(new TrafficTargetStatus(state));
    }

    public void updateTrafficTargetState(boolean isScheduled) {
        if (isScheduled) {
            setStatus(new TrafficTargetStatus(TrafficTargetStatus.State.SCHEDULING));
        } else {
            setStatus(new TrafficTargetStatus(TrafficTargetStatus.State.FAILURE));
        }
    }

}
