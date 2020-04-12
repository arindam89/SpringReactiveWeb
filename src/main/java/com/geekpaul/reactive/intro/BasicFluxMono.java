package com.geekpaul.reactive.intro;


import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

public class BasicFluxMono {

    /**
     * Create Flux with .just() operator.
     */
    @Test
    public void BasicFluxFactory() {
        Flux<String> names = Flux.just("Arindam","Paul");
        names.subscribe(System.out::println);
    }

    /**
     * Flux can also be created from Arrays or lists or collection
     * which has the concept of an Iterator.
     */
    @Test
    public void FluxFromList() {
        List<Integer> marks = Arrays.asList(1,2,3,4,5);
        Flux<Integer> flux = Flux.fromIterable(marks);

        // Note that you can use operators to transform a Stream like Flux
        flux.take(2)
            .map(n -> n*2)
            .subscribe(System.out::println);
    }

    @Test
    public void FluxMonoFactory() {
        Mono<String> bang = Mono.just("!");
        Flux<String> flux = Flux.just("Hello", "World");

        // Note that you can use operators to transform a Stream like Flux
        flux.concatWith(bang)
                .subscribe(System.out::println);
    }
}
