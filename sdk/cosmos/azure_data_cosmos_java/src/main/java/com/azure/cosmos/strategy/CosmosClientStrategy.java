package com.azure.cosmos.strategy;

import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemResponse;

/**
 * Strategy interface for swapping FFI backends (JNI, JNA, flapigen).
 * Select backend via system property: -Dlibcosmos.backend=jni|jna|flapigen
 */
public interface CosmosClientStrategy extends AutoCloseable {

    void createItem(String databaseId, String containerId,
                    String partitionKey, String jsonData) throws CosmosException;

    CosmosItemResponse readItem(String databaseId, String containerId,
                                 String itemId, String partitionKey) throws CosmosException;

    void upsertItem(String databaseId, String containerId,
                    String partitionKey, String jsonData) throws CosmosException;

    void deleteItem(String databaseId, String containerId,
                    String partitionKey, String itemId) throws CosmosException;

    @Override
    void close();

    static CosmosClientStrategy create(String endpoint, String key) throws CosmosException {
        String backend = System.getProperty("libcosmos.backend", "jni");
        return switch (backend) {
            case "jna" -> new com.azure.cosmos.jna.CosmosJnaClient(endpoint, key);
            case "jni" -> new com.azure.cosmos.jni.CosmosJniClient(endpoint, key);
            default -> throw new IllegalArgumentException("Unknown backend: " + backend
                    + ". Use -Dlibcosmos.backend=jni|jna");
        };
    }
}
