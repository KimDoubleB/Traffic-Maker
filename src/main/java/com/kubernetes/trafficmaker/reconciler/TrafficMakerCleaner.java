package com.kubernetes.trafficmaker.reconciler;

import com.kubernetes.trafficmaker.schedule.TrafficScheduler;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;

public class TrafficMakerCleaner {

    private TrafficMakerCleaner() {
    }

    public static DeleteControl cleanup(TrafficScheduler trafficScheduler, HasMetadata resource) {
        var taskName = resource.getMetadata().getName();
        trafficScheduler.removeTask(taskName);
        return DeleteControl.defaultDelete();
    }

}
