package ru.nikidzawa.datingapp.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.datingapp.store.entities.user.UserSiteAccount;

@Repository
public interface UserSiteAccountRepository extends JpaRepository<UserSiteAccount, Long> {
}
