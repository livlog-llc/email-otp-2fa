#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

set -euf

# OS specific support.  $var _must_ be set to either true or false.
darwin=false
case "$(uname)" in
  Darwin*) darwin=true ;;
  MINGW*|MSYS*|CYGWIN*)
    # On Windows, the shell can be msys, cygwin, git bash, or busybox
    # each of which have a different way of handling paths.
    ;;
esac

# set JAVACMD if not already set
if [ -z "${JAVACMD+x}" ]; then
  if [ -n "${JAVA_HOME+x}" ]; then
    if [ -x "$JAVA_HOME/jre/sh/java" ]; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD="$(\unset -f command 2>/dev/null; \command -v java)"
  fi
fi

if [ ! -x "$JAVACMD" ]; then
  echo "Error: JAVA_HOME is not defined correctly." >&2
  echo "  We cannot execute $JAVACMD" >&2
  exit 1
fi

# traverses directory structure from process work directory to filesystem root
# first directory with .mvn subdirectory is considered project base directory
find_maven_basedir() {
  if [ -z "$1" ]; then
    echo "Path not specified to find_maven_basedir" >&2
    return 1
  fi

  basedir="$1"
  wdir="$1"
  while [ "$wdir" != '/' ]; do
    if [ -d "$wdir/.mvn" ]; then
      basedir="$wdir"
      break
    fi
    # workaround for JBEAP-8937 (on Solaris 10/Sparc)
    if [ -d "$wdir" ]; then
      wdir=$(cd "$wdir/.."; pwd)
    fi
    # end of workaround
  done
  printf '%s' "$basedir"
}

# concatenates all lines of a file
concat_lines() {
  if [ -f "$1" ]; then
    tr -s '\n' ' ' < "$1"
  fi
}

log() {
  if [ "${MVNW_VERBOSE-}" = true ]; then
    printf '%s\n' "$1"
  fi
}

BASE_DIR=$(find_maven_basedir "$(pwd)") || exit $?
[ -f "$BASE_DIR/.mvn/wrapper/maven-wrapper.conf" ] && . "$BASE_DIR/.mvn/wrapper/maven-wrapper.conf"

MAVEN_PROJECTBASEDIR=${MAVEN_BASEDIR:-"$BASE_DIR"}
export MAVEN_PROJECTBASEDIR
log "Project base directory: $MAVEN_PROJECTBASEDIR"

MAVEN_CMD_LINE_ARGS="$*"
export MAVEN_CMD_LINE_ARGS

WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

wrapperJarPath="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
if [ ! -r "$wrapperJarPath" ]; then
  log "Couldn't find $wrapperJarPath, downloading it ..."

  if [ -n "${MVNW_REPOURL-}" ]; then
    wrapperUrl="$MVNW_REPOURL/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
  else
    wrapperUrl="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
  fi
  while IFS="=" read -r key value; do
    case "$key" in
      wrapperUrl) wrapperUrl="$value" ;;
    esac
  done < "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

  log "Downloading from: $wrapperUrl"
  wrapperJarDir=$(dirname "$wrapperJarPath")
  mkdir -p "$wrapperJarDir"
  if command -v wget >/dev/null 2>&1; then
    wget -q "$wrapperUrl" -O "$wrapperJarPath"
  elif command -v curl >/dev/null 2>&1; then
    curl -s -f -L "$wrapperUrl" -o "$wrapperJarPath"
  else
    echo "Error: Please install wget or curl" >&2
    exit 1
  fi
fi

if [ -n "${MVNW_VERBOSE-}" ]; then
  set -x
fi

exec "$JAVACMD" \
  ${MAVEN_OPTS:-""} \
  ${MAVEN_DEBUG_OPTS:-""} \
  -classpath "$wrapperJarPath" \
  "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
  ${WRAPPER_LAUNCHER} "$@"
