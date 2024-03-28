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
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {

    @Autowired
    CacheManager cacheManager;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

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
    public Long getUserAssessmentId(Long myId) {
        String excludedUserIdsJson = redisTemplate.opsForValue().get("userAssessmentId_" + myId);
        if (excludedUserIdsJson != null) {
            return objectMapper.readValue(excludedUserIdsJson, Long.class);
        }
        return null;
    }
    @SneakyThrows
    public void putUserAssessmentId(Long myId, Long userAssessmentId) {
        String excludedUserIdsJson = objectMapper.writeValueAsString(userAssessmentId);
        redisTemplate.opsForValue().set("userAssessmentId_" + myId, excludedUserIdsJson);
        redisTemplate.expire("userAssessmentId_" + myId, 3, TimeUnit.HOURS);
    }

    public void evictUserAssessmentId(Long myId) {
        redisTemplate.delete("userAssessmentId_" + myId);
    }


    @SneakyThrows
    public List<Long> getExcludedUserIds(Long myId) {
        String excludedUserIdsJson = redisTemplate.opsForValue().get("excludedUserIds_" + myId);
        if (excludedUserIdsJson != null) {
            Long[] excludedUserIds = objectMapper.readValue(excludedUserIdsJson, Long[].class);
            return new ArrayList<>(Arrays.asList(excludedUserIds));
        }
        return new ArrayList<>();
    }

    @SneakyThrows
    public void putExcludedUserIds(Long myId, List<Long> excludedUserIds) {
        String excludedUserIdsJson = objectMapper.writeValueAsString(excludedUserIds);
        boolean cacheExists = Boolean.TRUE.equals(redisTemplate.hasKey("excludedUserIds_" + myId));
        if (cacheExists) {
            redisTemplate.opsForValue().set("excludedUserIds_" + myId, excludedUserIdsJson);
        } else {
            redisTemplate.opsForValue().set("excludedUserIds_" + myId, excludedUserIdsJson);
            redisTemplate.expire("excludedUserIds_" + myId, 24, TimeUnit.HOURS);
        }
    }
}