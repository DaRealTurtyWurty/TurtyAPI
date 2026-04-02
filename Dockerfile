FROM eclipse-temurin:25
WORKDIR /opt/TurtyAPI/
COPY build/libs/TurtyAPI-all.jar TurtyAPI.jar
CMD ["java", "-jar", "TurtyAPI.jar", "-env", "/env/.env", "-keys", "/env/api.keys"]
