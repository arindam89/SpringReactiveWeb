package com.geekpaul.reactive.intro;


import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

public class BasicPubSub {

    @Test
    public void BasicFlux() {
        Flux<String> names = Flux.just("Arindam","Paul");
        names.subscribe(System.out::println);
    }
}
