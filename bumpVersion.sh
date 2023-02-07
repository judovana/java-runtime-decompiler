#!/bin/bash

## resolve folder of this script, following all symlinks,
## http://stackoverflow.com/questions/59895/can-a-bash-script-tell-what-directory-its-stored-in
SCRIPT_SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SCRIPT_SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  SCRIPT_DIR="$( cd -P "$( dirname "$SCRIPT_SOURCE" )" && pwd )"
  SCRIPT_SOURCE="$(readlink "$SCRIPT_SOURCE")"
  # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
  [[ $SCRIPT_SOURCE != /* ]] && SCRIPT_SOURCE="$SCRIPT_DIR/$SCRIPT_SOURCE"
done
readonly SCRIPT_DIR="$( cd -P "$( dirname "$SCRIPT_SOURCE" )" && pwd )"

set -e

current=`head ${SCRIPT_DIR}/pom.xml -n 20 | grep "<version>" | sed "s;./\?version.;;g" | sed "s/\s\+//g"`
echo    "current version: $current"
echo -n "type future one: "
read future
echo $current "->" $future
echo "ok? y/n"
read yn
if [ ! $yn == y ] ; then
  echo aborted
  exit 0
fi
poms=`find ${SCRIPT_DIR}| grep "/pom.xml"`
for pom in $poms ; do 
  echo " * $pom * "
  cat $pom | grep --color "<version>$current</version>" -A 1 -B 3
done

echo "ok? y/n"
read yn
if [ ! $yn == y ] ; then
  echo aborted
  exit 0
fi

poms=`find ${SCRIPT_DIR}| grep "/pom.xml"`
for pom in $poms ; do 
  echo " * $pom * "
  sed -i "s;<version>$current</version>;<version>$future</version>;g" ${pom}
  cat $pom | grep --color "<version>$future</version>" -A 1 -B 3
done

echo "ok? y/n"
read yn
if [ ! $yn == y ] ; then
  echo "run 'git reset --hard' in ${SCRIPT_DIR}"
  exit 0
fi
pushd ${SCRIPT_DIR}
  git diff    
popd
