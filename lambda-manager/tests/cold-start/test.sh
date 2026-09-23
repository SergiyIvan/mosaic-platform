#!/bin/bash

# Exit on error
set -e

# Configuration
ITERATIONS=10
OUTPUT_FILE="cold_start_results.csv"
LAMBDA_LOG="lambda.log"
DATA_ADDRESS="http://172.18.0.1:8000"
PROXY_PORT=8080

# Benchmark details for "compression"
BENCH_NAME="compression"
BENCH_UNDERSCORE="compression"

# 1. Mosaic configuration strings
MOSAIC_WASM_URL="$DATA_ADDRESS/apps/wasm/${BENCH_UNDERSCORE}.wasm"
MOSAIC_INIT_BODY='[
  {"url": "'$DATA_ADDRESS'/apps/trampoline/native/libhttp_trampoline.so", "functions": [{"module": "env", "name": "host_download", "param_types": ["i32", "i32", "i32", "i32"]}]},
  {"url": "'$DATA_ADDRESS'/apps/trampoline/native/libcompression_trampoline.so", "functions": [{"module": "env", "name": "host_compress", "param_types": ["i32", "i32", "i32", "i32"]}]}
]'

# 2. Uber / CPU-binding configuration strings
UBER_SO_URL="$DATA_ADDRESS/apps/native/native/lib${BENCH_UNDERSCORE}.so"

# 3. XaaS Repository Paths (assumed from repository structures provided)
# Dynamically locate repository root
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)/../"
# Adjust this path if your benchmark workspace directory differs
BENCH_REL_PATH="core-modules/native/$BENCH_NAME"

# Prepare output CSV file
echo "iteration,mode,latency_ms" > "$OUTPUT_FILE"
rm -f "$LAMBDA_LOG"

# Helper function to poll port availability cleanly
wait_for_port() {
    local port=$1
    while ! nc -z localhost "$port" >/dev/null 2>&1; do
        sleep 0.005
    done
}

echo "========================================="
echo " Starting Cold Start Latency Experiment "
echo " Benchmark: $BENCH_NAME | Iterations: $ITERATIONS"
echo "========================================="

for ((i=1; i<=ITERATIONS; i++)); do
    echo -e "\n--- Iteration $i of $ITERATIONS ---"

    # ==========================================
    # MOSAIC COLD START
    # ==========================================
    echo "Measuring Mosaic cold start..."

    # Track complete timeline from Docker initialization to API initialization completion
    start_time=$(date +%s%3N)

    # Launch generic proxy
    docker run --rm --name="mosaic-cold-start-proxy" \
        -p "$PROXY_PORT":8080 \
        -d mosaic-proxy:latest &>> $LAMBDA_LOG

    # Wait for web framework to yield readiness
    wait_for_port "$PROXY_PORT"

    # Trigger deployment/registration sequence via direct HTTP call
    curl -s -X POST "http://localhost:$PROXY_PORT/init?wasm_url=$(echo "$MOSAIC_WASM_URL" | jq -sRr @uri)" \
         -H "Content-Type: application/json" \
         --data "$MOSAIC_INIT_BODY"

    end_time=$(date +%s%3N)
    mosaic_ms=$((end_time - start_time))

    # Cleanup immediately
    docker kill "mosaic-cold-start-proxy" > /dev/null 2>&1 || true
    echo "Mosaic: ${mosaic_ms}ms"
    echo "$i,Mosaic,$mosaic_ms" >> "$OUTPUT_FILE"


    # ==========================================
    # UBER / CPU-BINDING
    # ==========================================
    echo "Measuring Uber / CPU-binding cold start..."

    start_time=$(date +%s%3N)

    # Launch native proxy
    docker run --rm --name="native-cold-start-proxy" \
        -p "$PROXY_PORT":8080 \
        -d native-proxy:latest &>> $LAMBDA_LOG

    wait_for_port "$PROXY_PORT"

    # Trigger registration with compilation flavor binary url query string
    curl -s -X POST "http://localhost:$PROXY_PORT/init?url=$(echo "$UBER_SO_URL" | jq -sRr @uri)"

    end_time=$(date +%s%3N)
    uber_ms=$((end_time - start_time))

    docker kill "native-cold-start-proxy" > /dev/null 2>&1 || true
    echo "Uber/CPU-binding: ${uber_ms}ms"
    echo "$i,Uber,$uber_ms" >> "$OUTPUT_FILE"


    # ==========================================
    # XaaS COLD START (Compilation)
    # ==========================================
    echo "Measuring XaaS (Native Source Build) cold start..."

    # Step 3a: Explicit Clean (Excluded from measured time bounds)
    docker run --rm \
        -u "$(id -u):$(id -g)" \
        -e EXECUTION_ENVIRONMENT="local" \
        -v "$REPO_ROOT:/workspace" \
        -w "/workspace/$BENCH_REL_PATH" \
        mosaic-builder \
        cargo clean > /dev/null 2>&1
    rm -rf "$REPO_ROOT/$BENCH_REL_PATH/target" "$REPO_ROOT/$BENCH_REL_PATH/Cargo.lock"

    # Step 3b: Measure Clean Build Latency
    start_time=$(date +%s%3N)

    docker run --rm \
        -u "$(id -u):$(id -g)" \
        -e EXECUTION_ENVIRONMENT="local" \
        -v "$REPO_ROOT:/workspace" \
        -w "/workspace/$BENCH_REL_PATH" \
        mosaic-builder \
        cargo build-native > /dev/null 2>&1

    end_time=$(date +%s%3N)
    xaas_ms=$((end_time - start_time))

    echo "XaaS: ${xaas_ms}ms"
    echo "$i,XaaS,$xaas_ms" >> "$OUTPUT_FILE"

done

echo -e "\n========================================="
echo "Experiment Complete! Data saved to: $OUTPUT_FILE"
echo "========================================="
