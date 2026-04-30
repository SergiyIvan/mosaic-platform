#!/bin/bash

if [[ -z "${ARGO_HOME}" ]]; then
	echo "ARGO_HOME is not defined. Exiting..."
	exit 1
fi

if [[ -z "${JAVA_HOME}" ]]; then
	echo "JAVA_HOME is not defined. Exiting..."
	exit 1
fi

# Note: this file may have variables that need to be adapted to your local environment.
export MANAGER_HOME=$ARGO_HOME/lambda-manager
export CODEBASE_HOME=$MANAGER_HOME/codebase
