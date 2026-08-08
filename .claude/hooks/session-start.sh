#!/bin/bash
# Installs the JDKs this build needs in Claude Code on the web containers.
#
# The plugin is compiled with a Java 8 toolchain, while the test task runs on Java 17 so that
# TestKit can start Gradle 9.x daemons (which require Java 17+) as well as Gradle 7.6 ones.
# Web containers ship only a newer JDK, so without this the build cannot resolve its toolchains.
set -euo pipefail

# Local machines are expected to have their own JDKs already.
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

JDK8_HOME=/usr/lib/jvm/java-8-openjdk-amd64
JDK17_HOME=/usr/lib/jvm/java-17-openjdk-amd64

SUDO=''
if [ "$(id -u)" -ne 0 ]; then
  SUDO='sudo'
fi

missing=()
[ -x "$JDK8_HOME/bin/javac" ] || missing+=(openjdk-8-jdk-headless)
[ -x "$JDK17_HOME/bin/javac" ] || missing+=(openjdk-17-jdk-headless)

if [ ${#missing[@]} -gt 0 ]; then
  echo "Installing: ${missing[*]}"
  export DEBIAN_FRONTEND=noninteractive
  # Third-party PPAs are blocked by the network policy; the Ubuntu archive itself is reachable,
  # so ignore index failures rather than failing the whole session.
  $SUDO apt-get update -qq >/dev/null 2>&1 || true
  $SUDO apt-get install -y -qq --no-install-recommends "${missing[@]}" >/dev/null
else
  echo "JDK 8 and JDK 17 already present"
fi

# Gradle discovers both JDKs by scanning /usr/lib/jvm, so the toolchains resolve on their own.
# JAVA_HOME only pins the daemon JVM: Gradle 7.6 (this project's wrapper) officially supports
# Java 8-19, and containers default to a newer JDK than that.
if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  echo "export JAVA_HOME=$JDK17_HOME" >> "$CLAUDE_ENV_FILE"
fi

"$JDK8_HOME/bin/java" -version 2>&1 | grep -v '^Picked up' | head -1
"$JDK17_HOME/bin/java" -version 2>&1 | grep -v '^Picked up' | head -1
