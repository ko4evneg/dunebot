FROM openjdk:17-oracle
LABEL org.opencontainers.image.authors="ko4evneg@gmail.com"
COPY target/dunebot.jar dunebot.jar
ENTRYPOINT ["java","-jar","-Dspring.profiles.active=prod","-Duser.timezone=Europe/Moscow", \
"-agentlib:jdwp=transport=dt_socket,server=y,address=*:8017,suspend=n","/dunebot.jar"]
