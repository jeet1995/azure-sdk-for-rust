package com.azure.cosmos.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

/**
 * JNA interface mapping to cosmos_* C-ABI symbols from libazurecosmos.so.
 * JNA loads the library and resolves symbols via reflection at runtime.
 */
public interface CosmosLibrary extends Library {

    CosmosLibrary INSTANCE = Native.load("azurecosmos", CosmosLibrary.class);

    // Runtime lifecycle
    Pointer cosmos_runtime_context_create(Pointer options, Pointer outError);
    void cosmos_runtime_context_free(Pointer ctx);

    // Call context lifecycle
    Pointer cosmos_call_context_create(Pointer runtimeContext, Pointer options);
    void cosmos_call_context_free(Pointer ctx);

    // Client lifecycle
    int cosmos_client_create_with_key(Pointer ctx, String endpoint, String key,
                                       Pointer options, PointerByReference outClient);
    void cosmos_client_free(Pointer client);

    // Navigation
    int cosmos_client_database_client(Pointer ctx, Pointer client, String databaseId,
                                       PointerByReference outDatabase);
    void cosmos_database_free(Pointer database);

    int cosmos_database_container_client(Pointer ctx, Pointer database, String containerId,
                                          PointerByReference outContainer);
    void cosmos_container_free(Pointer container);

    // Item CRUD
    int cosmos_container_create_item(Pointer ctx, Pointer container,
                                      String partitionKey, String jsonData, Pointer options);

    int cosmos_container_read_item(Pointer ctx, Pointer container,
                                    String partitionKey, String itemId,
                                    Pointer options, PointerByReference outJson);

    int cosmos_container_upsert_item(Pointer ctx, Pointer container,
                                      String partitionKey, String jsonData, Pointer options);

    int cosmos_container_delete_item(Pointer ctx, Pointer container,
                                      String partitionKey, String itemId, Pointer options);

    // String cleanup
    void cosmos_string_free(Pointer str);

    // Version
    Pointer cosmos_version();
}
