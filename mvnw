#!/bin/sh
# Maven wrapper script
set -e

MAVEN_WRAPPER_JAR=".mvn/wrapper/maven-wrapper.jar"
MAVEN_OPTS="${MAVEN_OPTS:-}"

# Download wrapper jar if missing
if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
    echo "Downloading Maven wrapper..."
    mkdir -p .mvn/wrapper
    curl -o "$MAVEN_WRAPPER_JAR" \
        "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar" \
        -f -L --silent
fi

exec java $MAVEN_OPTS -jar "$MAVEN_WRAPPER_JAR" "$@"
