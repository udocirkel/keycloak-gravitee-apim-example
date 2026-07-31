FROM eclipse-temurin:21-jdk as builder
WORKDIR /app
COPY maven/*-exec.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted
FROM eclipse-temurin:21-jdk
WORKDIR /app
ENV TZ="Europe/Berlin"
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
