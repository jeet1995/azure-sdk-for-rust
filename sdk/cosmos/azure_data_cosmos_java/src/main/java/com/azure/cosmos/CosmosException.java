// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos;

/**
 * Exception thrown when a Cosmos DB operation fails.
 *
 * <p>Maps to {@code CosmosErrorCode} from the native C-ABI layer.
 * This is a checked exception — database errors are recoverable
 * and callers must handle them.</p>
 */
public class CosmosException extends Exception {

    private final int errorCode;

    public CosmosException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CosmosException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Returns the native {@code CosmosErrorCode} value.
     * 0 = success (should never appear in an exception),
     * 404 = NotFound, 409 = Conflict, 429 = TooManyRequests, etc.
     */
    public int getErrorCode() {
        return errorCode;
    }

    public boolean isNotFound() {
        return errorCode == 404;
    }

    public boolean isConflict() {
        return errorCode == 409;
    }

    public boolean isThrottled() {
        return errorCode == 429;
    }
}
