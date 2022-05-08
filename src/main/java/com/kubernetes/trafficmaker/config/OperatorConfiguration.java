package com.kubernetes.trafficmaker.config;

import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OperatorConfiguration {

    @Bean
    KubernetesClient kubernetesClient() {
        var config = new ConfigBuilder().withNamespace(null).build();
        return new DefaultKubernetesClient(config);
    }

    @Bean
    Operator operator(KubernetesClient kubernetesClient, List<Reconciler<?>> customResources) {
        var operator = new Operator(kubernetesClient, DefaultConfigurationService.instance());
        customResources.forEach(operator::register);
        operator.installShutdownHook();
        operator.start();
        return operator;
    }

}
