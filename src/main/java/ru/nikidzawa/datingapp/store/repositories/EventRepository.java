package ru.nikidzawa.datingapp.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.datingapp.store.entities.event.EventEntity;
import ru.nikidzawa.datingapp.store.entities.event.EventImage;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {

    @Query("SELECT e.eventImages FROM EventEntity e WHERE e.id = :eventId")
    List<EventImage> findEventImagesByEventId (@Param("eventId") Long eventId);
}