#!/bin/bash

if [[ -z "${JAVA_HOME}" ]]; then
    echo "JAVA_HOME is not defined. Exiting..."
    exit 1
fi

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

# Note: this file may have variables that need to be adapted to your local environment.
export MANAGER_HOME="$DIR/../.."
export CODEBASE_HOME="$MANAGER_HOME/codebase"
