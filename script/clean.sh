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
