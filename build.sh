#!/bin/sh

export JAVA_HOME=`/usr/libexec/java_home -v 1.8`
java -version

mvn clean

mvn clean install -DskipTests=true

