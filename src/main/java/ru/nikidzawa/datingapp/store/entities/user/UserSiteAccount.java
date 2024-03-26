package ru.nikidzawa.datingapp.store.entities.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.nikidzawa.datingapp.store.entities.event.EventEntity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserSiteAccount implements Serializable {
    @Id
    Long id;

    @JsonIgnore
    @OneToOne
    UserEntity userEntity;

    double longitude;

    double latitude;

    @JsonIgnore
    @OneToMany(fetch = FetchType.EAGER)
    List<EventEntity> events = new ArrayList<>();
}