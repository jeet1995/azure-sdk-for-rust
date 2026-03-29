// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

//! JNI glue layer for azure_data_cosmos_native.
//!
//! This crate bridges Java `native` methods (JNI calling convention) to
//! `cosmos_*` C-ABI functions exported by `libazurecosmos.so`.
//!
//! It makes two kinds of C-ABI calls and nothing else:
//! - **Left**: JNI functions via `JNIEnv*` (construct Java objects, throw exceptions)
//! - **Right**: Cosmos functions via `extern "C"` (the Layer 2 C-ABI)

mod ffi;

use jni::objects::{JClass, JString, JObject};
use jni::sys::{jlong, jboolean};
use jni::JNIEnv;
use std::ffi::{c_char, c_void, CString};
use std::ptr;

// ═══════════════════════════════════════════════════════════════════════
// Helper: convert a Java String to a CString, or throw and return null
// ═══════════════════════════════════════════════════════════════════════

fn java_string_to_cstring(env: &mut JNIEnv, s: &JString) -> Option<CString> {
    let java_str: String = match env.get_string(s) {
        Ok(s) => s.into(),
        Err(_) => {
            let _ = env.throw_new(
                "com/azure/cosmos/CosmosException",
                "Failed to read Java string",
            );
            return None;
        }
    };
    match CString::new(java_str) {
        Ok(c) => Some(c),
        Err(_) => {
            let _ = env.throw_new(
                "com/azure/cosmos/CosmosException",
                "String contains NUL byte",
            );
            None
        }
    }
}

fn check_error_code(env: &mut JNIEnv, code: i32) -> bool {
    if code == 0 {
        return true; // success
    }
    let msg = format!("Cosmos operation failed with error code {}", code);
    let _ = env.throw_new("com/azure/cosmos/CosmosException", &msg);
    false
}

// ═══════════════════════════════════════════════════════════════════════
// Runtime lifecycle
// ═══════════════════════════════════════════════════════════════════════

#[no_mangle]
pub extern "system" fn Java_com_azure_cosmos_CosmosNativeBridge_nativeRuntimeContextCreate(
    _env: JNIEnv,
    _class: JClass,
) -> jlong {
    let mut out_error = ffi::CosmosError::default();
    let ctx = unsafe { ffi::cosmos_runtime_context_create(ptr::null(), &mut out_error) };
    ctx as jlong
}

