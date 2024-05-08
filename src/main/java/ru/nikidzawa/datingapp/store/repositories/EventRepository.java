package ru.nikidzawa.datingapp.store.repositories;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.datingapp.store.entities.event.EventEntity;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {
    @Query("SELECT e FROM EventEntity e JOIN e.tokens m WHERE e.id = :eventId AND m.token = :token")
    Optional<EventEntity> checkRegister(@Param("eventId") Long eventId, @Param("mail") String token);
}