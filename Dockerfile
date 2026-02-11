# Stage 1: Cache Gradle dependencies
FROM gradle:8.11-jdk21-alpine AS cache
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME=/home/gradle/cache_home
COPY build.gradle.kts gradle.properties settings.gradle.kts /home/gradle/cache_home/
COPY gradle /home/gradle/cache_home/gradle
COPY */build.gradle.kts /home/gradle/cache_home/
WORKDIR /home/gradle/cache_home
RUN gradle dependencies --no-daemon || true

# Stage 2: Build Application
FROM gradle:8.11-jdk21-alpine AS build
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle :app:shadowJar --no-daemon

# Stage 3: Create the Runtime Image
FROM eclipse-temurin:21-jre-alpine AS runtime
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/app/build/libs/*-all.jar /app/ktor-docker-sample.jar
ENTRYPOINT ["java","-jar","/app/ktor-docker-sample.jar"]