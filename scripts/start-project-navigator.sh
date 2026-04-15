#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
JAVA_BIN="${JAVA_HOME:-}/bin/java"
JAR_CANDIDATES=(
  "${ROOT_DIR}/project-navigator-0.1.0-SNAPSHOT.jar"
  "${ROOT_DIR}/target/project-navigator-0.1.0-SNAPSHOT.jar"
)

if [[ -z "${JAVA_HOME:-}" ]]; then
  JAVA_BIN="java"
fi

JAR_PATH=""
for candidate in "${JAR_CANDIDATES[@]}"; do
  if [[ -f "${candidate}" ]]; then
    JAR_PATH="${candidate}"
    break
  fi
done

if [[ -z "${JAR_PATH}" ]]; then
  echo "Jar not found."
  echo "Expected one of:"
  printf '  - %s\n' "${JAR_CANDIDATES[@]}"
  echo "If you are in the repo, build it first with: mvn -DskipTests package"
  exit 1
fi

JAVA_VERSION_OUTPUT="$("${JAVA_BIN}" -version 2>&1 | head -n 1 || true)"
JAVA_MAJOR="$(echo "${JAVA_VERSION_OUTPUT}" | sed -E 's/.*version "([0-9]+).*/\1/')"

if [[ -z "${JAVA_MAJOR}" || "${JAVA_MAJOR}" -lt 17 ]]; then
  echo "Java 17+ is required."
  echo "Current runtime: ${JAVA_VERSION_OUTPUT:-unknown}"
  echo "Set JAVA_HOME to a Java 17+ installation, then run this script again."
  exit 1
fi

exec "${JAVA_BIN}" -jar "${JAR_PATH}"
