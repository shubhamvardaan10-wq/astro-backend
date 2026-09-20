#!/bin/bash
set -e

CERT_DIR="/etc/nginx/certs"
mkdir -p "$CERT_DIR"

if [ ! -f "$CERT_DIR/privkey.pem" ] || [ ! -f "$CERT_DIR/fullchain.pem" ]; then
    echo "Generating self-signed SSL certificate for HTTPS ingress..."
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout "$CERT_DIR/privkey.pem" \
        -out "$CERT_DIR/fullchain.pem" \
        -subj "/C=IN/ST=Delhi/L=NewDelhi/O=AstroBackend/CN=localhost" \
        -addext "subjectAltName=DNS:localhost,DNS:*.localhost,IP:127.0.0.1" >/dev/null 2>&1
    echo "Self-signed certificate generated in $CERT_DIR"
fi

exec nginx -g "daemon off;"
