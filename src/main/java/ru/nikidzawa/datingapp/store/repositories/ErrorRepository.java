package ru.nikidzawa.datingapp.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nikidzawa.datingapp.store.entities.error.ErrorEntity;

public interface ErrorRepository extends JpaRepository<ErrorEntity, Long> {

}
