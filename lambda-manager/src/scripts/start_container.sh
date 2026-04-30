#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

source "$DIR"/environment.sh

LAMBDA_PORT=$1
if [ -z "$LAMBDA_PORT" ]; then
  echo "Lambda port is not present."
  exit 1
fi

LAMBDA_NAME=$2
if [ -z "$LAMBDA_NAME" ]; then
  echo "Lambda name is not present."
  exit 1
fi

CONTAINER_IMAGE=$3
if [ -z "$CONTAINER_IMAGE" ]; then
  echo "Container image is not present."
  exit 1
fi

CONTAINER_SIZE_OPTIONS=

# Only limit memory and CPU quota for Mosaic and Native proxy instances.
if [[ $CONTAINER_IMAGE == mosaic-proxy* || $CONTAINER_IMAGE == native-proxy* ]]; then
  LAMBDA_MEMORY=$4
  if [ -z "$LAMBDA_MEMORY" ]; then
    echo "Lambda memory is not present."
    exit 1
  fi

  LAMBDA_CPU_QUOTA=$5
  if [ -z "$LAMBDA_CPU_QUOTA" ]; then
    echo "Lambda CPU quota is not present."
    exit 1
  fi

  TAGS=( "${@:6}" )

  # The default value. Source: https://docs.docker.com/config/containers/resource_constraints/#configure-the-default-cfs-scheduler
  CGROUPS_CPU_PERIOD="100000"
  CONTAINER_SIZE_OPTIONS="--memory=$LAMBDA_MEMORY"
  # Limiting CPU to 1 core instead of using $LAMBDA_CPU_QUOTA - overprovisioning but relying on the OS scheduler.
  CONTAINER_SIZE_OPTIONS="$CONTAINER_SIZE_OPTIONS --cpu-period=$CGROUPS_CPU_PERIOD"
  CONTAINER_SIZE_OPTIONS="$CONTAINER_SIZE_OPTIONS --cpu-quota=$CGROUPS_CPU_PERIOD"
else
  TAGS=( "${@:4}" )
fi

# To set up such tags as lambda_port, lambda_timestamp, and LD_LIBRARY_PATH.
TAGS=( "${TAGS[@]/#/'-e '}" )

# The default value for our proxies.
PROXY_PORT="8080"

LAMBDA_HOME="$CODEBASE_HOME"/"$LAMBDA_NAME"
mkdir "$LAMBDA_HOME" &> /dev/null

cd "$LAMBDA_HOME"

docker run --rm --name="$LAMBDA_NAME" \
  ${TAGS[@]} \
  -p "$LAMBDA_PORT":"$PROXY_PORT" \
  $CONTAINER_SIZE_OPTIONS \
  "$CONTAINER_IMAGE" &

# Writes PID of the init process.
# docker inspect --format '{{ .State.Pid }}' "$LAMBDA_NAME" > "$LAMBDA_HOME"/lambda.pid

wait
