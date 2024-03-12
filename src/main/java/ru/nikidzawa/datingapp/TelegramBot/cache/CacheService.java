package ru.nikidzawa.datingapp.TelegramBot.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ru.nikidzawa.datingapp.TelegramBot.services.DataBaseService;
import ru.nikidzawa.datingapp.TelegramBot.stateMachine.StateEnum;
import ru.nikidzawa.datingapp.entities.UserEntity;

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

    public StateEnum setState (Long userId, StateEnum stateEnum) {
        cacheManager.getCache("states").put(userId, stateEnum);
        return stateEnum;
    }

    public void evictState (Long userId) {
        cacheManager.getCache("states").evict(userId);
    }

    public UserEntity getCachedUser(Long userId) {
        return (UserEntity) cacheManager.getCache("cached_user").get(userId).get();
    }

    public void putCachedUser(Long userId, UserEntity user) {
        cacheManager.getCache("cached_user").put(userId, user);
    }

    public void evictCachedUser(Long userId) {
        cacheManager.getCache("cached_user").evict(userId);
    }

    @SneakyThrows
    public List<UserEntity> getCachedProfiles(Long userId) {
        String cachedProfilesJson = redisTemplate.opsForValue().get("cached_profiles_" + userId);
        if (cachedProfilesJson != null) {
            UserEntity[] userEntities = objectMapper.readValue(cachedProfilesJson, UserEntity[].class);
            return new ArrayList<>(Arrays.asList(userEntities));
        }
        return null;
    }

    @SneakyThrows
    public void putCachedProfiles(Long userId, List<UserEntity> profiles) {
        String profilesJson = objectMapper.writeValueAsString(profiles);
        redisTemplate.opsForValue().set("cached_profiles_" + userId, profilesJson);
        redisTemplate.expire("cached_profiles_" + userId, 12, TimeUnit.HOURS);
    }

    public void evictCachedProfiles(Long userId, UserEntity entityToRemove, List<UserEntity> cachedProfiles) {
        cachedProfiles.removeIf(userEntity -> Objects.equals(userEntity.getId(), entityToRemove.getId()));
        putCachedProfiles(userId, cachedProfiles);
    }
}