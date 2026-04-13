#!/bin/bash

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

function install_graalvm {
    wget https://download.oracle.com/graalvm/17/archive/graalvm-jdk-17.0.7_linux-x64_bin.tar.gz
    tar -vzxf graalvm-jdk-17.0.7_linux-x64_bin.tar.gz
    mv graalvm-jdk-17.0.7+8.1 $PLATFORM_HOME/resources
    rm graalvm-jdk-17.0.7_linux-x64_bin.tar.gz
}

export PLATFORM_HOME=$(DIR)
export JAVA_HOME=$PLATFORM_HOME/resources/graalvm-jdk-17.0.7+8.1

if [ ! -f $JAVA_HOME/bin/java ];
then
    echo "JVM not found. Installing..."
    install_graalvm
fi

if ! command -v docker &> /dev/null
then
    echo "WARNING: docker could not be found!"
    echo "Docker is needed to run the lambda manager."
fi

read -p "Build lambda manager? (y or Y, everything else as no)? " -n 1 -r
echo    # move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
then
    bash $PLATFORM_HOME/lambda-manager/build.sh
fi
