#!/bin/bash

## resolve folder of this script, following all symlinks:
## http://stackoverflow.com/questions/59895/can-a-bash-script-tell-what-directory-its-stored-in
SCRIPT_SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SCRIPT_SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  SCRIPT_DIR="$( cd -P "$( dirname "$SCRIPT_SOURCE" )" && pwd )"
  SCRIPT_SOURCE="$(readlink "$SCRIPT_SOURCE")"
  # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
  [[ $SCRIPT_SOURCE != /* ]] && SCRIPT_SOURCE="$SCRIPT_DIR/$SCRIPT_SOURCE"
done
readonly PORTABLE_JRD_HOME="$( cd -P "$( dirname "$SCRIPT_SOURCE" )" && pwd )"

# Locate JDK from $PATH
javac_home=$(command -v javac)
javac_home=$(readlink -f "$javac_home")
javac_home="$(dirname "$(dirname "$javac_home")")"

PURPOSE=DEVELOPMENT
MVN_SOURCE="$HOME/.m2/repository"

function findLib(){
  if [ "x$PURPOSE" = "xDEVELOPMENT" ] ; then
    BASE="$MVN_SOURCE"
    GROUP="$1"
    FILENAME="$2"
  else
    BASE="$PORTABLE_JRD_HOME/libs/deps"
    GROUP=""
    FILENAME="$2"
  fi
  find "$BASE/$GROUP"  | grep "$FILENAME$"   | sort -V | tail -n 1
}

TOOLS="$javac_home"/lib/tools.jar

readonly RSYNTAXTEXTAREA=$(findLib "com/fifesoft/rsyntaxtextarea" "rsyntaxtextarea-.*\.jar" )
readonly GSON=$(findLib "com/google/code/gson/gson" "gson-.*\.jar")
readonly BYTEMAN=$(findLib "org/jboss/byteman/byteman-install" "byteman-install-.*\.jar")
readonly JUST_BUILD_JRD=`find "$PORTABLE_JRD_HOME"/runtime-decompiler/target/runtime-decompiler-*-SNAPSHOT.jar 2> /dev/null`
if [ -f "$JUST_BUILD_JRD" ] ; then
  readonly JRD="$JUST_BUILD_JRD"
else
  readonly JRD=$(findLib "java-runtime-decompiler/runtime-decompiler" "runtime-decompiler-.*\.jar")
fi

if [ "x$THE_TERRIBLE_INTERNAL_JRD" == "xtrue" ] ; then
  return 0
fi

readonly PROPERTY="-Djrd.location=$PORTABLE_JRD_HOME"

# launch application
"$javac_home"/bin/java "$PROPERTY" -cp "$TOOLS":\
"$JRD":"$RSYNTAXTEXTAREA":"$GSON":"$BYTEMAN" \
 org.jrd.backend.data.Main "$@"
