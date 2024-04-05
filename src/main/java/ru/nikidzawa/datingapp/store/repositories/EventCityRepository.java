package ru.nikidzawa.datingapp.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.datingapp.store.entities.event.EventCity;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;

import java.util.List;

@Repository
public interface EventCityRepository extends JpaRepository<EventCity, Long> {
    @Query("SELECT u FROM EventCity u " +
            "ORDER BY " +
            "6371 * 2 * ASIN(SQRT(" +
            "   POWER(SIN((:givenLatitude - u.latitude) * pi() / 180 / 2), 2) + " +
            "   COS(:givenLatitude * pi() / 180) * COS(u.latitude * pi() / 180) * " +
            "   POWER(SIN((:givenLongitude - u.longitude) * pi() / 180 / 2), 2) " +
            "))")
    List<EventCity> findAllOrderByDistance(@Param("givenLongitude") double givenLongitude,
                                           @Param("givenLatitude") double givenLatitude);
}
