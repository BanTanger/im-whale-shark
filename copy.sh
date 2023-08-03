#!/bin/bash

echo "--------------------------------"
echo "::: Welcome to IM-WhaleShark :::"

# 创建目标目录
target_directories=(
    "docker/build/domain"
    "docker/build/message-store"
    "docker/build/tcp"
)

for dir in "${target_directories[@]}"; do
    echo "开始创建目标目录 $dir .."
    mkdir -p "$dir" || echo "创建目标目录 $dir 失败"
    echo "创建目标目录 $dir 完成"
done

echo ">>>>>>>>>>>>>>>>>"

# 复制 Dockerfile 文件
docker_files=(
    "im-domain/Dockerfile"
    "im-message-store/Dockerfile"
    "im-tcp/Dockerfile"
)

for index in "${!docker_files[@]}"; do
    target_dir="${target_directories[$index]}"
    target_file="$target_dir/$(basename "$index")"
    docker_file="${docker_files[$index]}"
    echo "开始复制 $docker_file .."
    cp "$docker_file" "$target_file" || echo "复制 $docker_file 到 $target_file" 失败"
    echo "复制 $docker_file 到 $target_file" 完成"
done

echo ">>>>>>>>>>>>>>>>>"

# 复制 jar 文件
jar_files=(
    "im-domain/target/im-domain"
    "im-message-store/target/im-message-store"
    "im-tcp/target/im-tcp"
)

for file in "${jar_files[@]}"; do
    target_dir="${target_directories[$file]}"
    target_file="$target_dir/$(basename "$file").jar"
    echo "开始复制 $file-1.0-SNAPSHOT .."
    cp "$file-1.0-SNAPSHOT.jar" "$target_file" || echo "复制 $file-1.0-SNAPSHOT.jar 到 $target_file 失败"
    echo "复制 $file-1.0-SNAPSHOT 到 $target_file 完成"
done

echo ">>>>>>>>>>>>>>>>>"

# 展示创建的文件夹名称
echo "以下文件夹已被创建:"
for dir in "${target_directories[@]}"; do
    echo "$dir"
done

echo ">>>>>>>>>>>>>>>>>"

# 展示移动过程
echo "以下文件已被移动:"
for file in "${docker_files[@]}"; do
    target_file="docker/build/$(basename "$file")"
    echo "$file -> $target_file"
done

echo ">>>>>>>>>>>>>>>>>"

# 展示复制过程
echo "以下 jar 文件已被复制:"
for file in "${jar_files[@]}"; do
    target_file="docker/build/$(basename "$file")"
    echo "$file -> $target_file"
done
