FROM azul/zulu-openjdk:21-jre-headless as builder

WORKDIR /app

COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
COPY src ./src

RUN ./gradlew clean bootJar --no-daemon

FROM azul/zulu-openjdk:21-jre-headless

WORKDIR /app

RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

COPY --from=builder /app/build/libs/*.jar app.jar

ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV TZ=Asia/Seoul

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
