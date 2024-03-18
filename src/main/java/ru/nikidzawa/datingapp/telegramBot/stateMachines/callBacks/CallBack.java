package ru.nikidzawa.datingapp.telegramBot.stateMachines.callBacks;

public interface CallBack {
    void handleCallback(Long myId, Long anotherUserId);
}
