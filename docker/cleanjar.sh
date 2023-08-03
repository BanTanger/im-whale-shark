#!/bin/bash

# 删除jar文件
echo "开始清理jar文件"
rm -f ../build/domain/jar/im-domain.jar
rm -f ../build/message-store/jar/im-message-store.jar
rm -f ../build/tcp/jar/im-tcp.jar
rm -rf ../build
echo "清理完成"