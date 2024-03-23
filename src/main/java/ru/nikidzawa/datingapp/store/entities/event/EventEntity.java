package ru.nikidzawa.datingapp.store.entities.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.store.entities.user.UserSiteAccountEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String city;

    String address;

    String name;

    String date;

    String time;

    String rating;

    Long cost;

    Double lon;

    Double lat;

    @Column(length = 500)
    String smallDescription;

    @Column(length = 5000)
    String fullDescription;

    @Column(length = 10000)
    String image;

    @JsonIgnore
    @ManyToMany(mappedBy = "events", fetch = FetchType.LAZY)
    List<UserSiteAccountEntity> users = new ArrayList<>();
}