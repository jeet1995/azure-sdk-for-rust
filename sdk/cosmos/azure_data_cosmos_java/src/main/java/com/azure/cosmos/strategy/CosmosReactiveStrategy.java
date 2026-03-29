// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos.strategy;

import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;

/**
 * Reactor-first wrapper around any {@link CosmosClientStrategy}.
 * Provides {@link Mono} and {@link CompletableFuture} APIs regardless of which
 * backend (JNI/JNA) is used.
 */
public class CosmosReactiveStrategy implements AutoCloseable {

    private final CosmosClientStrategy delegate;

    public CosmosReactiveStrategy(CosmosClientStrategy delegate) {
        this.delegate = delegate;
    }

    public static CosmosReactiveStrategy create(String endpoint, String key) throws CosmosException {
        return new CosmosReactiveStrategy(CosmosClientStrategy.create(endpoint, key));
    }

    public Mono<CosmosItemResponse> readItem(String databaseId, String containerId,
                                              String itemId, String partitionKey) {
        return Mono.fromCallable(
                () -> delegate.readItem(databaseId, containerId, itemId, partitionKey)
        ).subscribeOn(Schedulers.boundedElastic());
    }

    public CompletableFuture<CosmosItemResponse> readItemAsync(
            String databaseId, String containerId, String itemId, String partitionKey) {
        return readItem(databaseId, containerId, itemId, partitionKey).toFuture();
    }

    public Mono<Void> createItem(String databaseId, String containerId,
                                  String partitionKey, String jsonData) {
        return Mono.<Void>fromCallable(() -> {
            delegate.createItem(databaseId, containerId, partitionKey, jsonData);
            return null;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public CompletableFuture<Void> createItemAsync(
            String databaseId, String containerId, String partitionKey, String jsonData) {
        return createItem(databaseId, containerId, partitionKey, jsonData).toFuture();
    }

    @Override
    public void close() {
        delegate.close();
    }
}
