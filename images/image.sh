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

if [ "x$VERIFY_CP" = "x" ] ; then
  VERIFY_CP=TRUE
fi

if [ "x$PLUGINS" = "x" ] ; then
  PLUGINS=TRUE
fi

if  [ "x$VERIFY_CP" = "xTRUE"  -a "x$PLUGINS" = "xTRUE" ] ;  then
  find "$HOME/.m2/repository" -type d | grep -e bitbucket/mstrobel/procyon -e org/jboss/windup/decompiler/fernflower -e  openjdk/asmtools -e benf/cfr
  if [ $? -eq 0 ] ; then
    echo "decompilers found in maven repo"
  else
    echo "no decompilers found! run 'mvn clean install -PdownloadPlugins' to obtain decompilers from maven repos or use VERIFY_CP/PLUGINS"
    exit 1
  fi
fi

FROM_CP=`mktemp`
if  [ "x$VERIFY_CP" = "xTRUE" ] ;  then
  a1=`mktemp`
  pushd ../runtime-decompiler
    mvn dependency:build-classpath  -PdownloadPlugins  -Dmdep.outputFile=$a1
  popd
  a2=`mktemp`
  pushd ../images
    mvn dependency:build-classpath  -PdownloadPlugins  -Dmdep.outputFile=$a2
  popd
  a3=`mktemp`
  pushd ../decompiler_agent
    mvn dependency:build-classpath  -PdownloadPlugins  -Dmdep.outputFile=$a3
  popd
  readonly CP_TO_VERIFY="`cat $a1`:`cat $a2`:`cat $a3`"
else
  readonly CP_TO_VERIFY=""
fi

function verifyOnCp() {
  local file="$1"
  echo "checking: $file against the classpath" 1>&2
  IFS_BACKUP="$IFS"
  IFS=":"
  for x in $CP_TO_VERIFY ;  do
    if [ `basename "$file"` == `basename "$x"` ] ; then
      echo "verified as $x" 1>&2
      echo -n ":$x" >> $FROM_CP
      IFS="$IFS_BACKUP"
      return 0
    fi
  done
  echo "not found on maven cp: $file ($CP_TO_VERIFY)" 1>&2
  echo "bad version? clean .m2?"  1>&2
  IFS="$IFS_BACKUP"
  exit 1
}

set -x
THE_TERRIBLE_INTERNAL_JRD=true
source  $SCRIPT_DIR/start.sh

set -ex
set -o pipefail

NAME=`basename $JRD | sed "s;.jar;;"`
VERSION=$1

TARGET_DIR="$SCRIPT_DIR/images/target"
IMAGE_DIR="$TARGET_DIR/image"
LIB_DIR="$IMAGE_DIR/libs"
DEPS_DIR="$LIB_DIR/deps"
DECOMPILERS="$LIB_DIR/decompilers"
CONFIG="$IMAGE_DIR/config"
AGENT_CONF="$CONFIG/conf"
PLUGINS_CONF="$CONFIG/plugins"

rm -rvf "$TARGET_DIR"
mkdir "$TARGET_DIR"
mkdir "$IMAGE_DIR"
mkdir "$LIB_DIR"
mkdir "$DEPS_DIR"
mkdir "$DECOMPILERS"
mkdir "$CONFIG"
mkdir "$AGENT_CONF"
mkdir "$PLUGINS_CONF"

cp "$RSYNTAXTEXTAREA" "$DEPS_DIR"
cp "$GSON" "$DEPS_DIR"
cp "$BYTEMAN" "$DEPS_DIR"
cp "$JRD" "$DEPS_DIR"
cp "$CPLC" "$DEPS_DIR"
cp "$SCRIPT_DIR/decompiler_agent/target/decompiler-agent-$VERSION.jar" "$LIB_DIR"
echo "{\"AGENT_PATH\":\"\${JRD}/libs/decompiler-agent-$VERSION.jar\"}" > "$AGENT_CONF/config.json"

# inject different macro into default decompiler wrappers
function modifyAndCopyWrappers() {
  local name=$(echo $1 | sed "s#\b.#\u\0#g") # make first letter capital for the .json filename

  mkdir -p temp/plugins
  unzip -p "$DEPS_DIR/$NAME.jar" "plugins/${name}DecompilerWrapper.json" > "temp/plugins/${name}DecompilerWrapper.json"
  unzip -p "$DEPS_DIR/$NAME.jar" "plugins/${name}DecompilerWrapper.java" > "temp/plugins/${name}DecompilerWrapper.java"

  sed -i "s#\${HOME}#\${JRD}#g" "temp/plugins/${name}DecompilerWrapper.json"
  sed -i "s#/\.m2\(/.\+\)/#/libs/decompilers/${1}/#g" "temp/plugins/${name}DecompilerWrapper.json"

  sed -i "s#\${XDG_CONFIG_HOME}#\${JRD}/config#" "temp/plugins/${name}DecompilerWrapper.json"

  jar -uf "$DEPS_DIR/$NAME.jar" -C temp "plugins/${name}DecompilerWrapper".json

  cp "temp/plugins/${name}DecompilerWrapper.json" "$PLUGINS_CONF"
  cp "temp/plugins/${name}DecompilerWrapper.java" "$PLUGINS_CONF"

  if  [ "x$VERIFY_CP" = "xTRUE" ] ;  then
    for jar in `cat  "temp/plugins/${name}DecompilerWrapper.json" | jq -r ".DependencyURL[]"` ; do
      verifyOnCp "$jar"
    done
  fi

  rm -rf temp
}

# if PLUGINS=TRUE && mvn install -PdownloadPlugins was run, and you really want them to include plugins in images
if [ "x$PLUGINS" == "xTRUE" ] ; then
  for dec in procyon fernflower jasm jcoder cfr; do
    mkdir "$DECOMPILERS/$dec"
    if [ "x$dec" == "xjasm" -o "x$dec" == "xjcoder" ] ; then
      lname=asmtools
    else
      lname=$dec
    fi
    # this is very naive, and may cause multiple versions in images
    # TODO, read dependencies from pom even with versions, and maybe check them against the jsons
    jars=`find $MVN_SOURCE | grep -e $lname | grep \.jar$`
    for jar in $jars ; do
      cp "$jar" "$DECOMPILERS/$dec"
    done
    modifyAndCopyWrappers $dec
    rmdir "$DECOMPILERS/$dec" 2>/dev/null || true
  done
  rmdir "$DECOMPILERS" 2>/dev/null || true
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

maven_cp=`mktemp`
echo              "$CP_TO_VERIFY" | sed "s/:/\\n/g" | sort | uniq | grep -v -e com/google/code/findbugs -e spotbugs -e guava -e apiguardian -e junit -e opentest4j > $maven_cp
used_cp=`mktemp`
cat "$FROM_CP" |sed "s/^://g" | sed "s/:/\\n/g" | sort | uniq > $used_cp
echo "There are following (with few filtered out) additional deps on mvn cp:"
diff $used_cp $maven_cp | grep -e  ">"  -e "<"  || echo "no fail now - but worthy to investigate"
