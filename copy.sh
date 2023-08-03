#!/bin/bash

echo "::: Welcome to IM-WhaleShark :::"
# 创建目标目录
mkdir -p docker/build/domain/jar
mkdir -p docker/build/message-store/jar
mkdir -p docker/build/tcp/jar

# 复制 Dockerfile 文件
echo "begin copy Dockerfile .."
cp im-domain/Dockerfile docker/build/domain/Dockerfile
cp im-message-store/Dockerfile docker/build/message-store/Dockerfile
cp im-tcp/Dockerfile  docker/build/tcp/Dockerfile
echo "end copy Dockerfile "

echo ">>>>>>>>>>>>>>>>>"

# 复制 jar 文件
echo "begin copy jar .."
cp im-domain/target/im-domain-1.0-SNAPSHOT.jar docker/build/domain/jar/im-domain.jar
cp im-message-store/target/im-message-store-1.0-SNAPSHOT.jar docker/build/message-store/jar/im-message-store.jar
cp im-tcp/target/im-tcp-1.0-SNAPSHOT.jar  docker/build/tcp/jar/im-tcp.jar
echo "end copy jar "