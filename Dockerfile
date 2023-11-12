FROM openjdk:19
WORKDIR /opt/TurtyAPI/
COPY build/libs/TurtyAPI-all.jar TurtyAPI.jar
CMD ["java", "-jar", "TurtyAPI.jar", "-env", "/env/.env"]