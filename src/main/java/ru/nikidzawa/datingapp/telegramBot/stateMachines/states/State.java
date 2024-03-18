package ru.nikidzawa.datingapp.telegramBot.stateMachines.states;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;

public interface State {
    void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered);
}
