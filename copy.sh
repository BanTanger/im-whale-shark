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
    echo "开始创建目标目录 $dir/jar .."
    mkdir -p "$dir/jar"
    echo "创建目标目录 $dir/jar 完成"
done

echo ">>>>>>>>>>>>>>>>>"

docker_files=(
    "im-domain/Dockerfile"
    "im-message-store/Dockerfile"
    "im-tcp/Dockerfile"
)

# 复制 Dockerfile
for index in "${!docker_files[@]}"; do
    target_file="${target_directories[$index]}/$(basename "${docker_files[$index]}")"
    echo "开始复制 ${docker_files[$index]} .."
    cp "${docker_files[$index]}" "$target_file"
    echo "复制 ${docker_files[$index]} 到 $target_file 完成"
done

echo ">>>>>>>>>>>>>>>>>"

config_files=(
    "im-message-store/src/main/resources/application-docker.yml"
    "im-domain/src/main/resources/application-docker.yml"
    "im-tcp/src/main/resources/config-docker.yml"
)

# 复制配置文件
for index in "${!config_files[@]}"; do
    target_file="${target_directories[$index]}/$(basename "${config_files[$index]}")"
    echo "开始复制 ${config_files[$index]} .."
    cp "${config_files[$index]}" "$target_file"
    cp "logback-spring.xml" "${target_directories[$index]}"
    echo "复制 ${config_files[$index]} 到 $target_file 完成"
done

echo ">>>>>>>>>>>>>>>>>"

# 复制 jar 文件并更改文件名
jar_files=(
    "im-domain/target/im-domain-1.0-SNAPSHOT.jar"
    "im-message-store/target/im-message-store-1.0-SNAPSHOT.jar"
    "im-tcp/target/im-tcp-1.0-SNAPSHOT-jar-with-dependencies.jar"
)

for index in "${!jar_files[@]}"; do
    target_file="${target_directories[$index]}/jar/$(basename "${jar_files[$index]}")"
    target_file_without_version="${target_directories[$index]}/jar/$(basename "${jar_files[$index]}" "-1.0-SNAPSHOT.jar").jar"
    if [[ "${jar_files[$index]}" == *"jar-with-dependencies.jar" ]]; then
        target_file_without_version="${target_directories[$index]}/jar/$(basename "${jar_files[$index]}" "-1.0-SNAPSHOT-jar-with-dependencies.jar").jar"
    fi
    echo "开始复制 ${jar_files[$index]} .."
    cp "${jar_files[$index]}" "$target_file"
    mv "$target_file" "$target_file_without_version"
    echo "复制 ${jar_files[$index]} 到 $target_file_without_version 完成"
done
