// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos;

import com.azure.cosmos.models.CosmosItemResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Reactor-first async Cosmos DB client.
 *
 * <p>All operations return {@link Mono} and execute on {@link Schedulers#boundedElastic()}
 * to avoid blocking Reactor's event loops. The underlying JNI call is synchronous —
 * the async layer is pure Java.</p>
 *
 * <p>Usage:
 * <pre>{@code
 * try (CosmosReactiveClient client = CosmosReactiveClient.createWithKey(endpoint, key)) {
 *     client.readItem("myDb", "myContainer", "doc1", "pk1")
 *           .map(CosmosItemResponse::getBody)
 *           .subscribe(System.out::println);
 * }
 * }</pre>
 */
public final class CosmosReactiveClient implements AutoCloseable {

    private final CosmosClient syncClient;

    private CosmosReactiveClient(CosmosClient syncClient) {
        this.syncClient = syncClient;
    }

    /**
     * Creates a new reactive CosmosClient authenticated with a master key.
     */
    public static CosmosReactiveClient createWithKey(String endpoint, String key)
            throws CosmosException {
        CosmosClient sync = CosmosClient.createWithKey(endpoint, key);
        return new CosmosReactiveClient(sync);
    }

    /**
     * Reads an item reactively.
     *
     * <p>The returned {@link Mono} is cold — nothing executes until subscribed.
     * The blocking JNI call runs on {@code Schedulers.boundedElastic()},
     * never on the caller's thread.</p>
     */
    public Mono<CosmosItemResponse> readItem(String databaseId, String containerId,
                                              String itemId, String partitionKey) {
        return Mono.fromCallable(
                () -> syncClient.readItem(databaseId, containerId, itemId, partitionKey)
        ).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Reads an item and returns the result as a {@link java.util.concurrent.CompletableFuture}.
     * Convenience for callers who prefer CompletableFuture over Mono.
     */
    public java.util.concurrent.CompletableFuture<CosmosItemResponse> readItemAsync(
            String databaseId, String containerId, String itemId, String partitionKey) {
        return readItem(databaseId, containerId, itemId, partitionKey).toFuture();
    }

    /**
     * Creates an item reactively.
     */
    public Mono<Void> createItem(String databaseId, String containerId,
                                  String partitionKey, String jsonData) {
        return Mono.<Void>fromCallable(() -> {
            syncClient.createItem(databaseId, containerId, partitionKey, jsonData);
            return null;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Creates an item and returns a {@link java.util.concurrent.CompletableFuture}.
     */
    public java.util.concurrent.CompletableFuture<Void> createItemAsync(
            String databaseId, String containerId, String partitionKey, String jsonData) {
        return createItem(databaseId, containerId, partitionKey, jsonData).toFuture();
    }

    @Override
    public void close() {
        syncClient.close();
    }
}
