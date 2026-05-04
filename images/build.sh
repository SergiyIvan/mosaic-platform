#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"


function build_container_image {
    IMAGE=$1
    cd "$DIR"/"$IMAGE"
    bash build_container_image.sh
    cd "$DIR"
}

read -p "Mosaic container (y or Y, everything else as no)? " -n 1 -r
echo    # move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
then
    build_container_image mosaic
fi

read -p "Native container (y or Y, everything else as no)? " -n 1 -r
echo    # move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
then
    build_container_image native
fi
