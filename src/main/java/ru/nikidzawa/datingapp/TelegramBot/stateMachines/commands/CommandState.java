package ru.nikidzawa.datingapp.TelegramBot.stateMachines.commands;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;

import java.util.Optional;

public interface CommandState {
    void handleInput (long userId, Message message, String role, Optional<UserEntity> optionalUser);
}
