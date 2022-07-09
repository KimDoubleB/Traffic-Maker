package com.kubernetes.trafficmaker.reconciler;

import com.kubernetes.trafficmaker.schedule.TrafficScheduler;
import com.kubernetes.trafficmaker.model.rate.RateTrafficTarget;
import com.kubernetes.trafficmaker.model.TrafficTargetStatus.State;
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
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

@ControllerConfiguration
@Component
@RequiredArgsConstructor
@Slf4j
public class RateTrafficMakerReconciler implements Reconciler<RateTrafficTarget>,
                                                   Cleaner<RateTrafficTarget>,
                                                   ErrorStatusHandler<RateTrafficTarget> {

    private final TrafficScheduler trafficScheduler;

    @Override
    public UpdateControl<RateTrafficTarget> reconcile(RateTrafficTarget rateTrafficTarget, Context context) {
        log.debug("Reconcile by trafficTarget {}", rateTrafficTarget);

        var taskName = rateTrafficTarget.getMetadata().getName();
        var target = rateTrafficTarget.getSpec().http();
        var trigger = new PeriodicTrigger(rateTrafficTarget.getSpec().rate());
        var currentState = rateTrafficTarget.getStatus() != null
                           ? rateTrafficTarget.getStatus().state() : null;

        var isScheduled = trafficScheduler.schedule(taskName, target, trigger, currentState);
        rateTrafficTarget.updateTrafficTargetState(isScheduled);
        return UpdateControl.updateStatus(rateTrafficTarget);
    }

    @Override
    public DeleteControl cleanup(RateTrafficTarget rateTrafficTarget, Context context) {
        log.debug("Cleanup by trafficTarget {}", rateTrafficTarget);

        var taskName = rateTrafficTarget.getMetadata().getName();
        trafficScheduler.removeTask(taskName);
        return DeleteControl.defaultDelete();
    }

    @Override
    public ErrorStatusUpdateControl<RateTrafficTarget> updateErrorStatus(RateTrafficTarget rateTrafficTarget,
                                                                         Context<RateTrafficTarget> context,
                                                                         Exception e) {
        var retryCount = context.getRetryInfo().map(RetryInfo::getAttemptCount).orElse(0);
        log.error("Error occurred [Retry count {}]. Caused by [{}]. Exception message: {}",
                  retryCount, e.getClass(), e.getMessage());
        rateTrafficTarget.updateTrafficTargetState(State.ERROR);
        return ErrorStatusUpdateControl.updateStatus(rateTrafficTarget);
    }

}
