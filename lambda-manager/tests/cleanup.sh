#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

# This script cleans up lambdas in case Lambda Manager terminated abruptly.

docker ps --filter name=lambda_* --filter status=running -aq | xargs docker stop
docker ps --filter name=lambda_* -aq | xargs docker rm
