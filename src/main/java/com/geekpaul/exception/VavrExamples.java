package com.geekpaul.exception;

import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class VavrExamples {

    // = Success(result) or Failure(exception)
    Try<Integer> divide(Integer dividend, Integer divisor) {
        return Try.of(() -> dividend / divisor);
    }

    @Test
    public void createTry() {
        System.out.println(divide(10,2));
        System.out.println(divide(10,0));
    }


    // = Right(result) or Left(exception)
    Either<Exception, Integer> divideEither(Integer dividend, Integer divisor) {
        try {
            return Either.right(dividend / divisor);
        } catch (Exception e) {
            return Either.left(e);
        }
    }

    @Test
    public void createEither() {
        System.out.println(divideEither(10,2));
        System.out.println(divideEither(10,0));
    }


    interface CustomerProvider {
        List<String> getPersonalRecomendations(final String customerId) throws IOException,IllegalArgumentException;
    }

    interface GenericSuggestionProvider {
        List<String> getGeneralRecomendations() throws IOException;
    }

    interface CacheSuggestionProvider {
        List<String> getCachedRecomendations();
    }

    CustomerProvider customer;
    GenericSuggestionProvider suggestion;
    CacheSuggestionProvider cache;

    public List<String> emptySuggestionList() {
        return new ArrayList<>();
    }

    List<String> getRecomendationsForUI(final String customerId) {
       return Try.of(() -> customer.getPersonalRecomendations(customerId))
                    .orElse(() -> Try.of(() -> suggestion.getGeneralRecomendations()))
                    .recover(IllegalArgumentException.class, cache.getCachedRecomendations())
                    .recover(IOException.class, cache.getCachedRecomendations())
                    .andFinally(System.out::println) // You can log metrics etc. here.
                    .get();

    }

}
