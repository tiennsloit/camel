#!/usr/bin/env bash
set -euo pipefail

# Copy the built io-component-spi JAR into plugin/libs folders for local dev.
# Usage: ./scripts/sync-spi.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SPI_JAR=$(ls -1 "${ROOT_DIR}/io-component-spi/target"/io-component-spi-*.jar 2>/dev/null | tail -n 1 || true)

if [[ -z "${SPI_JAR}" ]]; then
  echo "SPI jar not found. Build it first: mvn -pl io-component-spi clean package"
  exit 1
fi

echo "Using SPI jar: ${SPI_JAR}"

mkdir -p "${ROOT_DIR}/io-component-onedrive/libs" \
         "${ROOT_DIR}/io-component-gdrive/libs" \
         "${ROOT_DIR}/local-plugins"

cp -p "${SPI_JAR}" "${ROOT_DIR}/io-component-onedrive/libs/"
cp -p "${SPI_JAR}" "${ROOT_DIR}/io-component-gdrive/libs/"
cp -p "${SPI_JAR}" "${ROOT_DIR}/local-plugins/"

echo "Copied SPI jar to:"
echo "  - io-component-onedrive/libs/"
echo "  - io-component-gdrive/libs/"
echo "  - local-plugins/"

install_spi() {
  local module_dir="$1"
  local jar_path="$2"
  local version
  version=$(basename "${jar_path}" | sed -E 's/io-component-spi-([0-9.]+)\.jar/\1/')
  echo "Installing SPI ${version} into ${module_dir} local repo"
  (cd "${module_dir}" && mvn install:install-file \
    -Dfile="${jar_path}" \
    -DgroupId=com.example.camel \
    -DartifactId=io-component-spi \
    -Dversion="${version}" \
    -Dpackaging=jar)
  echo "Building ${module_dir##*/} with -U"
  (cd "${module_dir}" && mvn clean package -U)
}

install_spi "${ROOT_DIR}/io-component-onedrive" "${SPI_JAR}"
install_spi "${ROOT_DIR}/io-component-gdrive" "${SPI_JAR}"
