package com.azure.cosmos.jna;

import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.strategy.CosmosClientStrategy;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

/**
 * JNA-based sync Cosmos client. Calls cosmos_* C-ABI symbols directly via JNA reflection.
 * No native glue binary needed — JNA handles marshalling at runtime.
 *
 * ~100x slower per-call than JNI due to reflection overhead, but zero native build complexity.
 */
public class CosmosJnaClient implements CosmosClientStrategy {

    private static final CosmosLibrary LIB = CosmosLibrary.INSTANCE;

    private final Pointer runtimeContext;
    private final Pointer callContext;
    private final Pointer clientHandle;
    private volatile boolean closed = false;

    public CosmosJnaClient(String endpoint, String key) throws CosmosException {
        this.runtimeContext = LIB.cosmos_runtime_context_create(null, null);
        if (runtimeContext == null) {
            throw new CosmosException(999, "Failed to create runtime context");
        }
        this.callContext = LIB.cosmos_call_context_create(runtimeContext, null);
        if (callContext == null) {
            LIB.cosmos_runtime_context_free(runtimeContext);
            throw new CosmosException(999, "Failed to create call context");
        }

        PointerByReference outClient = new PointerByReference();
        int rc = LIB.cosmos_client_create_with_key(callContext, endpoint, key, null, outClient);
        if (rc != 0) {
            LIB.cosmos_call_context_free(callContext);
            LIB.cosmos_runtime_context_free(runtimeContext);
            throw new CosmosException(rc, "Failed to create Cosmos client");
        }
        this.clientHandle = outClient.getValue();
    }

    @Override
    public void createItem(String databaseId, String containerId,
                           String partitionKey, String jsonData) throws CosmosException {
        ensureOpen();
        Pointer container = getContainerHandle(databaseId, containerId);
        try {
            int rc = LIB.cosmos_container_create_item(callContext, container,
                    partitionKey, jsonData, null);
            checkError(rc, "createItem");
        } finally {
            LIB.cosmos_container_free(container);
        }
    }

    @Override
    public CosmosItemResponse readItem(String databaseId, String containerId,
                                        String itemId, String partitionKey) throws CosmosException {
        ensureOpen();
        Pointer container = getContainerHandle(databaseId, containerId);
        try {
            PointerByReference outJson = new PointerByReference();
            int rc = LIB.cosmos_container_read_item(callContext, container,
                    partitionKey, itemId, null, outJson);
            checkError(rc, "readItem");

            Pointer jsonPtr = outJson.getValue();
            String json = (jsonPtr != null) ? jsonPtr.getString(0) : "";
            if (jsonPtr != null) {
                LIB.cosmos_string_free(jsonPtr);
            }
            return new CosmosItemResponse(json);
        } finally {
            LIB.cosmos_container_free(container);
        }
    }

    @Override
    public void upsertItem(String databaseId, String containerId,
                           String partitionKey, String jsonData) throws CosmosException {
        ensureOpen();
        Pointer container = getContainerHandle(databaseId, containerId);
        try {
            int rc = LIB.cosmos_container_upsert_item(callContext, container,
                    partitionKey, jsonData, null);
            checkError(rc, "upsertItem");
        } finally {
            LIB.cosmos_container_free(container);
        }
    }

    @Override
    public void deleteItem(String databaseId, String containerId,
                           String partitionKey, String itemId) throws CosmosException {
        ensureOpen();
        Pointer container = getContainerHandle(databaseId, containerId);
        try {
            int rc = LIB.cosmos_container_delete_item(callContext, container,
                    partitionKey, itemId, null);
            checkError(rc, "deleteItem");
        } finally {
            LIB.cosmos_container_free(container);
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            LIB.cosmos_client_free(clientHandle);
            LIB.cosmos_call_context_free(callContext);
            LIB.cosmos_runtime_context_free(runtimeContext);
        }
    }

    private Pointer getContainerHandle(String databaseId, String containerId) throws CosmosException {
        PointerByReference outDb = new PointerByReference();
        int rc = LIB.cosmos_client_database_client(callContext, clientHandle, databaseId, outDb);
        checkError(rc, "databaseClient");
        Pointer db = outDb.getValue();

        try {
            PointerByReference outCtr = new PointerByReference();
            rc = LIB.cosmos_database_container_client(callContext, db, containerId, outCtr);
            checkError(rc, "containerClient");
            return outCtr.getValue();
        } finally {
            LIB.cosmos_database_free(db);
        }
    }

    private void checkError(int rc, String operation) throws CosmosException {
        if (rc != 0) {
            throw new CosmosException(rc, operation + " failed with error code " + rc);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("CosmosJnaClient is closed");
        }
    }
}
