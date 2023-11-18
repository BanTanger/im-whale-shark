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

# 为脚本赋予权限
cd script
chmod +x *.sh
# 清空 build 包
./clean.sh
# 将 compile 编译出来的 jar 包和 Dockerfile、resource 配置文件资源迁移到 build 包下
./copy.sh
# docker-compose 粗粒度启动所有服务，细粒度的启动请参考 deploy.sh 脚本
docker-compose up