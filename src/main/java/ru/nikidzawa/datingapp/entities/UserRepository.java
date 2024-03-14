package ru.nikidzawa.datingapp.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findFirstById (Long id);

    @Query("SELECT u FROM UserEntity u " +
            "WHERE u.isActive = true " +
            "AND u.isBanned = false " +
            "AND u.id != :userId " +
            "ORDER BY SQRT(POWER(u.longitude - :givenLongitude, 2) + POWER(u.latitude - :givenLatitude, 2))")
    List<UserEntity> findAllOrderByDistance(@Param("userId") Long userId,
                                            @Param("givenLongitude") double givenLongitude,
                                            @Param("givenLatitude") double givenLatitude);
}
