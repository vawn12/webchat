      
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy file cấu hình và source code vào Docker
COPY pom.xml .
COPY src ./src

# Docker tự chạy lệnh build (bạn không cần chạy ở ngoài)
RUN mvn clean package -DskipTests

# --- Giai đoạn 2: Chạy ứng dụng ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Lấy file .jar vừa build được ở Giai đoạn 1 ném sang đây
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]