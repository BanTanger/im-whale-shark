FROM openjdk:8-jre

MAINTAINER BanTanger

#ARG PROVIDER_JAR
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
RUN echo 'Asia/Shanghai' >/etc/timezone

COPY ./jar/im-tcp.jar /opt/application/im-tcp.jar

ENTRYPOINT ["java", "-jar", "/opt/application/im-tcp.jar",
"--spring.config.location=/opt/application/im-whale-shark/im-tcp/src/main/resources/config.yml"]
