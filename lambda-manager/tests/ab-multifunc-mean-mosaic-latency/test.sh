#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR/../benchmarks.sh"
source "$DIR/../shared.sh"


function run_latency_benchmark {
    export RESULTS_DIR="$DIR/ab-results"
    rm -r $RESULTS_DIR
    mkdir -p $RESULTS_DIR

    export CONCURRENCY=1
    export WORKLOAD=400
    export WARMUP=100

    for bench in "${MO_BENCHMARKS[@]}"; do
        register $bench
        benchmark $bench
    done

    unset CONCURRENCY
    unset WORKLOAD
    unset WARMUP

    unset RESULTS_DIR
}


function run {
    export FUNCTION_MEMORY=2048

    start_lambda_manager "$DIR/config.json" "$DIR/variables.json"
    sleep 5

    run_latency_benchmark

    stop_lambda_manager
    unset FUNCTION_MEMORY
}

run
