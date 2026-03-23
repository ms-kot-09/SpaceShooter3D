#!/bin/sh
#
# Copyright © 2015-2021 the original authors.
# Gradle wrapper shell script.
#

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

MAX_FD="maximum"

warn () { echo "$*"; }
die () { echo; echo "ERROR: $*"; echo; exit 1; }

OS="`uname`"
case "$OS" in
  CYGWIN*|MINGW*|MSYS*|Windows*) cygwin=true ;;
  Darwin*) darwin=true ;;
  NONSTOP*) nonstop=true ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

APP_HOME=`cd "${APP_HOME:-./}" && pwd -P` || exit

exec "$JAVACMD" \
  $DEFAULT_JVM_OPTS \
  $JAVA_OPTS \
  $GRADLE_OPTS \
  "-Dorg.gradle.appname=$APP_BASE_NAME" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
