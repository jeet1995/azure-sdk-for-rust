package com.azure.cosmos.jna;

import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;

/**
 * Reactor-first async wrapper over the JNA sync client.
 * Same pattern as the JNI reactive client — Mono.fromCallable + boundedElastic.
 */
public class CosmosJnaReactiveClient implements AutoCloseable {

    private final CosmosJnaClient syncClient;

    public CosmosJnaReactiveClient(String endpoint, String key) throws CosmosException {
        this.syncClient = new CosmosJnaClient(endpoint, key);
    }

    public Mono<CosmosItemResponse> readItem(String databaseId, String containerId,
                                              String itemId, String partitionKey) {
        return Mono.fromCallable(
                () -> syncClient.readItem(databaseId, containerId, itemId, partitionKey)
        ).subscribeOn(Schedulers.boundedElastic());
    }

    public CompletableFuture<CosmosItemResponse> readItemAsync(
            String databaseId, String containerId, String itemId, String partitionKey) {
        return readItem(databaseId, containerId, itemId, partitionKey).toFuture();
    }

    public Mono<Void> createItem(String databaseId, String containerId,
                                  String partitionKey, String jsonData) {
        return Mono.<Void>fromCallable(() -> {
            syncClient.createItem(databaseId, containerId, partitionKey, jsonData);
            return null;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public CompletableFuture<Void> createItemAsync(
            String databaseId, String containerId, String partitionKey, String jsonData) {
        return createItem(databaseId, containerId, partitionKey, jsonData).toFuture();
    }

    @Override
    public void close() {
        syncClient.close();
    }
}
