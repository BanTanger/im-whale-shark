#!/bin/sh

# 使用说明，用来提示输入参数
usage(){
	echo "Usage: sh 执行脚本.sh [base|services|stop|rm]"
	exit 1
}

# 检查是否存在docker-compose或docker compose命令
check_compose_command(){
	if command -v docker-compose >/dev/null 2>&1; then
		COMPOSE_COMMAND="docker-compose"
	elif command -v "docker compose" >/dev/null 2>&1; then
		COMPOSE_COMMAND="docker compose"
	else
		echo "Error: docker-compose or docker compose not found. Please install Docker Compose."
		exit 1
	fi
}

# 启动基础环境（必须）
base(){
	$COMPOSE_COMMAND up -d im-mysql im-redis im-zookeeper im-rabbitmq
}

# 启动程序模块（必须）
services(){
	$COMPOSE_COMMAND up -d im-domain im-message-store im-tcp
}

# 关闭所有环境/模块
stop(){
	$COMPOSE_COMMAND stop
}

# 删除所有环境/模块
rm(){
	$COMPOSE_COMMAND rm
}

# 根据输入参数，选择执行对应方法，不输入则执行使用说明
case "$1" in
"base")
	check_compose_command
	base
;;
"services")
	check_compose_command
	services
;;
"stop")
	check_compose_command
	stop
;;
"rm")
	check_compose_command
	rm
;;
*)
	usage
;;
esac
