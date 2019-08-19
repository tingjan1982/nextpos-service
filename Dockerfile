# https://spring.io/guides/gs/spring-boot-docker/
# https://github.com/docker/for-mac/issues/1922
FROM amazoncorretto:11

VOLUME /tmp
ARG JAR_FILE                                                            
COPY ${JAR_FILE} /app.jar
ENV PROFILE default
ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=$PROFILE -jar /app.jar"]
