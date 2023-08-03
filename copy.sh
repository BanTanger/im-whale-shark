#!/bin/bash

echo "--------------------------------"
echo "::: Welcome to IM-WhaleShark :::"

# 创建目标目录
target_directories=(
    "docker/build/domain/jar"
    "docker/build/message-store/jar"
    "docker/build/tcp/jar"
)

for dir in "${target_directories[@]}"; do
    echo "开始创建目标目录 $dir .."
    mkdir -p "$dir" || echo "创建目标目录 $dir 失败"
    echo "创建目标目录 $dir 完成"
done

# 复制 Dockerfile 文件
docker_files=(
    "im-domain/Dockerfile"
    "im-message-store/Dockerfile"
    "im-tcp/Dockerfile"
)

for file in "${docker_files[@]}"; do
    target_file="docker/build/$(basename "$file")"
    echo "开始复制 $file .."
    cp "$file" "$target_file" || echo "复制 $file 到 $target_file 失败"
    echo "复制 $file 到 $target_file 完成"
done

# 复制 jar 文件
jar_files=(
    "im-domain/target/im-domain-1.0-SNAPSHOT.jar"
    "im-message-store/target/im-message-store-1.0-SNAPSHOT.jar"
    "im-tcp/target/im-tcp-1.0-SNAPSHOT.jar"
)

for file in "${jar_files[@]}"; do
    target_file="docker/build/$(basename "$file")"
    echo "开始复制 $file .."
    cp "$file" "$target_file" || echo "复制 $file 到 $target_file 失败"
    echo "复制 $file 到 $target_file 完成"
done

echo ">>>>>>>>>>>>>>>>>"

# 展示创建的文件夹名称
echo "以下文件夹已被创建:"
for dir in "${target_directories[@]}"; do
    echo "$dir"
done

# 展示移动过程
echo "以下文件已被移动:"
for file in "${docker_files[@]}"; do
    target_file="docker/build/$(basename "$file")"
    echo "$file -> $target_file"
done

# 展示复制过程
echo "以下 jar 文件已被复制:"
for file in "${jar_files[@]}"; do
    target_file="docker/build/$(basename "$file")"
    echo "$file -> $target_file"
done
