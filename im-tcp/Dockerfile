FROM openjdk:8-jre

MAINTAINER BanTanger

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
RUN echo 'Asia/Shanghai' >/etc/timezone

COPY jar/im-tcp.jar /opt/application/im-tcp.jar
COPY im-codec/target/classes /opt/application/codec

# 复制配置文件到 /opt/application 目录下
COPY im-tcp/src/main/resources/config.yml /opt/application/config.yml

# 添加 SnakeYAML 依赖
ADD https://repo1.maven.org/maven2/org/yaml/snakeyaml/1.28/snakeyaml-1.28.jar /opt/application/snakeyaml.jar

ENTRYPOINT ["java", "-cp", "/opt/application/codec:/opt/application/im-tcp.jar:/opt/application/snakeyaml-1.28.jar", "com.bantanger.im.tcp.Starter", "--spring.config.location=/opt/application/config.yml"]