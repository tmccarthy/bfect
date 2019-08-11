#!/usr/bin/env bash

set -e

KEY=$1

[ -n "$KEY" ] || { echo "No key provided"; kill "$PPID"; exit 1; }

SOURCE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
DEST_DIR=/tmp/secrets
OPENSSL_COMMAND=aes-256-cbc

mkdir -p $DEST_DIR

for ENCRYPTED_FILE in "$SOURCE_DIR"/*.secret; do
  FILE_NAME=$(basename "$ENCRYPTED_FILE")
  DECRYPTED_NAME=${FILE_NAME%".secret"}
  openssl $OPENSSL_COMMAND -d -in "$ENCRYPTED_FILE" -k "$KEY" -out "$DEST_DIR/$DECRYPTED_NAME" -md md5
done
