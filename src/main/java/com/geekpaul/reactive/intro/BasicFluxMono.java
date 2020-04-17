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

    @Test
    public void FluxWithGenerate() {
        Flux<String> flux = Flux.generate(
                () -> 0,
                (state, sink) -> {
                    sink.next("3 x " + state + " = " + 3*state);
                    if (state == 10) sink.complete();
                    return state + 1;
                });
        flux.subscribe(System.out::println);
    }

    /**
     * create can be very useful to bridge an existing API
     * with the reactive world - such as an asynchronous API based on listeners.
     */
    // Given I have a Async API like the following interface.
    interface MyEventListener<T> {
        void onDataChunk(List<T> chunk);
        void processComplete();
    }

    @Test
    public void FluxWithCreate() throws InterruptedException {
        // Create a Simple Async API
        class EventProcessor {
            public MyEventListener l;
            public void register(MyEventListener listener) {
                this.l = listener;
            }
            public void process() throws InterruptedException {
                Arrays.asList(1,2,3,4,5)
                        .forEach(i -> {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            l.onDataChunk(Arrays.asList(i.toString()));
                        });
                Thread.sleep(2000);
                l.processComplete();
            }
        }
        // Creating a Async operation somewhere.
        EventProcessor processor = new EventProcessor();

        Flux<String> bridge = Flux.create(sink -> {
            processor.register(
                    new MyEventListener<String>() {

                        public void onDataChunk(List<String> chunk) {
                            for(String s : chunk) {
                                sink.next(s);
                            }
                        }

                        public void processComplete() {
                            sink.complete();
                        }
                    });
        });
        bridge.subscribe(
                System.out::println,
                null,
                () -> { System.out.println("Complete Done"); });
        // Need to trigger else nothing will happen.
        processor.process();
    }


}
