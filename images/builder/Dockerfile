FROM debian:bookworm-slim

# Install C/C++ toolchains and utilities.
RUN apt-get update && \
    apt-get install -y build-essential curl git sudo && \
    rm -rf /var/lib/apt/lists/*

# Install Rust toolchain.
ENV RUSTUP_HOME=/usr/local/rustup \
    CARGO_HOME=/usr/local/cargo \
    PATH=/usr/local/cargo/bin:$PATH

RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y --no-modify-path && \
    chmod -R a+w $RUSTUP_HOME $CARGO_HOME

# Install the Wasm WASI target for Mosaic guests.
RUN rustup target add wasm32-wasip1
