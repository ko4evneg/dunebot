FROM openjdk:17-oracle
MAINTAINER com.gmail.ko4evneg
COPY target/dunebot-0.0.1-SNAPSHOT.jar dunebot.jar
ENTRYPOINT ["java","-jar","-Dspring.profiles.active=prod","/dunebot.jar"]
