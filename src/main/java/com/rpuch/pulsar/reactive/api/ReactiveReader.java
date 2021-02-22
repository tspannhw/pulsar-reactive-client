package com.rpuch.pulsar.reactive.api;

import org.apache.pulsar.client.api.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Roman Puchkovskiy
 */
public interface ReactiveReader<T> {
    String getTopic();

    Flux<Message<T>> receive();

    boolean hasReachedEndOfTopic();

    Mono<Boolean> hasMessageAvailable();

    boolean isConnected();
}
