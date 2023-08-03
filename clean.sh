#!/bin/bash

echo "--------------------------------"
echo "::: Welcome to IM-WhaleShark :::"

# 删除 build 文件夹
echo "开始清理 build 文件夹 .."
if [ -d "docker/build" ]; then
  DELETED_FILES=$(find docker/build -type f -o -type d)
  rm -rf docker/build/
  echo "以下文件夹和文件已被删除:"
  echo "$DELETED_FILES" | sed 's/ /\n/g'
else
  echo "build 文件夹不存在，无需清理。"
fi
echo "清理完成"

# 删除所有停止的容器
echo "开始删除停止的容器 .."
if [ "$(docker ps -aq)" ]; then
  DELETED_CONTAINERS=$(docker rm $(docker ps -aq))
  echo "以下容器将被删除:"
  echo "$DELETED_CONTAINERS" | sed 's/ /\n/g'
else
  echo "没有停止的容器需要删除。"
fi
echo "容器删除成功"

# 删除所有未使用的镜像
echo "开始删除未使用的镜像 .."
if [ "$(docker images -q -f dangling=true)" ]; then
  DELETED_IMAGES=$(docker rmi $(docker images -q -f dangling=true))
  echo "以下镜像将被删除:"
  echo "$DELETED_IMAGES" | sed 's/ /\n/g'
else
  echo "没有未使用的镜像需要删除。"
fi
echo "镜像删除成功"
