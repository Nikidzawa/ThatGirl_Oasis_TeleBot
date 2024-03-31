package ru.nikidzawa.datingapp.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.datingapp.store.entities.error.ErrorEntity;

@Repository
public interface ErrorRepository extends JpaRepository<ErrorEntity, Long> {

}
