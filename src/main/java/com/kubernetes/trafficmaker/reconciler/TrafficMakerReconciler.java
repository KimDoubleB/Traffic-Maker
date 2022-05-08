package com.kubernetes.trafficmaker.reconciler;

import com.kubernetes.trafficmaker.target.TrafficTarget;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.springframework.stereotype.Component;

@ControllerConfiguration
@Component
public class TrafficMakerReconciler implements Reconciler<TrafficTarget> {

    @Override
    public UpdateControl<TrafficTarget> reconcile(TrafficTarget trafficTarget, Context context) {
        return UpdateControl.updateStatus(trafficTarget);
    }

    @Override
    public DeleteControl cleanup(TrafficTarget trafficTarget, Context context) {
        return DeleteControl.defaultDelete();
    }

}
