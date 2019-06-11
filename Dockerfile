FROM amazoncorretto:11

VOLUME /tmp
ARG JAR_FILE
ENV ARTIFACT abc
RUN echo $ARTIFACT
COPY $ARTIFACT /app2.jar
COPY ${JAR_FILE} /app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
