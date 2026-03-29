fn main() {
    // Link against the pre-built C-ABI library at the OS level.
    // This is the ONLY interface to Layer 2 — no Cargo dependency on Rust internals.
    //
    // At build time, libazurecosmos.so must be on the library search path.
    // Set AZURECOSMOS_LIB_DIR to override the search path:
    //   AZURECOSMOS_LIB_DIR=/path/to/lib cargo build
    if let Ok(lib_dir) = std::env::var("AZURECOSMOS_LIB_DIR") {
        println!("cargo:rustc-link-search=native={}", lib_dir);
    }
    println!("cargo:rustc-link-lib=dylib=azurecosmos");
}
