package ru.nikidzawa.datingapp.TelegramBot.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nikidzawa.datingapp.entities.LikeEntity;
import ru.nikidzawa.datingapp.entities.LikeRepository;
import ru.nikidzawa.datingapp.entities.UserEntity;
import ru.nikidzawa.datingapp.entities.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class DataBaseService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    LikeRepository likeRepository;

    @Cacheable(cacheNames = "user", key = "#id")
    public Optional<UserEntity> getUserById (Long id) {
        return userRepository.findFirstById(id);
    }

    @CachePut(cacheNames = "user", key = "#user.id")
    public UserEntity saveUser (UserEntity user) {
        return userRepository.saveAndFlush(user);
    }

    public Optional<UserEntity> getLikeById (Long id) {
        return userRepository.findFirstById(id);
    }

    public LikeEntity saveLike (LikeEntity likeEntity) {
        return likeRepository.saveAndFlush(likeEntity);
    }

    public void deleteLike(Long likeId) {
        likeRepository.deleteById(likeId);
    }

    public List<LikeEntity> findByLikerUserId (long likerUserId) {
        return likeRepository.findByLikerUserId(likerUserId);
    }

    public List<UserEntity> getProfiles (UserEntity myProfile) {
        return userRepository.findAllOrderByDistance(myProfile.getId(), myProfile.getLongitude(), myProfile.getLatitude());
    }
}
