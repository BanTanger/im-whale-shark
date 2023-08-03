#!/bin/bash

# 创建目标目录
mkdir -p build/domain/jar
mkdir -p build/message-store/jar
mkdir -p build/tcp/jar

# 复制 jar 文件
echo "begin copy jar "
cp ../im-domain/target/im-domain-1.0-SNAPSHOT.jar build/domain/jar/im-domain.jar
cp ../im-message-store/target/im-message-store-1.0-SNAPSHOT.jar build/message-store/jar/im-message-store.jar
cp ../im-tcp/target/im-tcp-1.0-SNAPSHOT.jar  build/tcp/jar/im-tcp.jar
echo "end copy jar "

# 复制 Dockerfile 文件
echo "begin copy Dockerfile "
cp ../im-domain/Dockerfile build/domain/jar/Dockerfile
cp ../im-message-store/Dockerfile build/message-store/Dockerfile
cp ../im-tcp/target/Dockerfile  build/tcp/Dockerfile
echo "end copy Dockerfile "