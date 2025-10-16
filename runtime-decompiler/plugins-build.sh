#!/bin/bash
echo "Originally all plugins were available as maven projects."
echo "Unluckily as years passed, some of them stopped to release, moved to other build systems"
echo "or simply died, despite being still working fie. This script is building them and populating them to maven cache"

## resolve folder of this script, following all symlinks,
## http://stackoverflow.com/questions/59895/can-a-bash-script-tell-what-directory-its-stored-in
SCRIPT_SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SCRIPT_SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
    PLUGINS_SCRIPT_DIR="$( cd -P "$( dirname "$SCRIPT_SOURCE" )" && pwd )"
    SCRIPT_SOURCE="$(readlink "$SCRIPT_SOURCE")"
    # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
    [[ $SCRIPT_SOURCE != /* ]] && SCRIPT_SOURCE="$PLUGINS_SCRIPT_DIR/$SCRIPT_SOURCE"
done
readonly PLUGINS_SCRIPT_DIR="$( cd -P "$( dirname "$SCRIPT_SOURCE" )" && pwd )"


set -ex

targetDir="${PLUGINS_SCRIPT_DIR}/target"
mavenDir="$HOME/.m2/repository"
if [ -z "$procyonVersion" ] ; then
  #TODO align with pom.xml? See jcommander...
  #procyonVersion=0.6.0
  procyonVersion=1.0-SNAPSHOT #TODO, move to 0.6.0?
fi
procyonName=procyon
if [ -z "$jdVersion" ] ; then
  #TODO align with pom.xml? See jcommander...
  jdVersion=1.1.3
fi
jdName=jd-core
if [ -z "$jasmVersionCheckout" -o -z "$jasmVersionName" ] ; then
  #TODO align with pom.xml?
  jasmVersionCheckout=master
  jasmVersionName=9.0.b12-ea #not needed?
fi
jasmName=asmtools
if [ -z "$jasmLegacyVersionCheckout" -o -z "$jasmLegacyVersionName" ] ; then
  #TODO align with pom.xml?
  jasmLegacyVersionCheckout=at7
  jasmLegacyVersionName=7.0.b10-ea  #not needed?
fi
jasmnLegacyName=asmtools-core

function setup() {
    mkdir -p ${targetDir}
}

function buildProcyon() {
  local sourceVersion="-source 8 -target 8 -g"
  pushd ${targetDir}
    if [ ! -e ${procyonName} ] ; then
      git clone https://github.com/mstrobel/procyon.git
    else
      # to avoid version clash
      rm -rf procyon/build/
    fi
    pushd ${procyonName}
      git checkout develop
      mkdir -p build/Procyon.CompilerTools/{libs,classes}
      mkdir -p build/Procyon.Core/{libs,classes}
      mkdir -p build/Procyon.Decompiler/{libs,classes}
      mkdir -p build/Procyon.Expressions/{libs,classes}
      mkdir -p build/Procyon.Reflection/{libs,classes}
      javac $sourceVersion -d build/Procyon.Core/classes/ ` find Procyon.Core/src/main/java -type f | grep    "\.java"`
      javac $sourceVersion -d build/Procyon.Reflection/classes/        -cp build/Procyon.Core/classes/ ` find Procyon.Reflection/src/main/java -type f | grep    "\.java"`
      javac $sourceVersion -d build/Procyon.Expressions/classes/     -cp build/Procyon.Core/classes/:build/Procyon.Reflection/classes/ ` find Procyon.Expressions/src/main/java -type f | grep    "\.java"`
      javac $sourceVersion -d build/Procyon.CompilerTools/classes/ -cp build/Procyon.Core/classes/ ` find Procyon.CompilerTools/src/main/java -type f | grep    "\.java"`
      # pack the jars
      for x in Procyon.CompilerTools Procyon.Core Procyon.Reflection Procyon.Expressions ; do
          pushd build/$x/classes/
              local project=`echo $x | sed -e "s/Procyon.//"    | sed -e 's/\(.*\)/\L\1/'`
              jar -cf ../../../build/$x/libs/${procyonName}-$project-${procyonVersion}.jar com
          popd
      done
      if [ -z "$JCOMMANDER" ] ; then
        echo "no JCOMMANDER set. Using default"
        echo "to get the default jcommander, run  mvn dependency:resolve || echo 'is ok to fail for this case'"
        local jcomName=`cat ${PLUGINS_SCRIPT_DIR}/pom.xml | grep  "jcommander</artifactId>" | sed "s/.*<artifactId>//" | sed "s/<.*//"`
        local jcomVer=`cat ${PLUGINS_SCRIPT_DIR}/pom.xml | grep  "jcommander</artifactId>" -A 1 | tail -n 1 | sed "s/.*<version>//" | sed "s/<.*//"`
        JCOMMANDER="$mavenDir/com/beust/${jcomName}/${jcomVer}/jcommander-${jcomVer}.jar"
      fi
      # create main/launcher jar to be used
      mkdir build/launcher-minimal
      mkdir build/launcher-minimal/classes
      javac $sourceVersion -cp    build/Procyon.Core/classes/:build/Procyon.CompilerTools/classes/:$JCOMMANDER    -d build/launcher-minimal/classes ` find Procyon.Decompiler/src/main/java -type f | grep    "\.java"`
      # pack the minimal jar
      pushd build/launcher-minimal/classes/
          jar -cf ../../../build/Procyon.Decompiler/libs/${procyonName}-decompiler-${procyonVersion}.jar com
      popd
    popd
  popd
}

function installProcyon() {
  local jars=`find ${targetDir}/${procyonName} | grep -v "gradle-wrapper.jar" | grep "\\.jar$"`
  for jar in $jars ; do
    echo $jar
    local name=`basename $jar`
    local pureName=`basename $jar | sed "s/-$procyonVersion.*//"`
    local dest="$mavenDir/com/github/mstrobel/$pureName/$procyonVersion/"
    mkdir -p "$dest"
    cp -v "$jar" "$dest"
  done 
}

function buildJd() {
  local sourceVersion="-source 8 -target 8 -g"
  pushd ${targetDir}
    if [ ! -e ${jdName} ] ; then
      git clone  https://github.com/java-decompiler/$jdName
    else
      # to avoid version clash
      rm -rf ${jdName}/build/
    fi
    pushd $jdName
      git checkout "v$jdVersion"
      rm -rf build
      mkdir build
      javac $sourceVersion `find src/main/java/   -type f ` -d build
      jar -cf $jdName-$jdVersion.jar -C build org
    popd
  popd
}

function installJd() {
  local mavenTarget="$mavenDir/org/jd/$jdName/$jdVersion/"
  rm -rf   $mavenTarget
  mkdir -p $mavenTarget
  cp  ${targetDir}/${jdName}/$jdName-$jdVersion.jar $mavenTarget
}

function buildJasm() {
    pushd ${targetDir}
      if [ ! -e ${jasmName} ] ; then
        git clone https://github.com/openjdk/$jasmName
      else
        # to avoid version clash
        rm -rf ${jasmName}/maven/target
      fi
      pushd $jasmName
        git checkout  $jasmVersionCheckout 
        cd maven/
        bash mvngen.sh
        mvn clean package -DskipTests
      popd
    popd
}

function installJasm() {
  pushd ${targetDir}/$jasmName/maven/
    mvn install -DskipTests
  popd
}


function cleanAll() {
  echo "todo"
}

#fixme, not working due to PLUGINS_SCRIPT_DIR
if [ ! "x$JUST_LIB" == "xtrue" ] ; then
  setup
  #if procyon...?
  buildProcyon
  installProcyon
  #if jd...?
  buildJd
  installJd
  #if jasm?
  buildJasm
  installJasm
fi

