package com.kubernetes.trafficmaker.target;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.verify.VerificationTimes;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

class HttpTargetSpecTest {

    private static final String HOST = "localhost";
    private static final int PORT = 8089;

    private ClientAndServer clientAndServer;
    private MockServerClient mockServerClient;

    @BeforeEach
    void setUp() {
        clientAndServer = ClientAndServer.startClientAndServer(PORT);
        mockServerClient = new MockServerClient(HOST, PORT);
    }

    @AfterEach
    void tearDown() {
        clientAndServer.stop();
        mockServerClient.close();
    }

    @DisplayName("HttpTargetSpec.toRequestMono로부터 전송되는 요청이 제대로 전송되는지 검증한다")
    @Test
    void verify_HttpTargetSpec_toRequestMono_request() {
        // given
        var method = "POST";
        var uri = "http://%s:%s".formatted(HOST, PORT);
        var headers = Map.of(HttpHeaders.AUTHORIZATION, "encrypted-data", HttpHeaders.CONTENT_TYPE, "application/json");
        var body = "{\"metadata\":{\"name\":\"some-resource-name\",\"createdAt\":412124214,\"namespace\":\"kube-system\"}}";
        var httpTargetSpec = new HttpTargetSpec(10, uri, headers, body, method);

        // and define mock client
        var result = "REQUEST COMPLETE";
        var mockHeaders = headers.keySet().stream()
                                  .map(headerKey -> new Header(headerKey, headers.get(headerKey)))
                                  .toList();
        mockServerClient
                .when(request()
                              .withMethod(method)
                              .withHeaders(mockHeaders)
                              .withBody(body))
                .respond(response()
                                 .withBody(result));

        // when
        var requestMono = httpTargetSpec.toRequestMono(WebClient.create());

        // then
        StepVerifier.create(requestMono)
                .expectNext(result)
                .verifyComplete();
        mockServerClient.verify(request().withMethod(method), VerificationTimes.exactly(1));
    }

    @DisplayName("HttpTargetSpec.timeoutSeconds 보다 응답이 늦게오는 경우, TimeoutException이 발생한다")
    @Test
    void throw_TimeoutException_when_delays_more_HttpTargetSpec_timeoutSeconds() {
        // given
        var method = "POST";
        var uri = "http://%s:%s".formatted(HOST, PORT);
        var httpTargetSpec = new HttpTargetSpec(1, uri, Map.of(), "Some-body", method);

        // and define mock client
        mockServerClient
                .when(request().withMethod(method))
                .respond(response().withDelay(TimeUnit.SECONDS, 5));

        // when
        var requestMono = httpTargetSpec.toRequestMono(WebClient.create());

        // then
        StepVerifier.create(requestMono)
                .expectError(TimeoutException.class)
                .verify();
    }

}