package ru.nikidzawa.datingapp.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.nikidzawa.datingapp.store.entities.complain.ComplainEntity;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<ComplainEntity, Long> {
    @Query("SELECT c FROM ComplainEntity c WHERE c.complaintUser = :user")
    List<ComplainEntity> findByComplaintUser(UserEntity user);
}
