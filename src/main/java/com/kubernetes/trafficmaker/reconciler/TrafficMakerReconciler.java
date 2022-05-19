package com.kubernetes.trafficmaker.reconciler;

import com.kubernetes.trafficmaker.schedule.TrafficScheduler;
import com.kubernetes.trafficmaker.target.TrafficTarget;
import com.kubernetes.trafficmaker.target.TrafficTargetStatus;
import com.kubernetes.trafficmaker.target.TrafficTargetStatus.Status;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("ReactiveStreamsUnusedPublisher")
@ControllerConfiguration
@Component
@RequiredArgsConstructor
@Slf4j
public class TrafficMakerReconciler implements Reconciler<TrafficTarget> {

    private final WebClient webClient;
    private final TrafficScheduler trafficScheduler;

    @Override
    public UpdateControl<TrafficTarget> reconcile(TrafficTarget trafficTarget, Context context) {
        log.debug("Reconcile by trafficTarget {}", trafficTarget);

        var status = trafficTarget.getStatus() != null
                             ? trafficTarget.getStatus().status() : Status.INITIALIZING;
        return switch (status) {
            case FAILURE -> failureStatusReconcile(trafficTarget);
            case SCHEDULING -> schedulingStatusReconcile(trafficTarget);
            default -> reconcile(trafficTarget);
        };
    }

    @Override
    public DeleteControl cleanup(TrafficTarget trafficTarget, Context context) {
        log.debug("Cleanup by trafficTarget {}", trafficTarget);

        var resourceName = trafficTarget.getMetadata().getName();
        trafficScheduler.removeSchedule(resourceName);
        return DeleteControl.defaultDelete();
    }

    private UpdateControl<TrafficTarget> failureStatusReconcile(TrafficTarget trafficTarget) {
        trafficTarget.updateTrafficTaskStatus(Status.UPDATING);
        return UpdateControl.updateStatus(trafficTarget)
                       .rescheduleAfter(5, TimeUnit.SECONDS);
    }

    private UpdateControl<TrafficTarget> schedulingStatusReconcile(TrafficTarget trafficTarget) {
        var taskName = trafficTarget.getMetadata().getName();
        var httpTargetSpec = trafficTarget.getSpec().http();
        var httpRequestMono = httpTargetSpec.toRequestMono(webClient);
        var period = DurationStyle.detectAndParse(trafficTarget.getSpec().rate());

        if (trafficScheduler.isScheduledTask(taskName)) {
            trafficScheduler.updateFixedRateSchedule(taskName, httpRequestMono::subscribe, period);
        } else {
            trafficScheduler.addFixedRateSchedule(taskName, httpRequestMono::subscribe, period);
        }
        return UpdateControl.noUpdate();
    }

    private UpdateControl<TrafficTarget> reconcile(TrafficTarget trafficTarget) {
        var taskName = trafficTarget.getMetadata().getName();
        var httpTargetSpec = trafficTarget.getSpec().http();
        var httpRequestMono = httpTargetSpec.toRequestMono(webClient);
        var period = DurationStyle.detectAndParse(trafficTarget.getSpec().rate());

        if (trafficScheduler.isScheduledTask(taskName)) {
            log.warn("Task {} is already scheduled task.", taskName);
            trafficTarget.updateTrafficTaskStatus(Status.FAILURE);
        } else {
            trafficScheduler.addFixedRateSchedule(taskName, httpRequestMono::subscribe, period);
            trafficTarget.updateTrafficTaskStatus(Status.SCHEDULING);
        }
        return UpdateControl.updateStatus(trafficTarget);
    }

}
