package com.kubernetes.trafficmaker.reconciler;

import com.kubernetes.trafficmaker.schedule.TrafficScheduleTask;
import com.kubernetes.trafficmaker.schedule.TrafficScheduler;
import com.kubernetes.trafficmaker.target.TrafficTarget;
import com.kubernetes.trafficmaker.target.TrafficTargetStatus.State;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.RetryInfo;
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
    public ErrorStatusUpdateControl<TrafficTarget> updateErrorStatus(TrafficTarget trafficTarget,
                                                                     Context<TrafficTarget> context,
                                                                     Exception e) {
        var retryCount = context.getRetryInfo().map(RetryInfo::getAttemptCount).orElse(0);
        log.error("Error occurred [Retry count {}]. Caused by [{}]. Exception message: {}",
                  retryCount, e.getClass(), e.getMessage());
        trafficTarget.updateTrafficTaskState(State.ERROR);
        return ErrorStatusUpdateControl.updateStatus(trafficTarget);
    }

}
