#!/bin/bash

# 删除jar文件
echo "开始清理jar文件"
rm -f ../im-whale-shark/domain/jar/im-domain.jar
rm -f ../im-whale-shark/message-store/jar/im-message-store.jar
rm -f ../im-whale-shark/tcp/jar/im-tcp.jar
echo "清理完成"