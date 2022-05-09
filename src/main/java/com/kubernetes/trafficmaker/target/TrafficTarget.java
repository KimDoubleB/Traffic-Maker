package com.kubernetes.trafficmaker.target;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

@Group("bb.traffic-maker.com")
@Version("v1alpha1")
@Slf4j
public class TrafficTarget
        extends CustomResource<TrafficTargetSpec, TrafficTargetStatus>
        implements Namespaced {

    public Runnable requestToTargetTask(WebClient webClient, URI targetUri) {
        return () -> {
            log.debug("Request to target {} by thread {}", targetUri, Thread.currentThread().getId());
            webClient.get()
                    .uri(targetUri)
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe();
        };
    }

}
