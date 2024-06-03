package ru.nikidzawa.datingapp.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.datingapp.store.entities.payment.PaymentEntity;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    @Query("select pe from PaymentEntity pe join pe.events e where e.id = :eventId")
    List<PaymentEntity> getAllPaymentsByEventId(Long eventId);
}
