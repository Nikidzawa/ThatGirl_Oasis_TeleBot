package ru.nikidzawa.datingapp.store.entities.event;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.nikidzawa.datingapp.store.entities.user.UserAvatar;

import java.io.Serializable;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventEntity implements Serializable {
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

    @OneToOne(fetch = FetchType.EAGER)
    EventImage mainImage;

    @OneToMany(fetch = FetchType.LAZY)
    List<EventImage> eventImages;
}