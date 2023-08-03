#!/bin/bash

# 检查是否为root用户
if [ "$(id -u)" -ne 0 ]; then
    echo "Error: This script must be run as root."
    exit 1
fi

echo "::: Welcome to IM-WhaleShark :::"

# 定义要开启的端口列表
ports=(3306 13306 6379 18000 18001 19001 19002 2181 2888 3888 5672 15672)

# 开启端口
open_ports() {
    for port in "${ports[@]}"; do
        echo "Opening port $port..."
        firewall-cmd --zone=public --add-port=$port/tcp --permanent
    done
    echo "Reloading firewall..."
    firewall-cmd --reload
    echo "Ports opened successfully!"
}

# 关闭端口
close_ports() {
    for port in "${ports[@]}"; do
        echo "Closing port $port..."
        firewall-cmd --zone=public --remove-port=$port/tcp --permanent
    done
    echo "Reloading firewall..."
    firewall-cmd --reload
    echo "Ports closed successfully!"
}

# 判断是否传入参数
if [[ $# -eq 0 ]]; then
    echo "Usage: $0 [open|close]"
    exit 1
fi

# 根据传入的参数调用相应的函数
if [[ $1 == "open" ]]; then
    open_ports
elif [[ $1 == "close" ]]; then
    close_ports
else
    echo "Usage: $0 [open|close]"
    exit 1
fi

exit 0
