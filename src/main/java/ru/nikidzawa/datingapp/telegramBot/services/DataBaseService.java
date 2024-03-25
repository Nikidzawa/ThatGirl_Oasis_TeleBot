package ru.nikidzawa.datingapp.telegramBot.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.nikidzawa.datingapp.store.entities.complain.ComplainEntity;
import ru.nikidzawa.datingapp.store.entities.error.ErrorEntity;
import ru.nikidzawa.datingapp.store.entities.like.LikeEntity;
import ru.nikidzawa.datingapp.store.entities.user.UserAvatar;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.store.entities.user.UserSiteAccount;
import ru.nikidzawa.datingapp.store.repositories.*;

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

    @Autowired
    private UserSiteAccountRepository userSiteAccountRepository;

    @Autowired
    private UserAvatarRepository userAvatarRepository;

    @Cacheable(cacheNames = "user", key = "#id")
    public Optional<UserEntity> getUserById (Long id) {
        return userRepository.findById(id);
    }

    @CachePut(cacheNames = "user", key = "#user.id")
    public UserEntity saveUser (UserEntity user) {
        return userRepository.saveAndFlush(user);
    }

    @CachePut(cacheNames = "userSite", key = "#userSiteAccount.id")
    public UserSiteAccount saveUserSiteAccount (UserSiteAccount userSiteAccount) {
        return userSiteAccountRepository.saveAndFlush(userSiteAccount);
    }

    public List<UserAvatar> saveAllUserAvatars (List<UserAvatar> userAvatars) {
        return userAvatarRepository.saveAllAndFlush(userAvatars);
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
