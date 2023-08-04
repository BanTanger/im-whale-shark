FROM openjdk:8-jre

MAINTAINER BanTanger

#ARG PROVIDER_JAR
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
RUN echo 'Asia/Shanghai' >/etc/timezone

COPY ./jar/im-message-store.jar /opt/application/im-message-store.jar
COPY application-docker.yml /opt/application/application.yml

ENTRYPOINT ["java", "-jar" ,"/opt/application/im-message-store.jar"]
