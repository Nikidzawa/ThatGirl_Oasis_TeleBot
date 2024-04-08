package ru.nikidzawa.datingapp.store.repositories;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.datingapp.store.entities.event.EventEntity;
import ru.nikidzawa.datingapp.store.entities.event.EventType;

import java.util.List;

@Repository
public interface EventTypeRepository extends JpaRepository<EventType, Long> {
    @Query("SELECT et.eventEntities FROM EventType et WHERE et.id = :typeId")
    List<EventEntity> findEventEntitiesByEventTypeId(@Param("typeId") Long typeId);
}
