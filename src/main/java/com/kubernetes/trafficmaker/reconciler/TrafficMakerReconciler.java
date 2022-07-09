package com.kubernetes.trafficmaker.reconciler;

import com.kubernetes.trafficmaker.schedule.TrafficScheduleTask;
import com.kubernetes.trafficmaker.schedule.TrafficScheduler;
import com.kubernetes.trafficmaker.target.TrafficTarget;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@ControllerConfiguration
@Component
@RequiredArgsConstructor
@Slf4j
public class TrafficMakerReconciler implements Reconciler<TrafficTarget>,
                                                       Cleaner<TrafficTarget>,
                                                       ErrorStatusHandler<TrafficTarget> {

    private final WebClient webClient;
    private final TrafficScheduler trafficScheduler;

    @Override
    public UpdateControl<TrafficTarget> reconcile(TrafficTarget trafficTarget, Context context) {
        log.debug("Reconcile by trafficTarget {}", trafficTarget);

        var taskName = trafficTarget.getMetadata().getName();
        var currentState = trafficTarget.getStatus() != null
                                   ? trafficTarget.getStatus().state() : null;

        var updatedState = TrafficScheduleTask.of(taskName, trafficTarget.getSpec(), webClient)
                                   .register(trafficScheduler, currentState);
        trafficTarget.updateTrafficTaskState(updatedState);
        return UpdateControl.updateStatus(trafficTarget);
    }

    @Override
    public DeleteControl cleanup(TrafficTarget trafficTarget, Context context) {
        log.debug("Cleanup by trafficTarget {}", trafficTarget);

        var taskName = trafficTarget.getMetadata().getName();
        trafficScheduler.removeSchedule(taskName);
        return DeleteControl.defaultDelete();
    }

    @Override
    public ErrorStatusUpdateControl<TrafficTarget> updateErrorStatus(TrafficTarget resource, Context<TrafficTarget> context, Exception e) {
        return null;
    }

}
