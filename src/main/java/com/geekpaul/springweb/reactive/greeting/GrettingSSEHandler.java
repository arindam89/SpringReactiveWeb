package com.geekpaul.springweb.reactive.greeting;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * Shows example of a continuous data sent from Server
 * in Flux Model
 */
@RestController
public class GrettingSSEHandler {
    /**
     * Continuous Response
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> hellContinuous() {
        Flux<String> producer = Flux.interval(Duration.ofSeconds(1)).map(tick -> "Hello World");
        return producer;
    }
}
