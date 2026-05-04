FROM debian:bookworm-slim

RUN apt update && \
    apt install -y wget && \
    rm -rf /var/lib/apt/lists/*

ADD disk/native-proxy /usr/local/bin/native-proxy

# Make sure the entrypoint is executable.
RUN chmod +x /usr/local/bin/native-proxy

ENTRYPOINT /usr/local/bin/native-proxy
