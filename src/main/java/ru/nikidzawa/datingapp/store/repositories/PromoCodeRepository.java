package ru.nikidzawa.datingapp.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.datingapp.store.entities.event.PromoCodeEntity;

import java.util.Optional;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCodeEntity, Long> {

    Optional<PromoCodeEntity> findByPromoCode (String promoCode);
}
