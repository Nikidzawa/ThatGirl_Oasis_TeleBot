package ru.nikidzawa.datingapp.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nikidzawa.datingapp.store.entities.event.EventImage;

import java.util.List;

public interface EventImageRepository extends JpaRepository<EventImage, Long> {

}
