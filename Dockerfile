FROM azul/zulu-openjdk-alpine:25-latest AS builder

WORKDIR /app

COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
COPY src ./src

RUN ./gradlew clean bootJar --no-daemon

FROM azul/zulu-openjdk-alpine:25-jre-headless-latest

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/build/libs/*.jar app.jar

ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV TZ=Asia/Seoul

EXPOSE 8100

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
