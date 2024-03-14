package ru.nikidzawa.datingapp.TelegramBot.stateMachine;

import ru.nikidzawa.datingapp.entities.LikeEntity;

public interface SendMessageType {
    void handleInput(Long userId, LikeEntity like);
}
