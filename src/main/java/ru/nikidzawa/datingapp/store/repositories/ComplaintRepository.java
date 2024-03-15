package ru.nikidzawa.datingapp.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.nikidzawa.datingapp.store.entities.complain.ComplainEntity;

import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<ComplainEntity, Long> {
    @Query("SELECT c FROM ComplainEntity c ORDER BY c.id ASC")
    Optional<ComplainEntity> findAnyComplain();
}
