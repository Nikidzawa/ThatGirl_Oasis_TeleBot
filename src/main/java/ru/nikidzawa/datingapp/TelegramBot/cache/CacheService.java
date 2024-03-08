package ru.nikidzawa.datingapp.TelegramBot.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import ru.nikidzawa.datingapp.TelegramBot.stateMachine.StateEnum;
import ru.nikidzawa.datingapp.entities.UserEntity;

@Service
public class CacheService {

    @Autowired
    CacheManager cacheManager;

    public Cache.ValueWrapper getCurrentState (Long userId) {
        return cacheManager.getCache("states").get(userId);
    }

    public StateEnum setState (Long userId, StateEnum stateEnum) {
        cacheManager.getCache("states").put(userId, stateEnum);
        return stateEnum;
    }

    public void evictState (Long userId) {
        cacheManager.getCache("states").evict(userId);
    }

    public Cache.ValueWrapper getEditUser (Long userId) {
        return cacheManager.getCache("edit_user").get(userId);
    }

    public void putEditUser (Long userId, UserEntity user) {
        cacheManager.getCache("edit_user").put(userId, user);
    }
}