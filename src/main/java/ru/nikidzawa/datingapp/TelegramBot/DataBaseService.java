package ru.nikidzawa.datingapp.TelegramBot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.nikidzawa.datingapp.entities.UserEntity;
import ru.nikidzawa.datingapp.entities.UserRepository;

import java.util.Optional;

@Service
@CacheConfig(cacheNames = "user")
public class DataBaseService {

    @Autowired
    UserRepository userRepository;

    @Cacheable(key = "#id")
    public Optional<UserEntity> getUserById (Long id) {
        return userRepository.findFirstById(id);
    }

    @CachePut(key = "#user.id")
    public UserEntity saveUser (UserEntity user) {
        userRepository.saveAndFlush(user);
        return user;
    }
}
