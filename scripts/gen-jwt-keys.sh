#!/usr/bin/env bash
# =========================================================================
# Sinh RSA keypair 4096-bit cho JWT RS256.
# Dev: ghi vào backend/certs/*.pem (đã .gitignore).
# Test: ghi vào backend/congvan-app/src/test/resources/certs/test/*.pem
#       và commit vào repo (chỉ dùng cho Testcontainers).
# Prod: KHÔNG sinh ở đây — dùng K8s secret hoặc Vault.
# =========================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="${1:-dev}"

case "${MODE}" in
    dev)
        OUT_DIR="${ROOT_DIR}/backend/certs"
        ;;
    test)
        OUT_DIR="${ROOT_DIR}/backend/congvan-app/src/test/resources/certs/test"
        ;;
    *)
        echo "Usage: $0 [dev|test]" >&2
        exit 1
        ;;
esac

mkdir -p "${OUT_DIR}"
PRIV="${OUT_DIR}/jwt-private.pem"
PUB="${OUT_DIR}/jwt-public.pem"

if [[ -s "${PRIV}" && -s "${PUB}" && "${FORCE:-0}" != "1" ]]; then
    echo "→ Keypair đã tồn tại ở ${OUT_DIR}. Dùng FORCE=1 để ghi đè."
    exit 0
fi

echo "→ Sinh RSA 4096 keypair..."
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:4096 -out "${PRIV}" 2>/dev/null
openssl rsa -in "${PRIV}" -pubout -out "${PUB}" 2>/dev/null
chmod 600 "${PRIV}"
chmod 644 "${PUB}"

echo "✅ Private: ${PRIV}"
echo "✅ Public : ${PUB}"

if [[ "${MODE}" == "dev" ]]; then
    echo ""
    echo "ℹ️  Thêm vào .env:"
    echo "  JWT_PRIVATE_KEY_PATH=file:${PRIV}"
    echo "  JWT_PUBLIC_KEY_PATH=file:${PUB}"
fi
