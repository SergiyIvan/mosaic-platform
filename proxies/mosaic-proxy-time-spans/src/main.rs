use axum::{extract::{State, Query}, routing::post, Json, Router};
use serde::{Deserialize, Serialize};
use std::sync::{Arc, Mutex};
use std::fs::File;
use std::io::Write;
use libloading::{Library, Symbol};
use wasmtime::{Engine, Module, Store, Linker, FuncType, ValType, Val};
use wasmtime_wasi::preview1::{self, WasiP1Ctx};
use wasmtime_wasi::p2::WasiCtxBuilder;


// The FFI signature of the universal trampoline function.
type TrampolineDispatch = unsafe extern "C" fn(
    *const u8, usize, *mut u8, usize, *const u64, usize, *mut u64
) -> bool;

struct ProxyState {
    engine: Engine,
    loaded_libs: Vec<Arc<Library>>,
    wasm_module: Option<Module>,
    linker: Option<Linker<ProxyModuleState>>,
}

#[derive(Deserialize)]
struct HostFuncDef {
    module: String,
    name: String,
    param_types: Vec<String>, // e.g., ["i32", "i32"].
}

#[derive(Deserialize)]
struct TrampolineDef {
    url: String,
    functions: Vec<HostFuncDef>,
}

#[derive(Deserialize)]
struct InitParams {
    wasm_url: String,
}

#[derive(Serialize)]
struct GenericResponse {
    status: String,
    message: String,
}

// Wasm state.
struct ProxyModuleState {
    wasi: WasiP1Ctx,
}

// Helper to convert JSON strings to Wasmtime types.
fn parse_valtype(t: &str) -> ValType {
    match t {
        "i32" => ValType::I32,
        "i64" => ValType::I64,
        "f32" => ValType::F32,
        "f64" => ValType::F64,
        _ => panic!("Unknown type"),
    }
}


async fn init_handler(
    State(state): State<Arc<Mutex<ProxyState>>>,
    Query(params): Query<InitParams>,
    Json(trampolines): Json<Vec<TrampolineDef>>,
) -> Json<GenericResponse> {
    let mut state_lock = state.lock().unwrap();

    if !state_lock.loaded_libs.is_empty() {
        return Json(GenericResponse { status: "error".into(), message: "Functions are already registered in this proxy instance.".into() });
    }

    // Download & init Wasm guest.
    let wasm_bytes = match reqwest::blocking::get(&params.wasm_url) {
        Ok(res) => res.bytes().unwrap(),
        Err(_) => return Json(GenericResponse { status: "error".into(), message: "Failed to download Wasm module.".into() }),
    };
    let module = Module::new(&state_lock.engine, &wasm_bytes).unwrap();

    // Build dynamic Linker.
    let mut linker: Linker<ProxyModuleState> = Linker::new(&state_lock.engine);
    preview1::add_to_linker_sync(&mut linker, |state| &mut state.wasi).unwrap();

    let mut loaded_libs = Vec::new();

    for (idx, tramp_def) in trampolines.into_iter().enumerate() {
        // Assign unique temp file for each library.
        let tramp_path = format!("/tmp/trampoline_{}.so", idx);

        let tramp_bytes = match reqwest::blocking::get(&tramp_def.url) {
            Ok(res) => res.bytes().unwrap(),
            Err(_) => return Json(GenericResponse { status: "error".into(), message: format!("Failed to download trampoline at {}.", tramp_def.url) }),
        };

        File::create(&tramp_path).unwrap().write_all(&tramp_bytes).unwrap();

        // Load lib and extract address of the trampoline function.
        let lib = unsafe { Arc::new(Library::new(&tramp_path).unwrap()) };
        let dispatch_ptr: usize = unsafe {
            let sym: Symbol<TrampolineDispatch> = lib.get(b"trampoline_dispatch").unwrap();
            *sym as *const () as usize
        };

        for func_def in tramp_def.functions {
            let params: Vec<ValType> = func_def.param_types.iter().map(|t| parse_valtype(t)).collect();
            let results = vec![ValType::I32]; // Assuming standard i32 return.

            let func_type = FuncType::new(&state_lock.engine, params, results);
            let func_name_cloned = func_def.name.clone();

            let log_name = func_def.name.clone();

            linker.func_new(&func_def.module, &func_def.name, func_type,
                move |mut caller, args: &[Val], rets: &mut [Val]| -> anyhow::Result<()> {
                    // ---> START HOST BOUNDARY SPAN
                    let start_host = std::time::Instant::now();

                    let mem = caller.get_export("memory").unwrap().into_memory().unwrap();
                    let mem_base = mem.data_mut(&mut caller).as_mut_ptr();
                    let mem_len = mem.data(&caller).len();

                    let flat_args: Vec<u64> = args.iter().map(|v| match v {
                        Val::I32(i) => *i as u64, Val::I64(i) => *i as u64, _ => 0,
                    }).collect();

                    let mut ret_val: u64 = 0;

                    // Call into the SPECIFIC trampoline for this function.
                    unsafe {
                        let dispatch_fn: TrampolineDispatch = std::mem::transmute(dispatch_ptr);

                        let success = dispatch_fn(
                            func_name_cloned.as_ptr(), func_name_cloned.len(),
                            mem_base, mem_len,
                            flat_args.as_ptr(), flat_args.len(),
                            &mut ret_val
                        );

                        rets[0] = Val::I32(if success { ret_val as i32 } else { 0 });
                    }

                    // ---> END HOST BOUNDARY SPAN
                    println!("*** Span: HostFunc_{} | DurationUs: {}", log_name, start_host.elapsed().as_micros());
                    Ok(())
                }
            ).unwrap();
        }

        loaded_libs.push(lib);
    }

    state_lock.loaded_libs = loaded_libs;
    state_lock.wasm_module = Some(module);
    state_lock.linker = Some(linker);

    Json(GenericResponse { status: "success".into(), message: "Functions registered and loaded successfully.".into() })
}


