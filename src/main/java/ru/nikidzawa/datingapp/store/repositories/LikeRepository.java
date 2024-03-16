package ru.nikidzawa.datingapp.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.datingapp.store.entities.like.LikeEntity;

@Repository
public interface LikeRepository extends JpaRepository<LikeEntity, Long> {

}
