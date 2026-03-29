// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos;

import com.azure.cosmos.models.CosmosItemResponse;

/**
 * Synchronous Cosmos DB client that wraps the native C-ABI layer via JNI.
 *
 * <p>Usage:
 * <pre>{@code
 * try (CosmosClient client = CosmosClient.createWithKey(endpoint, key)) {
 *     CosmosItemResponse response = client.readItem("myDb", "myContainer", "doc1", "pk1");
 *     System.out.println(response.getBody());
 *
 *     client.createItem("myDb", "myContainer", "pk1", "{\"id\":\"doc2\",\"pk\":\"pk1\"}");
 * }
 * }</pre>
 *
 * <p>Thread safety: instances are thread-safe. The underlying native handles are
 * reference-counted and safe to share across threads.</p>
 */
public final class CosmosClient implements AutoCloseable {

    private final long runtimeContext;
    private final long callContext;
    private final long clientHandle;
    private volatile boolean closed = false;

    private CosmosClient(long runtimeContext, long callContext, long clientHandle) {
        this.runtimeContext = runtimeContext;
        this.callContext = callContext;
        this.clientHandle = clientHandle;
    }

    /**
     * Creates a new CosmosClient authenticated with a master key.
     *
     * @param endpoint the Cosmos DB account endpoint URL
     * @param key      the Cosmos DB account master key
     * @return a new CosmosClient
     * @throws CosmosException if connection fails
     */
    public static CosmosClient createWithKey(String endpoint, String key) throws CosmosException {
        long runtime = CosmosNativeBridge.nativeRuntimeContextCreate();
        long ctx = CosmosNativeBridge.nativeCallContextCreate(runtime, true);
        long client = CosmosNativeBridge.nativeClientCreateWithKey(ctx, endpoint, key);
        return new CosmosClient(runtime, ctx, client);
    }

    /**
     * Reads an item from the specified container.
     *
     * @param databaseId   the database name
     * @param containerId  the container name
     * @param itemId       the item ID
     * @param partitionKey the partition key value
     * @return the item response containing the raw JSON body
     * @throws CosmosException if the read fails (e.g., 404 NotFound)
     */
    public CosmosItemResponse readItem(String databaseId, String containerId,
                                       String itemId, String partitionKey) throws CosmosException {
        ensureOpen();
        long db = CosmosNativeBridge.nativeDatabaseClient(callContext, clientHandle, databaseId);
        try {
            long container = CosmosNativeBridge.nativeContainerClient(callContext, db, containerId);
            try {
                String json = CosmosNativeBridge.nativeReadItem(
                        callContext, container, partitionKey, itemId);
                return new CosmosItemResponse(json);
            } finally {
                CosmosNativeBridge.nativeContainerFree(container);
            }
        } finally {
            CosmosNativeBridge.nativeDatabaseFree(db);
        }
    }

    /**
     * Creates a new item in the specified container.
     *
     * @param databaseId   the database name
     * @param containerId  the container name
     * @param partitionKey the partition key value
     * @param jsonData     the item as a raw JSON string (must include "id" field)
     * @throws CosmosException if the create fails (e.g., 409 Conflict)
     */
    public void createItem(String databaseId, String containerId,
                           String partitionKey, String jsonData) throws CosmosException {
        ensureOpen();
        long db = CosmosNativeBridge.nativeDatabaseClient(callContext, clientHandle, databaseId);
        try {
            long container = CosmosNativeBridge.nativeContainerClient(callContext, db, containerId);
            try {
                CosmosNativeBridge.nativeCreateItem(callContext, container, partitionKey, jsonData);
            } finally {
                CosmosNativeBridge.nativeContainerFree(container);
            }
        } finally {
            CosmosNativeBridge.nativeDatabaseFree(db);
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            CosmosNativeBridge.nativeClientFree(clientHandle);
            CosmosNativeBridge.nativeCallContextFree(callContext);
            CosmosNativeBridge.nativeRuntimeContextFree(runtimeContext);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("CosmosClient is closed");
        }
    }
}