async fn run_handler(
    State(state): State<Arc<Mutex<ProxyState>>>,
    body: String,
) -> Json<GenericResponse> {
    // ---> START TOTAL REQUEST SPAN
    let start_total = std::time::Instant::now();

    let state_lock = state.lock().unwrap();

    if state_lock.wasm_module.is_none() {
        return Json(GenericResponse { status: "error".into(), message: "No function initialized.".into() });
    }

    let engine = &state_lock.engine;
    let module = state_lock.wasm_module.as_ref().unwrap();
    let linker = state_lock.linker.as_ref().unwrap();

    // ---> START INSTANTIATION SPAN
    let start_init = std::time::Instant::now();
    // Setup fresh WASI context and Store for this execution.
    let wasi = WasiCtxBuilder::new().inherit_stdout().inherit_stderr().build_p1();
    let mut store = Store::new(engine, ProxyModuleState { wasi });

    // Instantiate the module.
    let instance = match linker.instantiate(&mut store, module) {
        Ok(i) => i,
        Err(e) => return Json(GenericResponse { status: "error".into(), message: e.to_string() }),
    };
    // ---> END INSTANTIATION SPAN
    println!("*** Span: WasmInit | DurationUs: {}", start_init.elapsed().as_micros());

    // Extract necessary guest functions (assuming 'alloc' is exported by the guest module).
    let alloc = instance.get_typed_func::<u32, u32>(&mut store, "alloc").unwrap();
    let run_proxy = instance.get_typed_func::<(u32, u32), u64>(&mut store, "run_proxy").unwrap();
    let memory = instance.get_memory(&mut store, "memory").unwrap();

    // Allocate memory in Wasm guest for the JSON input.
    let input_len = body.len() as u32;
    let input_ptr = alloc.call(&mut store, input_len).unwrap();

    // Write JSON body into Wasm memory.
    memory.write(&mut store, input_ptr as usize, body.as_bytes()).unwrap();

    // ---> START WASM EXECUTION SPAN
    let start_exec = std::time::Instant::now();
    // Execute the function.
    let packed_ret = match run_proxy.call(&mut store, (input_ptr, input_len)) {
        Ok(res) => res,
        Err(e) => return Json(GenericResponse { status: "error".into(), message: e.to_string() }),
    };
    // ---> END WASM EXECUTION SPAN
    println!("*** Span: WasmExec | DurationUs: {}", start_exec.elapsed().as_micros());

    // Decode the returned packed u64 (out_ptr << 32 | out_len).
    let out_ptr = (packed_ret >> 32) as usize;
    let out_len = (packed_ret & 0xFFFFFFFF) as usize;

    if out_len == 0 {
        return Json(GenericResponse { status: "error".into(), message: "Guest returned 0 bytes.".into() });
    }

    // Read output JSON string back from Wasm memory.
    let mem_slice = memory.data(&store);
    let out_bytes = &mem_slice[out_ptr..(out_ptr + out_len)];

    let result_str = std::str::from_utf8(out_bytes).unwrap_or("Invalid UTF-8");
    // ---> END TOTAL REQUEST SPAN
    println!("*** Span: TotalRun | DurationUs: {}", start_total.elapsed().as_micros());

    Json(GenericResponse { status: "success".into(), message: result_str.to_string() })
}


#[tokio::main(flavor = "current_thread")]
async fn main() {
    let engine = Engine::default();

    let state = Arc::new(Mutex::new(ProxyState {
        engine,
        loaded_libs: Vec::new(),
        wasm_module: None,
        linker: None,
    }));

    let app = Router::new()
        .route("/init", post(init_handler))
        .route("/run", post(run_handler))
        .with_state(state);

    println!("Mosaic proxy listening on 0.0.0.0:8080");
    axum::Server::bind(&"0.0.0.0:8080".parse().unwrap())
        .serve(app.into_make_service())
        .await
        .unwrap();
}
