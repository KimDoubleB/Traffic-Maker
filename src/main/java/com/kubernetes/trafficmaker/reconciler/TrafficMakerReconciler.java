package com.kubernetes.trafficmaker.reconciler;

import com.kubernetes.trafficmaker.schedule.TrafficScheduler;
import com.kubernetes.trafficmaker.target.TrafficTarget;
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
        scheduleTask(trafficTarget);
        return UpdateControl.updateStatus(trafficTarget);
    }

    @Override
    public DeleteControl cleanup(TrafficTarget trafficTarget, Context context) {
        log.debug("Cleanup by trafficTarget {}", trafficTarget);

        var resourceName = trafficTarget.getMetadata().getName();
        trafficScheduler.removeSchedule(resourceName);
        return DeleteControl.defaultDelete();
    }

    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    private void scheduleTask(TrafficTarget trafficTarget) {
        var resourceName = trafficTarget.getMetadata().getName();
        var httpTargetSpec = trafficTarget.getSpec().http();
        var httpRequestMono = httpTargetSpec.toRequestMono(webClient);
        var period = DurationStyle.detectAndParse(trafficTarget.getSpec().rate());
        var isTaskScheduled = trafficScheduler.addFixedRateSchedule(resourceName, httpRequestMono::subscribe, period);
        trafficTarget.updateTrafficTaskStatus(isTaskScheduled);
    }

}
