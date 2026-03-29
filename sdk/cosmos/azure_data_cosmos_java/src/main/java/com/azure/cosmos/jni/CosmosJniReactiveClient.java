// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos.jni;

import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;

/**
 * Reactor-first async wrapper over the JNI sync client.
 * Mono.fromCallable + Schedulers.boundedElastic() ensures blocking JNI calls
 * never run on Reactor's event loops.
 */
public class CosmosJniReactiveClient implements AutoCloseable {

    private final CosmosJniClient syncClient;

    public CosmosJniReactiveClient(String endpoint, String key) throws CosmosException {
        this.syncClient = new CosmosJniClient(endpoint, key);
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
