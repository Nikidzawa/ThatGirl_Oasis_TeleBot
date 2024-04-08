package ru.nikidzawa.datingapp.store.entities.event;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
                                   
    String address;

    String name;

    String date;

    String time;

    String rating;

    Long cost;

    boolean favorite;

    @Column(length = 500)
    String smallDescription;

    @Column(length = 5000)
    String fullDescription;

    @OneToOne(fetch = FetchType.EAGER)
    EventImage mainImage;

    @ManyToOne(fetch = FetchType.EAGER)
    EventType eventType;

    @ManyToOne(fetch = FetchType.EAGER)
    EventCity city;

    @OneToMany(fetch = FetchType.LAZY)
    List<EventImage> eventImages;
}