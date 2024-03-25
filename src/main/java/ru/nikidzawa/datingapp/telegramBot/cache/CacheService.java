package ru.nikidzawa.datingapp.telegramBot.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.mainStates.StateEnum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {

    @Autowired
    CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Cache.ValueWrapper getCurrentState (Long userId) {
        return cacheManager.getCache("states").get(userId);
    }

    public void setState (Long userId, StateEnum stateEnum) {
        cacheManager.getCache("states").put(userId, stateEnum);
    }

    public void evictState (Long userId) {
        cacheManager.getCache("states").evict(userId);
    }

    @SneakyThrows
    public UserEntity getCachedUser(Long userId) {
        String cachedUserJson = redisTemplate.opsForValue().get("cached_user_" + userId);
        if (cachedUserJson != null) {
            return objectMapper.readValue(cachedUserJson, UserEntity.class);
        }
        return null;
    }

    @SneakyThrows
    public void putCachedUser(Long userId, UserEntity user) {
        String cachedUserJson = objectMapper.writeValueAsString(user);
        redisTemplate.opsForValue().set("cached_user_" + userId, cachedUserJson);
        redisTemplate.expire("cached_user_" + userId, 3, TimeUnit.HOURS);
    }

    public void evictCachedUser(Long userId) {
        redisTemplate.delete("cached_user_" + userId);
    }

    public Long getComplaintUserId(Long complainSenderId) {
        return (Long) cacheManager.getCache("complain_user_id").get(complainSenderId).get();
    }

    public void putComplaintUser(Long complainSenderId, Long complaintUserId) {
        cacheManager.getCache("complain_user_id").put(complainSenderId, complaintUserId);
    }

    public void evictComplaintUser(Long complainSenderId) {
        cacheManager.getCache("complain_user_id").evict(complainSenderId);
    }

    @SneakyThrows
    public List<UserEntity> getCachedProfiles(Long userId) {
        String cachedProfilesJson = redisTemplate.opsForValue().get("recommendations_" + userId);
        if (cachedProfilesJson != null) {
            UserEntity[] userEntities = objectMapper.readValue(cachedProfilesJson, UserEntity[].class);
            return new ArrayList<>(Arrays.asList(userEntities));
        }
        return null;
    }

    @SneakyThrows
    public void putCachedProfiles(Long userId, List<UserEntity> profiles) {
        String cachedAvatarsJson = objectMapper.writeValueAsString(profiles);
        redisTemplate.opsForValue().set("recommendations_" + userId, cachedAvatarsJson);
        redisTemplate.expire("recommendations_" + userId, 3, TimeUnit.HOURS);
    }

    public void evictCachedProfiles(Long userId, UserEntity entityToRemove, List<UserEntity> cachedProfiles) {
        cachedProfiles.removeIf(userEntity -> Objects.equals(userEntity.getId(), entityToRemove.getId()));
        putCachedProfiles(userId, cachedProfiles);
    }
}