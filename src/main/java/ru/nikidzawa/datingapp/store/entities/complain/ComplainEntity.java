package ru.nikidzawa.datingapp.store.entities.complain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComplainEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long complainSenderId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "complaintUser_id")
    UserEntity complaintUser;

    String description;
}
