package ru.nikidzawa.datingapp.TelegramBot;

import lombok.SneakyThrows;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class BotFunctions {

    private final TelegramBot telegramBot;

    public BotFunctions (TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    @SneakyThrows
    public void sendMessage (Long id, String message){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(id));
        sendMessage.setText(message);
        telegramBot.execute(sendMessage);
    }
}
