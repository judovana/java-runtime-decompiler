#!/bin/bash
# Locate JDK from $PATH
path=`which java`
# go through symlinks until we are at /usr/lib/jvm/...
while [[ $path != /usr/lib/jvm/* ]]
do
path=`readlink $path`
done
# go three directories up to the JDK root folder.
for i in {1..3}
do
path="$(dirname "$path")"
done
export JAVA_HOME=$path
# launch application
java -cp $JAVA_HOME/lib/tools.jar:$HOME/.m2/repository/com/fifesoft/rsyntaxtextarea/2.6.1/rsyntaxtextarea-2.6.1.jar:$HOME/.m2/repository/java/java-runtime-decompiler/1.0.0-SNAPSHOT/java-runtime-decompiler-1.0.0-SNAPSHOT.jar com.redhat.thermostat.vm.decompiler.data.Main
