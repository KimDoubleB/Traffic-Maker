package com.kubernetes.trafficmaker.target;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public record HttpTargetSpec(int timeoutSeconds,
                             String uri,
                             Map<String, String> headers,
                             String body,
                             String method) {

    public Mono<String> toRequestMono(WebClient webClient) {
        var httpMethod = HttpMethod.valueOf(method);
        var targetUri = URI.create(uri);
        var timeout = Duration.ofSeconds(timeoutSeconds);
        return webClient.method(httpMethod)
                        .uri(targetUri)
                        .headers(h -> h.setAll(headers))
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(timeout)
                        .doOnNext(responseLogging());
    }

    private Consumer<String> responseLogging() {
        return response -> log.debug("Http task ({}) by thread {} - Response ({})",
                                     this, Thread.currentThread().getId(), response);
    }

}
