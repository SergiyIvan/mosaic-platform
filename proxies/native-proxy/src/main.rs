use axum::{
    extract::{State, Query},
    routing::post,
    Json, Router,
};
use serde::{Deserialize, Serialize};
use std::sync::{Arc, Mutex};
use std::fs::File;
use std::io::Write;
use libloading::{Library, Symbol};

// The FFI function signature our functions must implement.
type InvokeFunc = unsafe extern "C" fn(
    input_ptr: *const u8,
    input_len: usize,
    out_ptr: *mut u8,
    max_out_len: usize,
) -> usize;

struct ProxyState {
    // Stores the dynamically loaded library.
    // Wrapping it in a Mutex enforces strict sequential execution.
    loaded_lib: Option<Library>,
}

#[derive(Deserialize)]
struct InitParams {
    url: String,
}

#[derive(Serialize)]
struct GenericResponse {
    status: String,
    message: String,
}

async fn init_handler(
    State(state): State<Arc<Mutex<ProxyState>>>,
    Query(params): Query<InitParams>,
) -> Json<GenericResponse> {
    let mut state_lock = state.lock().unwrap();

    if state_lock.loaded_lib.is_some() {
        return Json(GenericResponse {
            status: "error".to_string(),
            message: "A function is already registered in this proxy instance.".to_string(),
        });
    }

    // Download the binary.
    let download_path = "/tmp/func.so";
    let response = match reqwest::blocking::get(&params.url) {
        Ok(r) => r,
        Err(e) => return Json(GenericResponse {
            status: "error".to_string(),
            message: format!("Failed to download from URL: {}", e),
        }),
    };

    let mut file = File::create(download_path).unwrap();
    let content = response.bytes().unwrap();
    file.write_all(&content).unwrap();

    // Dynamically load the .so file.
    unsafe {
        match Library::new(download_path) {
            Ok(lib) => {
                state_lock.loaded_lib = Some(lib);
                Json(GenericResponse {
                    status: "success".to_string(),
                    message: "Function registered and loaded successfully.".to_string(),
                })
            }
            Err(e) => Json(GenericResponse {
                status: "error".to_string(),
                message: format!("Failed to load library: {}", e),
            }),
        }
    }
}

async fn run_handler(
    State(state): State<Arc<Mutex<ProxyState>>>,
    body: String, // Accept raw JSON string.
) -> Json<GenericResponse> {
    // The Mutex blocks concurrent requests, enforcing sequential execution.
    let state_lock = state.lock().unwrap();

    let lib = match &state_lock.loaded_lib {
        Some(l) => l,
        None => return Json(GenericResponse { status: "error".to_string(), message: format!("No function initialized.") })
    };

    let mut out_buf = vec![0u8; 1024]; // 1KB output buffer.

    let bytes_written = unsafe {
        // Load the 'run_proxy' symbol from the function library.
        let func: Symbol<InvokeFunc> = match lib.get(b"run_proxy") {
            Ok(f) => f,
            Err(_) => return Json(GenericResponse { status: "error".to_string(), message: format!("Function missing 'run_proxy' entrypoint.") }),
        };

        // Call the dynamically loaded function entrypoint.
        func(
            body.as_ptr(),
            body.len(),
            out_buf.as_mut_ptr(),
            out_buf.len(),
        )
    };

    if bytes_written == 0 {
        return Json(GenericResponse { status: "error".to_string(), message: format!("Function execution failed or returned 0 bytes.") });
    }

    // Parse the returned bytes back into JSON to send to the client.
    let result_str = std::str::from_utf8(&out_buf[..bytes_written]).unwrap();

    Json(GenericResponse { status: "success".to_string(), message: result_str.to_string() })
}

#[tokio::main]
async fn main() {
    let state = Arc::new(Mutex::new(ProxyState { loaded_lib: None }));

    let app = Router::new()
        .route("/init", post(init_handler))
        .route("/run", post(run_handler))
        .with_state(state);

    println!("Native proxy listening on 127.0.0.1:8080");
    axum::Server::bind(&"127.0.0.1:8080".parse().unwrap())
        .serve(app.into_make_service())
        .await
        .unwrap();
}
