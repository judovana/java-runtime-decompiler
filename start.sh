#!/bin/bash
# Locate JDK from $PATH
javac_home=$(command -v javac)
javac_home=$(readlink -f "$javac_home")
javac_home="$(dirname "$(dirname "$javac_home")")"

TOOLS="$javac_home"/lib/tools.jar
RSYNTAXTEXTAREA=$(find "$HOME"/.m2/repository/com/fifesoft/rsyntaxtextarea/*/rsyntaxtextarea-*.jar -printf %p:)
GSON=$(find "$HOME"/.m2/repository/com/google/code/gson/gson/*/gson-*.jar -printf %p:)
BYTEMAN=$(find "$HOME"/.m2/repository/org/jboss/byteman/byteman-install/*/byteman-install-*.jar -printf %p:)
JRD=$(find "$HOME"/.m2/repository/java-runtime-decompiler/runtime-decompiler/*/runtime-decompiler-*.jar -printf %p:)
# launch application
"$javac_home"/bin/java -cp "$TOOLS":\
"$RSYNTAXTEXTAREA":\
"$GSON":\
"$BYTEMAN":\
"$JRD"\
 org.jrd.backend.data.Main