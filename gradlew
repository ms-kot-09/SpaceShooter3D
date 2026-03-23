#!/usr/bin/env sh
APP_HOME=$( cd "${APP_HOME:-./}" && pwd -P ) || exit
APP_BASE_NAME=$( basename "$0" )
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Find java
if [ -n "$JAVA_HOME" ]; then
    JAVACMD=$JAVA_HOME/bin/java
else
    JAVACMD=java
fi

exec "$JAVACMD" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
