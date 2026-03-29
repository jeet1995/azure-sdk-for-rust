// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos;

/**
 * Native bridge to the azure_data_cosmos_native C-ABI layer.
 *
 * <p>This class declares the {@code native} methods that map to {@code cosmos_*} C functions
 * exported by {@code libazurecosmos}. All methods are package-private — callers use
 * {@link CosmosClient} or {@link CosmosReactiveClient} instead.</p>
 *
 * <p>Error handling: every C-ABI function returns a {@code CosmosErrorCode} (int).
 * 0 = success, non-zero = error. The JNI glue converts non-zero into
 * {@link CosmosException} before returning to Java.</p>
 */
final class CosmosNativeBridge {

    static {
        System.loadLibrary("azurecosmos");
        System.loadLibrary("azurecosmos_jni");
    }

    private CosmosNativeBridge() {}

    // ── Runtime lifecycle ──────────────────────────────────────────────

    static native long nativeRuntimeContextCreate();

    static native void nativeRuntimeContextFree(long runtimeContext);

    // ── Call context lifecycle ──────────────────────────────────────────

    static native long nativeCallContextCreate(long runtimeContext, boolean includeErrorDetails);

    static native void nativeCallContextFree(long callContext);

    // ── Client lifecycle ───────────────────────────────────────────────

    static native long nativeClientCreateWithKey(
            long callContext,
            String endpoint,
            String key) throws CosmosException;

    static native void nativeClientFree(long client);

    // ── Navigation ─────────────────────────────────────────────────────

    static native long nativeDatabaseClient(
            long callContext,
            long client,
            String databaseId) throws CosmosException;

    static native void nativeDatabaseFree(long database);

    static native long nativeContainerClient(
            long callContext,
            long database,
            String containerId) throws CosmosException;

    static native void nativeContainerFree(long container);

    // ── Item CRUD ──────────────────────────────────────────────────────

    static native void nativeCreateItem(
            long callContext,
            long container,
            String partitionKey,
            String jsonData) throws CosmosException;

    static native String nativeReadItem(
            long callContext,
            long container,
            String partitionKey,
            String itemId) throws CosmosException;

    static native void nativeUpsertItem(
            long callContext,
            long container,
            String partitionKey,
            String jsonData) throws CosmosException;

    static native void nativeDeleteItem(
            long callContext,
            long container,
            String partitionKey,
            String itemId) throws CosmosException;
}
