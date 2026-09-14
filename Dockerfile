FROM eclipse-temurin:21-jdk-alpine AS base
WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY sentinel/pom.xml sentinel/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY inventory-service/pom.xml inventory-service/pom.xml

FROM eclipse-temurin:21-jre-alpine AS runtime

# --- sentinel ---
FROM base AS sentinel-build
COPY sentinel sentinel
RUN ./mvnw -pl sentinel -am -DskipTests package

FROM runtime AS sentinel-runtime
WORKDIR /app
COPY --from=sentinel-build /app/sentinel/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# --- payment-service ---
FROM base AS payment-build
COPY payment-service payment-service
RUN ./mvnw -pl payment-service -am -DskipTests package

FROM runtime AS payment-runtime
WORKDIR /app
COPY --from=payment-build /app/payment-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# --- order-service ---
FROM base AS order-build
COPY order-service order-service
RUN ./mvnw -pl order-service -am -DskipTests package

FROM runtime AS order-runtime
WORKDIR /app
COPY --from=order-build /app/order-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# --- inventory-service ---
FROM base AS inventory-build
COPY inventory-service inventory-service
RUN ./mvnw -pl inventory-service -am -DskipTests package

FROM runtime AS inventory-runtime
WORKDIR /app
COPY --from=inventory-build /app/inventory-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]