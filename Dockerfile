FROM openjdk:20
WORKDIR /opt/TurtyLib/
COPY build/libs/TurtyLib-all.jar TurtyLib.jar
CMD ["java", "-jar", "TurtyLib.jar"]