#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR/../benchmarks.sh"
source "$DIR/../shared.sh"


function run_latency_benchmark {
    export RESULTS_DIR="$DIR/ab-results"
    rm -rf $RESULTS_DIR
    mkdir -p $RESULTS_DIR

    if [ -z "$CONCURRENCY" ]; then
        export CONCURRENCY=1
    fi
    if [ -z "$WORKLOAD" ]; then
        export WORKLOAD=400
    fi
    if [ -z "$WARMUP" ]; then
        export WARMUP=100
    fi

    echo "=========================================="
    echo " RUNNING MOSAIC BENCHMARKS"
    echo "=========================================="
    for bench in "${MO_BENCHMARKS[@]}"; do
        register "$bench"
        benchmark "$bench"
        request "$bench"
        sleep 10
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
