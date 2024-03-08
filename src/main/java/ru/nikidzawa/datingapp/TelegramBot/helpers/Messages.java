package ru.nikidzawa.datingapp.TelegramBot.helpers;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class Messages {
    @Value("${ASK_NAME}")
    String ASK_NAME;

    @Value("${ASK_AGE}")
    String ASK_AGE;

    @Value("${ASK_CITY}")
    String ASK_CITY;

    @Value("${ASK_HOBBY}")
    String ASK_HOBBY;

    @Value("${ASK_SPEND_TIME}")
    String ASK_SPEND_TIME;

    @Value("${ASK_ABOUT_ME}")
    String ASK_ABOUT_ME;

    @Value("${ASK_PHOTO}")
    String ASK_PHOTO;

    @Value("${RESULT}")
    String RESULT;

    @Value("${LEFT}")
    String LEFT;

    @Value("${INVALID_FORMAT_EXCEPTION}")
    String INVALID_FORMAT_EXCEPTION;

    @Value("${IS_NOT_A_NUMBER_EXCEPTION}")
    String IS_NOT_A_NUMBER_EXCEPTION;
}
