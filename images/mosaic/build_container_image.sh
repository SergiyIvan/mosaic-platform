#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

TIME_SPANS_PATH_SUFFIX=""
if [ -n "$TIME_SPANS" ]; then
    TIME_SPANS_PATH_SUFFIX="-time-spans"
fi

PROXY_DIR="$DIR/../../proxies/mosaic-proxy$TIME_SPANS_PATH_SUFFIX"
DISK="$DIR/disk"

echo "Building mosaic-proxy..."
(cd "$PROXY_DIR" && cargo build --release)

# Prepare directory used to setup the filesystem.
rm -rf $DISK &> /dev/null
mkdir -p $DISK

# Copy proxy.
cp "$PROXY_DIR/target/release/mosaic-proxy" "$DISK/"

# Build docker.
docker build --tag=mosaic-proxy $DIR

# Remove directory used to create the image.
rm -r $DISK
