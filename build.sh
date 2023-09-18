#!/usr/bin/env bash
# Be sure your script exits whenever encounter errors
set -e
# Be sure your charset is correct. eg: zh_CN.UTF-8
export LC_ALL=en_US.UTF-8
export LANG=en_US.UTF-8
export LANGUAGE=en_US.UTF-8
## set env
#export JAVA_HOME=$ORACLEJDK_1_8_0_HOME
#export PATH=$ORACLEJDK_1_8_0_BIN:$PATH
#export MAVEN_HOME=$MAVEN_3_5_3_HOME
#export PATH=$MAVEN_3_6_3_BIN:$PATH

# compile
mvn -U clean install -Dmaven.javadoc.skip=true -Dmaven.test.skip=true