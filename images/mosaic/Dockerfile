FROM debian:bookworm-slim

RUN apt update && \
    apt install -y wget && \
    rm -rf /var/lib/apt/lists/*

ADD disk/mosaic-proxy /usr/local/bin/mosaic-proxy

# Make sure the entrypoint is executable.
RUN chmod +x /usr/local/bin/mosaic-proxy

ENTRYPOINT /usr/local/bin/mosaic-proxy
