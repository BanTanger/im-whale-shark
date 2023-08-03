FROM openjdk:8-jre

MAINTAINER BanTanger

#ARG PROVIDER_JAR
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
RUN echo 'Asia/Shanghai' >/etc/timezone

COPY docker/build/tcp/jar/im-domain.jar /opt/application/im-tcp.jar
COPY src/main/resources/config.yml config.yml

ENTRYPOINT ["java", "-jar" ,"/opt/application/im-tcp.jar"]
