// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

//! C-ABI declarations for `libazurecosmos.so`.
//!
//! These are the ONLY symbols this crate calls from Layer 2.
//! Declared manually from `azurecosmos.h` — no Cargo dependency on the native crate.

use std::ffi::{c_char, c_void};

#[repr(C)]
pub struct CallContextOptions {
    pub include_error_details: bool,
}

#[repr(C)]
#[derive(Default)]
pub struct CosmosError {
    pub code: i32,
    pub message: *const c_char,
    pub detail: *const c_char,
}

extern "C" {
    // ── Runtime lifecycle ──────────────────────────────────────────────
    pub fn cosmos_runtime_context_create(
        options: *const c_void,
        out_error: *mut CosmosError,
    ) -> *mut c_void;

    pub fn cosmos_runtime_context_free(ctx: *mut c_void);

    // ── Call context lifecycle ──────────────────────────────────────────
    pub fn cosmos_call_context_create(
        runtime_context: *const c_void,
        options: *const CallContextOptions,
    ) -> *mut c_void;

    pub fn cosmos_call_context_free(ctx: *mut c_void);

    // ── Client lifecycle ───────────────────────────────────────────────
    pub fn cosmos_client_create_with_key(
        ctx: *mut c_void,
        endpoint: *const c_char,
        key: *const c_char,
        options: *const c_void,
        out_client: *mut *mut c_void,
    ) -> i32;

    pub fn cosmos_client_free(client: *mut c_void);

    // ── Navigation ─────────────────────────────────────────────────────
    pub fn cosmos_client_database_client(
        ctx: *mut c_void,
        client: *const c_void,
        database_id: *const c_char,
        out_database: *mut *mut c_void,
    ) -> i32;

    pub fn cosmos_database_free(database: *mut c_void);

    pub fn cosmos_database_container_client(
        ctx: *mut c_void,
        database: *const c_void,
        container_id: *const c_char,
        out_container: *mut *mut c_void,
    ) -> i32;

    pub fn cosmos_container_free(container: *mut c_void);

    // ── Item CRUD ──────────────────────────────────────────────────────
    pub fn cosmos_container_create_item(
        ctx: *mut c_void,
        container: *const c_void,
        partition_key: *const c_char,
        json_data: *const c_char,
        options: *const c_void,
    ) -> i32;

    pub fn cosmos_container_read_item(
        ctx: *mut c_void,
        container: *const c_void,
        partition_key: *const c_char,
        item_id: *const c_char,
        options: *const c_void,
        out_json: *mut *const c_char,
    ) -> i32;

    pub fn cosmos_container_upsert_item(
        ctx: *mut c_void,
        container: *const c_void,
        partition_key: *const c_char,
        json_data: *const c_char,
        options: *const c_void,
    ) -> i32;

    pub fn cosmos_container_delete_item(
        ctx: *mut c_void,
        container: *const c_void,
        partition_key: *const c_char,
        item_id: *const c_char,
        options: *const c_void,
    ) -> i32;

    // ── String cleanup ─────────────────────────────────────────────────
    pub fn cosmos_string_free(str: *const c_char);
}
