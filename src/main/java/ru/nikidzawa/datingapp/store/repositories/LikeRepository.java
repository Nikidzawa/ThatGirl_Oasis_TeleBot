package ru.nikidzawa.datingapp.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.datingapp.store.entities.like.LikeEntity;

import java.util.List;

@Repository
public interface LikeRepository extends JpaRepository<LikeEntity, Long> {
    @Query("SELECT l FROM LikeEntity l WHERE l.likerUserId = :likerUserId")
    List<LikeEntity> findByLikerUserId(long likerUserId);
}
