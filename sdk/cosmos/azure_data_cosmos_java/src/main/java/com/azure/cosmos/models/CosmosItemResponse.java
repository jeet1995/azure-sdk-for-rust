// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos.models;

/**
 * Represents a response from a Cosmos DB item read operation.
 *
 * <p>The body is raw JSON (as a String) because the driver is schema-agnostic.
 * Callers deserialize with Jackson, Gson, or any JSON library.</p>
 */
public final class CosmosItemResponse {

    private final String body;

    public CosmosItemResponse(String body) {
        this.body = body;
    }

    /**
     * Returns the item body as a raw JSON string.
     */
    public String getBody() {
        return body;
    }

    /**
     * Deserializes the body into the given type using Jackson.
     *
     * @param <T>   the target type
     * @param clazz the class to deserialize into
     * @return the deserialized object
     * @throws IllegalArgumentException if deserialization fails
     */
    public <T> T getItem(Class<T> clazz) {
        try {
            // Users provide their own ObjectMapper; this is a convenience sketch
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(body, clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize response body", e);
        }
    }
}
