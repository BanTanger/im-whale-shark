# 基础镜像
FROM redis:6.0.8
# author
MAINTAINER BanTanger

# 挂载目录
VOLUME /home/order/redis
# 创建目录
RUN mkdir -p /home/order/redis
# 指定路径
WORKDIR /home/order/redis
# 复制conf文件到路径
COPY ./conf/redis.conf /home/order/redis/redis.conf

# 指定 ENTRYPOINT 命令，设置默认的参数为加载配置文件的路径
ENTRYPOINT ["redis-server"]
# 设置默认的 CMD 参数为配置文件路径
CMD ["/home/order/redis/redis.conf"]
