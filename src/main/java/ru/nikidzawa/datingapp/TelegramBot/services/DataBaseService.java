package ru.nikidzawa.datingapp.TelegramBot.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.nikidzawa.datingapp.store.entities.complain.ComplainEntity;
import ru.nikidzawa.datingapp.store.entities.like.LikeEntity;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.store.repositories.ComplaintRepository;
import ru.nikidzawa.datingapp.store.repositories.LikeRepository;
import ru.nikidzawa.datingapp.store.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class DataBaseService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    LikeRepository likeRepository;

    @Autowired
    ComplaintRepository complaintRepository;

    @Cacheable(cacheNames = "user", key = "#id")
    public Optional<UserEntity> getUserById (Long id) {
        return userRepository.findFirstById(id);
    }

    @CachePut(cacheNames = "user", key = "#user.id")
    public UserEntity saveUser (UserEntity user) {
        return userRepository.saveAndFlush(user);
    }

    public LikeEntity saveLike (LikeEntity likeEntity) {
        return likeRepository.saveAndFlush(likeEntity);
    }

    public ComplainEntity saveComplain (ComplainEntity complainEntity) {
        return complaintRepository.saveAndFlush(complainEntity);
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
