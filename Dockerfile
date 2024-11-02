FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY build/libs/aws-launcher-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
ENV AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
ENV AWS_REGION=${AWS_REGION}
ENV BUILD_TIMESTAMP=${BUILD_TIMESTAMP}

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/app/app.jar"]
