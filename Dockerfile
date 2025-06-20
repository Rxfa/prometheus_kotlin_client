FROM openjdk:21-jdk-slim

WORKDIR /app

COPY . .

RUN chmod +x ./gradlew

EXPOSE 8080

CMD ./gradlew :ktor-example:runFatJar
