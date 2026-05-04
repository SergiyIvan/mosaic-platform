#!/bin/bash

# This is a smoke-test for Lambda Manager and Mosaic/native runtimes.
# It uploads functions with different runtimes and performs a single invocation to every registered function.
# The functions in this script are the typical functions from the benchmark suite we use for evaluation.
# NOTE: this script requires the "web" and "upload" containers to be started (see mosaic-system/data/start-webserver.sh).

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR/../benchmarks.sh"
source "$DIR/../shared.sh"


function run_na_benchmarks {
    echo "=== Running Native Benchmarks ==="
    for bench in "${NA_BENCHMARKS[@]}"; do
        register $bench
        request $bench
    done
}

function run_mo_benchmarks {
    echo "=== Running Mosaic Benchmarks ==="
    for bench in "${MO_BENCHMARKS[@]}"; do
        register $bench
        request $bench
    done
}


function run {
    export FUNCTION_MEMORY=2048

    start_lambda_manager "$DIR/config.json" "$DIR/variables.json"
    sleep 5

    run_na_benchmarks
    run_mo_benchmarks

    stop_lambda_manager
    unset FUNCTION_MEMORY
}

run
