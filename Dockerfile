FROM openjdk:17-oracle
MAINTAINER com.gmail.ko4evneg
COPY target/dunebot_dev.jar dunebot.jar
ENTRYPOINT ["java","-jar","-Dspring.profiles.active=prod","-Duser.timezone=Europe/Moscow", \
"-agentlib:jdwp=transport=dt_socket,server=y,address=*:8017,suspend=y","/dunebot.jar"]
