// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos;

/**
 * Native bridge to the azure_data_cosmos_native C-ABI layer.
 *
 * <p>This class declares the {@code native} methods that map to {@code cosmos_*} C functions
 * exported by {@code libazurecosmos}. Methods are public for cross-package access by
 * JNI/JNA strategy implementations.</p>
 *
 * <p>Error handling: every C-ABI function returns a {@code CosmosErrorCode} (int).
 * 0 = success, non-zero = error. The JNI glue converts non-zero into
 * {@link CosmosException} before returning to Java.</p>
 */
public final class CosmosNativeBridge {

    static {
        NativeLoader.load();
    }

    private CosmosNativeBridge() {}

    // ── Runtime lifecycle ──────────────────────────────────────────────

    public static native long nativeRuntimeContextCreate();

    public static native void nativeRuntimeContextFree(long runtimeContext);

    // ── Call context lifecycle ──────────────────────────────────────────

    public static native long nativeCallContextCreate(long runtimeContext, boolean includeErrorDetails);

    public static native void nativeCallContextFree(long callContext);

    // ── Client lifecycle ───────────────────────────────────────────────

    public static native long nativeClientCreateWithKey(
            long callContext,
            String endpoint,
            String key) throws CosmosException;

    public static native void nativeClientFree(long client);

    // ── Navigation ─────────────────────────────────────────────────────

    public static native long nativeDatabaseClient(
            long callContext,
            long client,
            String databaseId) throws CosmosException;

    public static native void nativeDatabaseFree(long database);

    public static native long nativeContainerClient(
            long callContext,
            long database,
            String containerId) throws CosmosException;

    public static native void nativeContainerFree(long container);

    // ── Item CRUD ──────────────────────────────────────────────────────

    public static native void nativeCreateItem(
            long callContext,
            long container,
            String partitionKey,
            String jsonData) throws CosmosException;

    public static native String nativeReadItem(
            long callContext,
            long container,
            String partitionKey,
            String itemId) throws CosmosException;

    public static native void nativeUpsertItem(
            long callContext,
            long container,
            String partitionKey,
            String jsonData) throws CosmosException;

    public static native void nativeDeleteItem(
            long callContext,
            long container,
            String partitionKey,
            String itemId) throws CosmosException;
}
