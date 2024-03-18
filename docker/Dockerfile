FROM openjdk:21

ARG TELEBOT=*.jar

COPY ${TELEBOT} telebot.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "telebot.jar"]