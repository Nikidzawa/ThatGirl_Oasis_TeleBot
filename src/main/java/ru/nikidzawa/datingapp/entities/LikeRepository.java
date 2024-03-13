package ru.nikidzawa.datingapp.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LikeRepository extends JpaRepository<LikeEntity, Long> {
    @Query("SELECT l FROM LikeEntity l WHERE l.likerUserId = :likerUserId")
    List<LikeEntity> findByLikerUserId(long likerUserId);
}
