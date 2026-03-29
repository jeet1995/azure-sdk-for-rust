// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos.jni;

import com.azure.cosmos.CosmosException;
import com.azure.cosmos.CosmosNativeBridge;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.strategy.CosmosClientStrategy;

/**
 * JNI-based sync Cosmos client. Calls through the Rust JNI glue crate
 * (libazurecosmos_jni.so) which translates Java_* symbols to cosmos_* C-ABI calls.
 *
 * <p>~10-50ns per FFI crossing — production path.</p>
 */
public class CosmosJniClient implements CosmosClientStrategy {

    private final long runtimeContext;
    private final long callContext;
    private final long clientHandle;
    private volatile boolean closed = false;

    public CosmosJniClient(String endpoint, String key) throws CosmosException {
        this.runtimeContext = CosmosNativeBridge.nativeRuntimeContextCreate();
        this.callContext = CosmosNativeBridge.nativeCallContextCreate(runtimeContext, true);
        this.clientHandle = CosmosNativeBridge.nativeClientCreateWithKey(callContext, endpoint, key);
    }

    @Override
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
    public CosmosItemResponse readItem(String databaseId, String containerId,
                                        String itemId, String partitionKey) throws CosmosException {
        ensureOpen();
        long db = CosmosNativeBridge.nativeDatabaseClient(callContext, clientHandle, databaseId);
        try {
            long container = CosmosNativeBridge.nativeContainerClient(callContext, db, containerId);
            try {
                String json = CosmosNativeBridge.nativeReadItem(callContext, container, partitionKey, itemId);
                return new CosmosItemResponse(json);
            } finally {
                CosmosNativeBridge.nativeContainerFree(container);
            }
        } finally {
            CosmosNativeBridge.nativeDatabaseFree(db);
        }
    }

    @Override
    public void upsertItem(String databaseId, String containerId,
                           String partitionKey, String jsonData) throws CosmosException {
        ensureOpen();
        long db = CosmosNativeBridge.nativeDatabaseClient(callContext, clientHandle, databaseId);
        try {
            long container = CosmosNativeBridge.nativeContainerClient(callContext, db, containerId);
            try {
                CosmosNativeBridge.nativeUpsertItem(callContext, container, partitionKey, jsonData);
            } finally {
                CosmosNativeBridge.nativeContainerFree(container);
            }
        } finally {
            CosmosNativeBridge.nativeDatabaseFree(db);
        }
    }

    @Override
    public void deleteItem(String databaseId, String containerId,
                           String partitionKey, String itemId) throws CosmosException {
        ensureOpen();
        long db = CosmosNativeBridge.nativeDatabaseClient(callContext, clientHandle, databaseId);
        try {
            long container = CosmosNativeBridge.nativeContainerClient(callContext, db, containerId);
            try {
                CosmosNativeBridge.nativeDeleteItem(callContext, container, partitionKey, itemId);
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
            throw new IllegalStateException("CosmosJniClient is closed");
        }
    }
}
