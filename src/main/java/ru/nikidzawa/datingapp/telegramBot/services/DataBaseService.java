package ru.nikidzawa.datingapp.telegramBot.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.nikidzawa.datingapp.store.entities.complain.ComplainEntity;
import ru.nikidzawa.datingapp.store.entities.error.ErrorEntity;
import ru.nikidzawa.datingapp.store.entities.like.LikeEntity;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.store.repositories.ComplaintRepository;
import ru.nikidzawa.datingapp.store.repositories.ErrorRepository;
import ru.nikidzawa.datingapp.store.repositories.LikeRepository;
import ru.nikidzawa.datingapp.store.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class DataBaseService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ErrorRepository errorRepository;

    @Cacheable(cacheNames = "user", key = "#id")
    public Optional<UserEntity> getUserById (Long id) {
        return userRepository.findById(id);
    }

    @CachePut(cacheNames = "user", key = "#user.id")
    public UserEntity saveUser (UserEntity user) {
        return userRepository.saveAndFlush(user);
    }

    public Long getCountActiveAndNotBannedUsers () {
        return userRepository.countActiveAndNotBannedUsers();
    }

    public String[] findTop10CitiesByUserCount() {
        return userRepository.findTop10CitiesByUserCount();
    }

    public LikeEntity saveLike (LikeEntity likeEntity) {
        return likeRepository.saveAndFlush(likeEntity);
    }

    public void saveComplain (ComplainEntity complainEntity) {
        complaintRepository.saveAndFlush(complainEntity);
    }

    public List<ComplainEntity> findByComplaintUser (UserEntity complaintUser) {
        return complaintRepository.findByComplaintUser(complaintUser);
    }

    public void deleteAllComplainEntities(List<ComplainEntity> complainEntities) {
        complaintRepository.deleteAll(complainEntities);
    }

    public List<ComplainEntity> findAllComplaints () {
        return complaintRepository.findAll();
    }

    public void deleteLike(Long likeId) {
        likeRepository.deleteById(likeId);
    }

    public List<UserEntity> getProfiles (UserEntity myProfile) {
        return userRepository.findAllOrderByDistance(myProfile.getId(), myProfile.getLongitude(), myProfile.getLatitude());
    }

    public void saveError (ErrorEntity errorEntity) {
        errorRepository.saveAndFlush(errorEntity);
    }

    public List<ErrorEntity> findAllErrors () {
        return errorRepository.findAll();
    }

    public void deleteError (ErrorEntity errorEntity) {
        errorRepository.delete(errorEntity);
    }
}
