package com.kubernetes.trafficmaker.reconciler;

import com.kubernetes.trafficmaker.schedule.TrafficScheduler;
import com.kubernetes.trafficmaker.model.cron.CronTrafficTarget;
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
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@ControllerConfiguration
@Component
@RequiredArgsConstructor
@Slf4j
public class CronTrafficMakerReconciler implements Reconciler<CronTrafficTarget>,
                                                   Cleaner<CronTrafficTarget>,
                                                   ErrorStatusHandler<CronTrafficTarget> {

    private final TrafficScheduler trafficScheduler;

    @Override
    public UpdateControl<CronTrafficTarget> reconcile(CronTrafficTarget cronTrafficTarget, Context context) {
        log.debug("Reconcile by trafficTarget {}", cronTrafficTarget);

        var taskName = cronTrafficTarget.getMetadata().getName();
        var target = cronTrafficTarget.getSpec().http();
        var trigger = new CronTrigger(cronTrafficTarget.getSpec().cron());
        var currentState = cronTrafficTarget.getStatus() != null
                           ? cronTrafficTarget.getStatus().state() : null;

        var isScheduled = trafficScheduler.schedule(taskName, target, trigger, currentState);
        cronTrafficTarget.updateTrafficTargetState(isScheduled);
        return UpdateControl.updateStatus(cronTrafficTarget);
    }

    @Override
    public DeleteControl cleanup(CronTrafficTarget cronTrafficTarget, Context context) {
        log.debug("Cleanup by CronTrafficTarget {}", cronTrafficTarget);
        return TrafficMakerCleaner.cleanup(trafficScheduler, cronTrafficTarget);
    }

    @Override
    public ErrorStatusUpdateControl<CronTrafficTarget> updateErrorStatus(CronTrafficTarget cronTrafficTarget,
                                                                         Context<CronTrafficTarget> context,
                                                                         Exception e) {
        var retryCount = context.getRetryInfo().map(RetryInfo::getAttemptCount).orElse(0);
        log.error("Error occurred [Retry count {}]. Caused by [{}]. Exception message: {}",
                  retryCount, e.getClass(), e.getMessage());
        cronTrafficTarget.updateTrafficTargetState(State.ERROR);
        return ErrorStatusUpdateControl.updateStatus(cronTrafficTarget);
    }

}
