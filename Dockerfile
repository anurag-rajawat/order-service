FROM gcr.io/distroless/java17-debian11

WORKDIR workspace

COPY maven/build/libs/*.jar order-service.jar

EXPOSE 9002

CMD ["order-service.jar"]