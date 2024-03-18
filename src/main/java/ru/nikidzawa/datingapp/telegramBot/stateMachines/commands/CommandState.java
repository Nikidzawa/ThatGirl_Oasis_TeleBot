package ru.nikidzawa.datingapp.telegramBot.stateMachines.commands;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;

import java.util.Optional;

public interface CommandState {
    void handleInput (long userId, Message message, String role, Optional<UserEntity> optionalUser);
}
