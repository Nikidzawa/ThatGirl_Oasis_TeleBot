package ru.nikidzawa.datingapp.telegramBot.botFunctions;

import ru.nikidzawa.datingapp.store.entities.like.LikeEntity;

public interface SendMessageType {
    void handleInput(Long userId, LikeEntity like);
}
