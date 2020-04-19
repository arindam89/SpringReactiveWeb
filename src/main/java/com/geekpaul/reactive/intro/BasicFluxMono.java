package com.geekpaul.reactive.intro;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    /**
     * Get the info about the current code and time
     * This is a generic Example of a Blocking call
     * In reality it can be any Blocking call
     */
    public String blockingGetInfo(Integer input) {
        delay();
        return String.format("[%d] on thread [%s] at time [%s]",
                input,
                Thread.currentThread().getName(),
                new Date());
    }


    /**
     * Basic example to show that Reactor
     * by itself will run the pipeline or code
     * on the same thread on which .subscribe() happened.
     */
    @Test
    public void threadBlocking() {
        Flux.range(1,5)
                .flatMap(a -> Mono.just(blockingGetInfo(a)))
                .subscribe(System.out::println);
    }

    /**
     * Adding a Scheduler puts the workload
     * of the main thread and hands it over to the
     * Scheduler orchestration.
     * But, still it doesn't gurantee it will leverage
     * all threads of create new threads whenever a
     * Async task is requested. So, basically we wanted
     * to fire all 5 tasks in 5 threads parallely
     * but that will not happen and you will see
     * it will end up reusing the same thread from the Scheduler
     */
    @Test
    public void threadNonBlockingBySchedulersProblem()  {
        Flux.range(1,5)
            .publishOn(Schedulers.elastic())
            .flatMap(a -> Mono.just(blockingGetInfo(a)))
            .subscribe(System.out::println);

    }

    // Solution of truly parallel
    // https://stackoverflow.com/questions/49489348/project-reactor-parallel-execution

    /**
     * Quickfix by converting the Flux Stream
     * to a parallel Flux which would runOn
     * a Scheduler and will be submitted as parallel
     * tasks depending on the .parallel() inputs
     * By default it creates parallism of the same
     * number of cores you have (for me it was 4)
     */
    @Test
    public void threadNonBlockingBySchedulers() {
        Flux.range(1,5)
            .parallel()
            .runOn(Schedulers.elastic())
            .flatMap(a -> Mono.just(blockingGetInfo(a)))
            .sequential()
            .subscribe(System.out::println);

    }

    /** Output
     * Notice: No [5] ran after 2s, but [1] to [4] ran on the same time.
     * [3] on thread [elastic-4] at time [Sun Apr 19 00:09:49 IST 2020]
     * [1] on thread [elastic-2] at time [Sun Apr 19 00:09:49 IST 2020]
     * [2] on thread [elastic-3] at time [Sun Apr 19 00:09:49 IST 2020]
     * [4] on thread [elastic-5] at time [Sun Apr 19 00:09:49 IST 2020]
     * [5] on thread [elastic-2] at time [Sun Apr 19 00:09:51 IST 2020]
     */

    /**
     * Getting into ParallelFlux creates
     * More confusion at times because even if
     * we pass explicit threadpool with more threads
     * it will not use those threads and again
     * will be limited to .parallel() call
     */
    @Test
    public void threadNonBlockingBySchedulersExecutor() {
        ExecutorService myPool = Executors.newFixedThreadPool(10);
        Flux.range(1,6)
            .parallel()
            .runOn(Schedulers.fromExecutorService(myPool))
            .flatMap(a -> Mono.just(blockingGetInfo(a)))
            .sequential()
            .subscribe(System.out::println);
    }

    /**
     * You can see even if we had 10 threads, only 4 are used.
     [4] on thread [pool-1-thread-8] at time [Sun Apr 19 00:13:38 IST 2020]
     [1] on thread [pool-1-thread-5] at time [Sun Apr 19 00:13:38 IST 2020]
     [2] on thread [pool-1-thread-6] at time [Sun Apr 19 00:13:38 IST 2020]
     [3] on thread [pool-1-thread-7] at time [Sun Apr 19 00:13:38 IST 2020]
     [5] on thread [pool-1-thread-5] at time [Sun Apr 19 00:13:40 IST 2020]
     [6] on thread [pool-1-thread-6] at time [Sun Apr 19 00:13:40 IST 2020]
     */

    /**
     * We can fix this by passing a
     * parallelism number to the
     * parallel() method then that many
     * Parallel Flux will be created from the Schedulers
     * But again, Schedulers might have less Threads
     * So, its a better practive to avoid parallel()
     * for just wrapping blocking calls
     * There are other powerful usages of ParallelFlux
     */
    @Test
    public void threadNonBlockingBySchedulersExecutorFixed() {
        ExecutorService myPool = Executors.newFixedThreadPool(10);
        Flux.range(1,6)
                .parallel(10)
                .runOn(Schedulers.fromExecutorService(myPool))
                .flatMap(a -> Mono.just(blockingGetInfo(a)))
                .sequential()
                .subscribe(System.out::println);
    }

    /**
     * You can see now, everything ran at once and all 6 threads are new from the pool.
     *  [4] on thread [pool-1-thread-5] at time [Sun Apr 19 00:17:57 IST 2020]
     *  [1] on thread [pool-1-thread-1] at time [Sun Apr 19 00:17:57 IST 2020]
     *  [3] on thread [pool-1-thread-3] at time [Sun Apr 19 00:17:57 IST 2020]
     *  [5] on thread [pool-1-thread-4] at time [Sun Apr 19 00:17:57 IST 2020]
     *  [2] on thread [pool-1-thread-2] at time [Sun Apr 19 00:17:57 IST 2020]
     *  [6] on thread [pool-1-thread-6] at time [Sun Apr 19 00:17:57 IST 2020]
     */


    /**
     * THERE GOT TO BE A BETTER WAY! AND THERE IS!
     * This is the Magic method which takes a Blocking call
     * and based on Scheduler makes it into completely
     * non-blocking Stream of data from the Publisher
     * which can be consumed by Subscribers.
     * @param a Int signifies which one it it.
     * @return Mono of the future Info.
     */
    public Mono<String> getInfoCallable(Integer a) {
        // Returns a non-blocking Publisher with a Single Value (Mono)
        return Mono
                .fromCallable(() -> blockingGetInfo(a)) // Define blocking call
                .subscribeOn(Schedulers.elastic()); // Define the execution model
    }

    /**
     * With the above wrapper on getInfo(),
     * we can now compose many calls in parallel
     * No need to worry about how much parallism
     * it will do as much as the Scheduler can
     * keep upto.
     */
    @Test
    public void blockingToNonBlockingRightWay() {
        Flux.range(1,10)
                .flatMap(this::getInfoCallable)
                .subscribe(System.out::println);

    }

    /**
     * Helper method which waits for the tests
     * to finish.
     */
    @AfterAll
    static void waitForMe() {
        try {
            Thread.sleep(12_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
