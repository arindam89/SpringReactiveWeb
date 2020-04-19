package com.geekpaul.reactive.intro;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

public class StepVerifierExamples {

    @Test
    public void basicStepVerifier() {
        Flux<String> source = Flux.just("John", "Monica", "Mark", "Cloe", "Frank", "Casper", "Olivia", "Emily", "Cate")
                .filter(name -> name.length() == 4)
                .map(String::toUpperCase);

        StepVerifier
                .create(source)
                .expectNext("JOHN")
                .expectNextMatches(name -> name.startsWith("MA"))
                .expectNext("CLOE", "CATE")
                .expectComplete()
                .verify();
    }


    /**
     * Test with virtual time
     * Here we can simulate the clock
     * and make it tick faster to get
     * results instants.
     */
    @Test
    public void basicVirtualTimeVerifier() {
        // We want to verify a clock ticker which emits 0, 1, 2.. etc
        // every 5 mins
        StepVerifier
                .withVirtualTime(() -> Flux.interval(Duration.ofMinutes(5)).take(2))
                .expectSubscription()
                .expectNoEvent(Duration.ofMinutes(5))
                .expectNext(0L)
                .thenAwait(Duration.ofMinutes(5))
                .expectNext(1L)
                .verifyComplete();
    }

    /**
     * Simple method which simulates a crash
     * at some point.
     * @param a Integer input
     */
    public String doSomethingDangerous(Integer a) {
        // Just crash for vaule 6
        if(a == 6) {
            throw new RuntimeException("Oops! something broke");
        }
        return a.toString();
    }

    /**
     * Error handling in Subscriber
     * Callback
     */
    @Test
    public void fluxErrorHanlingInSubscriber() {
        Flux.range(1, 10)
                .map(this::doSomethingDangerous)
                .subscribe(
                        value -> System.out.println("RECEIVED " + value),
                        error -> System.err.println("CAUGHT " + error)
            );
    }

    /**
     * This example demonstrates Cold Publishers
     * You will see that even when second
     * subscriber starts late, still he gets all
     * the values from the beginning of the sequence
     * @throws InterruptedException
     */
    @Test
    public void exampleColdPublisher() throws InterruptedException {
        // Start a cold Publisher which emits 0,1,2 every sec.
        Flux<Long> flux =  Flux.interval(Duration.ofSeconds(1));
        // Let's subscribe to that with multiple subscribers.
        flux.subscribe(i -> System.out.println("first_subscriber received value:" + i));
        // Start firing events with .connect() on the published flux.
        Thread.sleep(3_000);
        // Let a second subscriber come after some time 3 secs here.
        flux.subscribe(i -> System.out.println("second_subscriber received value:" + i));
    }

    /**
     * This example demonstrates Hot Publishers
     * You will see that when second
     * subscriber starts late, it will only get
     * the values after it has subscribed and
     * not from the beginning of the sequence
     * @throws InterruptedException
     */
    @Test
    public void exampleHotPublisher() throws InterruptedException {
        // Start a cold Publisher which emits 0,1,2 every sec.
        Flux<Long> flux =  Flux.interval(Duration.ofSeconds(1));
        // Make the Publisher Hot
        ConnectableFlux<Long> connectableFlux = flux.publish();
        // Now that we have a handle on the hot Publisher
        // Let's subscribe to that with multiple subscribers.
        connectableFlux.subscribe(i -> System.out.println("first_subscriber received value:" + i));
        // Start firing events with .connect() on the published flux.
        connectableFlux.connect();
        Thread.sleep(3_000);
        // Let a second subscriber come after some time 3 secs here.
        connectableFlux.subscribe(i -> System.out.println("second_subscriber received value:" + i));
    }

    /**
     * Helper method which waits for the tests
     * to finish.
     */
    @AfterAll
    static void waitForMe() {
        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
