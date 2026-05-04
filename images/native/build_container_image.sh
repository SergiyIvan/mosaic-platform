#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

PROXY_DIR="$DIR/../../proxies/native-proxy"
DISK="$DIR/disk"

echo "Building native-proxy..."
(cd "$PROXY_DIR" && cargo build --release)

# Prepare directory used to setup the filesystem.
rm -rf $DISK &> /dev/null
mkdir -p $DISK

# Copy proxy.
cp "$PROXY_DIR/target/release/native-proxy" "$DISK/"

# Build docker.
docker build --tag=native-proxy $DIR

# Remove directory used to create the image.
rm -r $DISK
