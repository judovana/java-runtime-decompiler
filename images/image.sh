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
readonly THIS_SCRIPT_DIR="$( cd -P "$( dirname "$SCRIPT_SOURCE" )" && pwd )"
readonly SCRIPT_DIR=`readlink -f "$THIS_SCRIPT_DIR/../"`

THE_TERRIBLE_INTERNAL_JRD=true
. $SCRIPT_DIR/start.sh


set -ex
set -o pipefail

NAME=`basename $JRD | sed "s;.jar;;"`
VERSION=$1

TARGET_DIR="$SCRIPT_DIR/images/target"
IMAGE_DIR="$TARGET_DIR/image"
LIB_DIR="$IMAGE_DIR/libs"
DEPS_DIR="$LIB_DIR/deps"
DECOMPS="$LIB_DIR/decompilers"
CONFIG="$IMAGE_DIR/config"

rm -rvf "$TARGET_DIR"
mkdir "$TARGET_DIR"
mkdir "$IMAGE_DIR"
mkdir "$LIB_DIR"
mkdir "$DEPS_DIR"
mkdir "$DECOMPS"
mkdir "$CONFIG"

cp "$RSYNTAXTEXTAREA" "$DEPS_DIR"
cp "$GSON" "$DEPS_DIR"
cp "$BYTEMAN" "$DEPS_DIR"
cp "$JRD" "$DEPS_DIR"
cp "$SCRIPT_DIR/decompiler_agent/target/decompiler-agent-$VERSION.jar" "$LIB_DIR"

# inject different macro into default decompiler wrappers
function modifyWrappers() {
  name=$(echo $1 | sed "s#\b.#\u\0#g") # make first letter capital for the .json filename

  mkdir -p temp/plugins
  unzip -p "$DEPS_DIR/$NAME.jar" "plugins/${name}DecompilerWrapper.json" > "temp/plugins/${name}DecompilerWrapper.json"

  sed -i "s#\${HOME}#\${JRD}#g" "temp/plugins/${name}DecompilerWrapper.json"
  sed -i "s#/\.m2\(/.\+\)/#/libs/decompilers/${1}/#g" "temp/plugins/${name}DecompilerWrapper.json"

  sed -i "s#\${XDG_CONFIG_HOME}#\${JRD}/config#" "temp/plugins/${name}DecompilerWrapper.json"

  jar -uf "$DEPS_DIR/$NAME.jar" -C temp "plugins/${name}DecompilerWrapper".json

  rm -rf temp
}

# if PLUGINS=TRUE && mvn install -PdownloadPlugins was run, and you really wont them to include plugins in images
if [ "x$PLUGINS" == "xTRUE" ] ; then
  for dec in procyon fernflower ; do
    mkdir "$DECOMPS/$dec"
    jars=`find $MVN_SOURCE | grep -e $dec | grep \.jar$`
    for jar in $jars ; do
      cp "$jar" "$DECOMPS/$dec"
    done
    modifyWrappers $dec
    rmdir "$DECOMPS/$dec" 2>/dev/null || true
  done
  rmdir "$DECOMPS" 2>/dev/null || true
  SUFFIX="-with-decompilers"
fi

for extension in sh bat ; do
  echo "creating $IMAGE_DIR/start.$extension"
  cat $SCRIPT_DIR/start.$extension | sed "s/PURPOSE=DEVELOPMENT/PURPOSE=PORTABLE/" > $IMAGE_DIR/start.${extension}
  chmod 755 "$IMAGE_DIR/start.$extension"
done

pushd $TARGET_DIR
cp -r $IMAGE_DIR $NAME$SUFFIX
tar -cJf  $TARGET_DIR/$NAME$SUFFIX.tar.xz $NAME$SUFFIX
if which zip  ; then
  zip  $TARGET_DIR/$NAME$SUFFIX.zip `find $NAME$SUFFIX`
fi
popd
