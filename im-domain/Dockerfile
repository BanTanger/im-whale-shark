FROM openjdk:8-jre

MAINTAINER BanTanger

#ARG PROVIDER_JAR
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
RUN echo 'Asia/Shanghai' >/etc/timezone

COPY im-domain-1.0-SNAPSHOT.jar /opt/application/im-domain-1.0-SNAPSHOT.jar
COPY application.yml application.yml

ENTRYPOINT ["java", "-jar" ,"/opt/application/im-domain-1.0-SNAPSHOT.jar"]
