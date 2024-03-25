package ru.nikidzawa.datingapp.store.entities.event;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

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

    @Column(length = 10000)
    String image;
}