# 基础镜像
FROM mysql:8.0.11

# author
MAINTAINER BanTanger

# 执行sql脚本
# `/docker-entrypoint-initdb.d/`是MySQL官方镜像中的一个特殊目录，用于存放初始化数据库的脚本文件。
ADD ./db/*.sql /docker-entrypoint-initdb.d/
