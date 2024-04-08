package ru.nikidzawa.datingapp.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.datingapp.store.entities.event.EventCity;
import ru.nikidzawa.datingapp.store.entities.event.EventEntity;

import java.util.List;

@Repository
public interface EventCityRepository extends JpaRepository<EventCity, Long> {
    @Query("SELECT ec.eventEntities FROM EventCity ec WHERE ec.id = :cityId")
    List<EventEntity> findEventEntitiesByCityId(@Param("cityId") Long cityId);
}
