package ru.nikidzawa.datingapp.TelegramBot.botFunctions;

import ru.nikidzawa.datingapp.store.entities.like.LikeEntity;

public interface SendMessageType {
    void handleInput(Long userId, LikeEntity like);
}
