package com.kubernetes.trafficmaker.router;

import com.kubernetes.trafficmaker.schedule.TrafficScheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class TrafficMakerRouter {

    @Bean
    public RouterFunction<ServerResponse> trafficRouter(TrafficScheduler trafficScheduler) {
        return RouterFunctions.route()
                              .GET("/v1/health", r -> ServerResponse.ok().build())
                              .GET("/v1/schedule/count", r -> ServerResponse.ok().bodyValue(trafficScheduler.getActiveScheduleCount()))
                              .build();
    }

}
