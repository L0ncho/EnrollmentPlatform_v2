FROM eclipse-temurin:21-jdk AS build
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY Wallet_ENROLLMENTPLATFORMDB ./wallet
ENV TNS_ADMIN=/app/wallet
ENV ORACLE_WALLET_DIR=/app/wallet
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/EnrollmentPlatform-0.0.1-SNAPSHOT.jar app.jar
COPY Wallet_ENROLLMENTPLATFORMDB ./wallet
ENV TNS_ADMIN=/app/wallet
ENV ORACLE_WALLET_DIR=/app/wallet
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
