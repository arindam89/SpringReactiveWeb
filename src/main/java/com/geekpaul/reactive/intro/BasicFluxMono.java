package com.geekpaul.reactive.intro;


import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

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

    @Test
    public void quickBrownFox() {
        String allAlphabets = "the quick brown fox jumps over the lazy dog";
        List<String> words = Arrays.asList(allAlphabets.split(" "));

        Flux.fromIterable(words)
                .flatMap(word -> Flux.fromArray(word.split("")))
                .distinct()
                .zipWith(Flux.range(1,50), (letter, line) -> line + " " + letter)
                .subscribe(System.out::println);
    }

    // This will run the code in main thread
    @Test
    public void publisherOnMainThread() throws InterruptedException {
        final Mono<String> mono = Mono.just("Hello from thread: ");
        mono.subscribe(v -> System.out.println(v + Thread.currentThread().getName()));
    }

    /** Output
     * 19:22:01.181 [main] DEBUG reactor.util.Loggers$LoggerFactory - Using Slf4j logging framework
     * Hello from thread: main
     *
     * Process finished with exit code 0
     **/

    // This will run the code in a separate thread
    @Test
    public void publisherOnSeparateThread() throws InterruptedException {
        Thread t = new Thread(() -> {
            final Mono<String> mono = Mono.just("Hello from thread: ");
            mono.subscribe(v -> System.out.println(v + Thread.currentThread().getName()));
        });
        t.start();
        t.join();
    }

    /** Output
     * 19:23:57.597 [main] DEBUG reactor.util.Loggers$LoggerFactory - Using Slf4j logging framework
     * Hello from thread: main
     *
     * Hello from thread: Thread-0
     **/

    // Just simulate some delay on the current thread.
    public void delay() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Get the info about the current code and time
    public String getInfo(Integer input) {
        return String.format("[%d] on thread [%s] at time [%s]",
                input,
                Thread.currentThread().getName(),
                new Date());
    }


    @Test
    public void threadBlocking() {
        Flux.fromIterable(Arrays.asList(1,2,3,4,5))
                .flatMap(a -> {
                    delay();
                    String info = getInfo(a);
                    return Mono.just(info);
                }).subscribe(System.out::println);
    }

    @Test
    public void threadNonBlockingBySchedulersProblem() throws InterruptedException {
        Flux.fromIterable(Arrays.asList(1,2,3,4,5))
                .publishOn(Schedulers.elastic())
                .flatMap(a -> {
                    delay();
                    return Mono.just(getInfo(a));
                })
                .subscribe(System.out::println);

        // Becasue the publish is happening at a different thread
        // We have to wait on the Main for it to complete
        Thread.sleep(10000);
    }

    // Solution of truly parallel
    // https://stackoverflow.com/questions/49489348/project-reactor-parallel-execution
    @Test
    public void threadNonBlockingBySchedulers() throws InterruptedException {
        Flux.fromIterable(Arrays.asList(1,2,3,4,5))
                .parallel()
                .runOn(Schedulers.elastic())
                .flatMap(a -> {
                    delay();
                    return Mono.just(getInfo(a));
                })
                .sequential()
                .subscribe(System.out::println);

        // Becasue the publish is happening at a different thread
        // We have to wait on the Main for it to complete
        Thread.sleep(3000);
    }

    @Test
    public void threadNonBlockingBySchedulersExecutor() throws InterruptedException {
        ExecutorService myPool = Executors.newFixedThreadPool(10);
        Flux.fromIterable(Arrays.asList(1,2,3,4,5))
                .parallel()
                .runOn(Schedulers.fromExecutorService(myPool))
                .flatMap(a -> {
                    delay();
                    return Mono.just(getInfo(a));
                })
                .sequential()
                .subscribe(System.out::println);

        // Becasue the publish is happening at a different thread
        // We have to wait on the Main for it to complete
        Thread.sleep(3000);
    }

}
