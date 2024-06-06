package ru.nikidzawa.datingapp.store.entities.payment;

import lombok.Getter;
import lombok.Setter;
import ru.nikidzawa.datingapp.store.entities.event.EventCity;
import ru.nikidzawa.datingapp.store.entities.event.EventEntity;
import ru.nikidzawa.datingapp.store.entities.event.EventImage;
import ru.nikidzawa.datingapp.store.entities.event.EventType;

import java.util.List;

@Getter
@Setter
public class PaymentOrder extends EventEntity {

    int count;

    String address;

    String name;

    String date;

    String time;

    String contactPhone;

    String rating;

    Long cost;

    boolean favorite;

    String smallDescription;

    String fullDescription;

    EventImage mainImage;

    EventType eventType;

    EventCity city;

    List<EventImage> eventImages;
}
