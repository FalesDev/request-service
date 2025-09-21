FROM gradle:8.5.0-jdk21 AS build

WORKDIR /home/gradle/src

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle/

RUN ./gradlew build --no-daemon || true

COPY . .

RUN ./gradlew clean build -x validateStructure -x test --no-daemon


FROM eclipse-temurin:21-jdk-alpine

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/./urandom"

VOLUME /tmp

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=build /home/gradle/src/applications/app-service/build/libs/*.jar request-service.jar

RUN chown -R appuser:appgroup /app

USER appuser

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar request-service.jar" ]