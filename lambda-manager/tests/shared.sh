#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

# Load benchmarks configs.
source "$(dirname "${BASH_SOURCE[0]}")/benchmarks.sh"


GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

LAMBDA_MANAGER_HOST=localhost
LAMBDA_MANAGER_PORT=30008
LAMBDA_MANAGER_SOCKET_PORT=30009
LAMBDA_MANAGER_HOME="$(DIR)/.."

USER=user


function register {
    bench=$1

    if [ -z "$FUNCTION_MEMORY" ]; then
        FUNCTION_MEMORY=512
    fi

    runtime=
    if [[ $bench == "na_"* ]]; then
        runtime="native"
    elif [[ $bench == "mo_"* ]]; then
        runtime="mosaic"
    else
        echo -e "${RED}Cannot determine runtime of the benchmark: $bench${NC}"
        exit 1
    fi

    code_url=${BENCHMARK_CODE["$bench"]}
    metadata=${BENCHMARK_METADATA["$bench"]}

    echo "Registering $bench ($runtime)..."
    curl -s -X POST "$LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/upload_function?username=$USER&function_name=$bench&function_memory=$FUNCTION_MEMORY&function_code_url=$code_url&function_runtime=$runtime" \
         -H 'Content-Type: text/plain' \
         --data "$metadata"
}

function request {
    bench=$1

    echo -e "${GREEN}Invoking $bench...${NC}"
    curl -s -X POST $LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/$USER/$bench -H 'Content-Type: application/json' --data "${BENCHMARK_PAYLOADS["$bench"]}"
}

function benchmark {
    bench=$1

    payload=${BENCHMARK_PAYLOADS["$bench"]}

    if [ -z "$RESULTS_DIR" ]; then
        echo -e "${RED}Define RESULTS_DIR.${NC}"
    fi

    if [ -z "$CONCURRENCY" ]; then
        CONCURRENCY=1
    fi
    if [ -z "$WORKLOAD" ]; then
        WORKLOAD=500
    fi

    app_post=/tmp/app-post
    echo $payload > $app_post
    results_file=$RESULTS_DIR/"$USER-$bench.log"

    echo -e "${GREEN}Benchmarking $bench...${NC}"
    ab -p $app_post -T application/json -c $CONCURRENCY -n $WORKLOAD http://$LAMBDA_MANAGER_HOST:$LAMBDA_MANAGER_PORT/$USER/$bench &> $results_file

    res=$(cat $results_file | grep "Time per request" | grep "(mean)")
    echo -e "${GREEN}Mean latency for $bench:\n$res${NC}"

    rm $app_post
}

function wait_port {
    host=$1
    port=$2
    while ! nc -z $host $port; do sleep 0.01; done
}

function start_lambda_manager {
    config_path=$1
    variables_path=$2

    bash $LAMBDA_MANAGER_HOME/deploy.sh --config $config_path --variables $variables_path --http &

    wait_port $LAMBDA_MANAGER_HOST $LAMBDA_MANAGER_PORT
}

function stop_lambda_manager {
    kill $(lsof -i -P -n | grep LISTEN | grep $LAMBDA_MANAGER_PORT | awk '{print $2}')
}

function start_lambda_manager_socket {
    config_path=$1
    variables_path=$2

    bash $LAMBDA_MANAGER_HOME/deploy.sh --config $config_path --variables $variables_path --socket &

    wait_port $LAMBDA_MANAGER_HOST $LAMBDA_MANAGER_SOCKET_PORT
}

function stop_lambda_manager_socket {
    kill $(lsof -i -P -n | grep LISTEN | grep $LAMBDA_MANAGER_SOCKET_PORT | awk '{print $2}')
}
