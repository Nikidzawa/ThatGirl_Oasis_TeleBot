package ru.nikidzawa.datingapp.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.datingapp.store.entities.event.EventCart;

@Repository
public interface EventCartRepository extends JpaRepository<EventCart, Long> {
}
