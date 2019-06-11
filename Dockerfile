# https://spring.io/guides/gs/spring-boot-docker/
FROM amazoncorretto:11

#VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} /app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
