#!/bin/bash

echo "--------------------------------"
echo "::: Welcome to IM-WhaleShark :::"
# 创建目标目录
target_directories=(
    "docker/build/domain"
    "docker/build/message-store"
    "docker/build/tcp"
)

docker_files=(
    "im-domain/Dockerfile"
    "im-message-store/Dockerfile"
    "im-tcp/Dockerfile"
    "im-tcp/src/main/resources/config-docker.yml"
    "im-domain/src/main/resources/application-docker.yml"
    "im-message-store/src/main/resources/application-docker.yml"
    "logback-spring.xml"
)

for dir in "${target_directories[@]}"; do
    echo "开始创建目标目录 $dir/jar .."
    mkdir -p "$dir/jar"
    echo "创建目标目录 $dir/jar 完成"
done

echo ">>>>>>>>>>>>>>>>>"

# 复制 Dockerfile 文件 and 配置文件
for index in "${!docker_files[@]}"; do
    target_file="${target_directories[$index]}/$(basename "${docker_files[$index]}")"
    echo "开始复制 ${docker_files[$index]} .."
    cp "${docker_files[$index]}" "$target_file"
    echo "复制 ${docker_files[$index]} 到 $target_file 完成"
done

echo ">>>>>>>>>>>>>>>>>"

# 复制 jar 文件并更改文件名
jar_files=(
    "im-domain/target/im-domain-1.0-SNAPSHOT.jar"
    "im-message-store/target/im-message-store-1.0-SNAPSHOT.jar"
    "im-tcp/target/im-tcp-1.0-SNAPSHOT.jar"
)

for index in "${!jar_files[@]}"; do
    target_file="${target_directories[$index]}/jar/$(basename "${jar_files[$index]}")"
    target_file_without_version="${target_directories[$index]}/jar/$(basename "${jar_files[$index]}" "-1.0-SNAPSHOT.jar").jar"
    echo "开始复制 ${jar_files[$index]} .."
    cp "${jar_files[$index]}" "$target_file"
    mv "$target_file" "$target_file_without_version"
    echo "复制 ${jar_files[$index]} 到 $target_file_without_version 完成"
done