#[no_mangle]
pub extern "system" fn Java_com_azure_cosmos_CosmosNativeBridge_nativeRuntimeContextFree(
    _env: JNIEnv,
    _class: JClass,
    runtime_context: jlong,
) {
    if runtime_context != 0 {
        unsafe { ffi::cosmos_runtime_context_free(runtime_context as *mut c_void) };
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Call context lifecycle
// ═══════════════════════════════════════════════════════════════════════

#[no_mangle]
pub extern "system" fn Java_com_azure_cosmos_CosmosNativeBridge_nativeCallContextCreate(
    _env: JNIEnv,
    _class: JClass,
    runtime_context: jlong,
    include_error_details: jboolean,
) -> jlong {
    let options = ffi::CallContextOptions {
        include_error_details: include_error_details != 0,
    };
    let ctx = unsafe {
        ffi::cosmos_call_context_create(runtime_context as *const c_void, &options)
    };
    ctx as jlong
}

#[no_mangle]
pub extern "system" fn Java_com_azure_cosmos_CosmosNativeBridge_nativeCallContextFree(
    _env: JNIEnv,
    _class: JClass,
    call_context: jlong,
) {
    if call_context != 0 {
        unsafe { ffi::cosmos_call_context_free(call_context as *mut c_void) };
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Client lifecycle
// ═══════════════════════════════════════════════════════════════════════

#[no_mangle]
pub extern "system" fn Java_com_azure_cosmos_CosmosNativeBridge_nativeClientCreateWithKey<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    call_context: jlong,
    endpoint: JString<'local>,
    key: JString<'local>,
) -> jlong {
    let endpoint_c = match java_string_to_cstring(&mut env, &endpoint) {
        Some(c) => c,
        None => return 0,
    };
    let key_c = match java_string_to_cstring(&mut env, &key) {
        Some(c) => c,
        None => return 0,
    };

    let mut out_client: *mut c_void = ptr::null_mut();
    let rc = unsafe {
        ffi::cosmos_client_create_with_key(
            call_context as *mut c_void,
            endpoint_c.as_ptr(),
            key_c.as_ptr(),
            ptr::null(),
            &mut out_client,
        )
    };

    if !check_error_code(&mut env, rc) {
        return 0;
    }
    out_client as jlong
}

#[no_mangle]
pub extern "system" fn Java_com_azure_cosmos_CosmosNativeBridge_nativeClientFree(
    _env: JNIEnv,
    _class: JClass,
    client: jlong,
) {
    if client != 0 {
        unsafe { ffi::cosmos_client_free(client as *mut c_void) };
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Navigation: Client → Database → Container
// ═══════════════════════════════════════════════════════════════════════

#[no_mangle]
pub extern "system" fn Java_com_azure_cosmos_CosmosNativeBridge_nativeDatabaseClient<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    call_context: jlong,
    client: jlong,
    database_id: JString<'local>,
) -> jlong {
    let db_c = match java_string_to_cstring(&mut env, &database_id) {
        Some(c) => c,
        None => return 0,
    };

    let mut out_db: *mut c_void = ptr::null_mut();
    let rc = unsafe {
        ffi::cosmos_client_database_client(
            call_context as *mut c_void,
            client as *const c_void,
            db_c.as_ptr(),
            &mut out_db,
        )
    };

    if !check_error_code(&mut env, rc) {
        return 0;
    }
    out_db as jlong
}

#[no_mangle]
pub extern "system" fn Java_com_azure_cosmos_CosmosNativeBridge_nativeDatabaseFree(
    _env: JNIEnv,
    _class: JClass,
    database: jlong,
) {
    if database != 0 {
        unsafe { ffi::cosmos_database_free(database as *mut c_void) };
    }
}

#[no_mangle]
pub extern "system" fn Java_com_azure_cosmos_CosmosNativeBridge_nativeContainerClient<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    call_context: jlong,
    database: jlong,
    container_id: JString<'local>,
) -> jlong {
    let ctr_c = match java_string_to_cstring(&mut env, &container_id) {
        Some(c) => c,
        None => return 0,
    };

    let mut out_ctr: *mut c_void = ptr::null_mut();
    let rc = unsafe {
        ffi::cosmos_database_container_client(
            call_context as *mut c_void,
            database as *const c_void,
            ctr_c.as_ptr(),
            &mut out_ctr,
        )
    };

    if !check_error_code(&mut env, rc) {
        return 0;
    }
    out_ctr as jlong
}

#[no_mangle]
pub extern "system" fn Java_com_azure_cosmos_CosmosNativeBridge_nativeContainerFree(
    _env: JNIEnv,
    _class: JClass,
    container: jlong,
) {
    if container != 0 {
        unsafe { ffi::cosmos_container_free(container as *mut c_void) };
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Item CRUD
// ═══════════════════════════════════════════════════════════════════════

#[no_mangle]
pub extern "system" fn Java_com_azure_cosmos_CosmosNativeBridge_nativeCreateItem<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    call_context: jlong,
    container: jlong,
    partition_key: JString<'local>,
    json_data: JString<'local>,
) {
    let pk_c = match java_string_to_cstring(&mut env, &partition_key) {
        Some(c) => c,
        None => return,
    };
    let json_c = match java_string_to_cstring(&mut env, &json_data) {
        Some(c) => c,
        None => return,
    };

    let rc = unsafe {
        ffi::cosmos_container_create_item(
            call_context as *mut c_void,
            container as *const c_void,
            pk_c.as_ptr(),
            json_c.as_ptr(),
            ptr::null(),
        )
    };

    check_error_code(&mut env, rc);
}

#[no_mangle]
pub extern "system" fn Java_com_azure_cosmos_CosmosNativeBridge_nativeReadItem<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    call_context: jlong,
    container: jlong,
    partition_key: JString<'local>,
    item_id: JString<'local>,
) -> JObject<'local> {
    let pk_c = match java_string_to_cstring(&mut env, &partition_key) {
        Some(c) => c,
        None => return JObject::null(),
    };
    let id_c = match java_string_to_cstring(&mut env, &item_id) {
        Some(c) => c,
        None => return JObject::null(),
    };

    let mut out_json: *const c_char = ptr::null();
    let rc = unsafe {
        ffi::cosmos_container_read_item(
            call_context as *mut c_void,
            container as *const c_void,
            pk_c.as_ptr(),
            id_c.as_ptr(),
            ptr::null(),
            &mut out_json,
        )
    };

    if !check_error_code(&mut env, rc) {
        return JObject::null();
    }

    // Convert C string → Java String
    let json_str = if out_json.is_null() {
        String::new()
    } else {
        let cstr = unsafe { std::ffi::CStr::from_ptr(out_json) };
        let s = cstr.to_string_lossy().into_owned();
        unsafe { ffi::cosmos_string_free(out_json) };
        s
    };

    match env.new_string(&json_str) {
        Ok(jstr) => JObject::from(jstr),
        Err(_) => {
            let _ = env.throw_new(
                "com/azure/cosmos/CosmosException",
                "Failed to create Java string from response",
            );
            JObject::null()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_azure_cosmos_CosmosNativeBridge_nativeUpsertItem<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    call_context: jlong,
    container: jlong,
    partition_key: JString<'local>,
    json_data: JString<'local>,
) {
    let pk_c = match java_string_to_cstring(&mut env, &partition_key) {
        Some(c) => c,
        None => return,
    };
    let json_c = match java_string_to_cstring(&mut env, &json_data) {
        Some(c) => c,
        None => return,
    };

    let rc = unsafe {
        ffi::cosmos_container_upsert_item(
            call_context as *mut c_void,
            container as *const c_void,
            pk_c.as_ptr(),
            json_c.as_ptr(),
            ptr::null(),
        )
    };

    check_error_code(&mut env, rc);
}

#[no_mangle]
pub extern "system" fn Java_com_azure_cosmos_CosmosNativeBridge_nativeDeleteItem<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    call_context: jlong,
    container: jlong,
    partition_key: JString<'local>,
    item_id: JString<'local>,
) {
    let pk_c = match java_string_to_cstring(&mut env, &partition_key) {
        Some(c) => c,
        None => return,
    };
    let id_c = match java_string_to_cstring(&mut env, &item_id) {
        Some(c) => c,
        None => return,
    };

    let rc = unsafe {
        ffi::cosmos_container_delete_item(
            call_context as *mut c_void,
            container as *const c_void,
            pk_c.as_ptr(),
            id_c.as_ptr(),
            ptr::null(),
        )
    };

    check_error_code(&mut env, rc);
}
