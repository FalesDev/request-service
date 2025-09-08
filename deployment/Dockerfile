FROM eclipse-temurin:21-jdk-alpine
VOLUME /tmp
COPY applications/app-service/build/libs/request-service.jar request-service.jar
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/./urandom"
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -jar request-service.jar" ]
