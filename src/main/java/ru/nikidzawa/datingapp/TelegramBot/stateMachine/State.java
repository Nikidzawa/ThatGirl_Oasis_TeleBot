package ru.nikidzawa.datingapp.TelegramBot.stateMachine;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.nikidzawa.datingapp.entities.UserEntity;

public interface State {
    void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered);
}
