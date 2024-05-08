package ru.nikidzawa.datingapp.store.entities.event;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.nikidzawa.datingapp.store.entities.siteAccount.SiteAccount;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;

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

    String contactPhone;

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "event_members",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "account_id")
    )
    List<SiteAccount> members;
}