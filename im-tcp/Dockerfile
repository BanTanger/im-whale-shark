FROM openjdk:8-jre

MAINTAINER BanTanger

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
RUN echo 'Asia/Shanghai' >/etc/timezone

EXPOSE 19001
EXPOSE 19002

COPY jar/im-tcp.jar /opt/application/im-tcp.jar

# 复制配置文件到 /opt/application 目录下
COPY config-docker.yml /opt/application/config.yml

ENTRYPOINT ["java", "-jar", "/opt/application/im-tcp.jar", "/opt/application/config.yml"]