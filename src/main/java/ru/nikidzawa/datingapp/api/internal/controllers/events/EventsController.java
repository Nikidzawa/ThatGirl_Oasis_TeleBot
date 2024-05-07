package ru.nikidzawa.datingapp.api.internal.controllers.events;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nikidzawa.datingapp.api.internal.controllers.users.RolesController;
import ru.nikidzawa.datingapp.api.internal.exceptions.NotFoundException;
import ru.nikidzawa.datingapp.store.entities.event.EventCity;
import ru.nikidzawa.datingapp.store.entities.event.EventEntity;
import ru.nikidzawa.datingapp.store.entities.event.EventImage;
import ru.nikidzawa.datingapp.store.entities.event.EventType;
import ru.nikidzawa.datingapp.store.repositories.EventCityRepository;
import ru.nikidzawa.datingapp.store.repositories.EventImageRepository;
import ru.nikidzawa.datingapp.store.repositories.EventRepository;
import ru.nikidzawa.datingapp.store.repositories.EventTypeRepository;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("api/events/")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventsController {

    EventRepository eventRepository;

    EventCityRepository eventCityRepository;

    EventTypeRepository eventTypeRepository;

    RolesController rolesController;

    EventImageRepository eventImageRepository;

    @GetMapping("{id}")
    public EventEntity getEventById (@PathVariable Long id) {
        return eventRepository.findById(id).orElseThrow(() -> new NotFoundException("Мероприятие не найдено"));
    }

    @DeleteMapping("{eventId}/{userId}")
    public ResponseEntity<?> deleteEventById (@PathVariable Long eventId, @PathVariable Long userId) {
        rolesController.checkAdminStatus(userId);
        EventEntity eventEntity = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Мероприятие не найдено"));
        EventCity eventCity = eventEntity.getCity();
        List<EventEntity> eventEntities = new ArrayList<>(eventCityRepository.findEventEntitiesByCityId(eventCity.getId()));
        eventEntities.remove(eventEntity);
        eventCity.setEventEntities(eventEntities);
        eventCityRepository.saveAndFlush(eventCity);

        EventType eventType = eventEntity.getEventType();
        List<EventEntity> eventEntities2 = new ArrayList<>(eventTypeRepository.findEventEntitiesByEventTypeId(eventType.getId()));
        eventEntities2.remove(eventEntity);
        eventType.setEventEntities(eventEntities2);
        eventTypeRepository.saveAndFlush(eventType);

        eventRepository.delete(eventEntity);
        return ResponseEntity.ok().build();
    }

    @SneakyThrows
    @PostMapping("{userId}")
    public EventEntity postEvent(@RequestBody EventEntity eventEntity, @PathVariable Long userId) {
        rolesController.checkAdminStatus(userId);
        EventEntity savedEventEntity = eventRepository.saveAndFlush(eventEntity);

        EventType eventType = savedEventEntity.getEventType();
        List<EventEntity> eventEntities1 = eventTypeRepository.findEventEntitiesByEventTypeId(eventType.getId());
        eventEntities1.add(savedEventEntity);
        eventType.setEventEntities(eventEntities1);
        eventTypeRepository.saveAndFlush(eventType);

        EventCity eventCity = savedEventEntity.getCity();
        List<EventEntity> eventEntities2 = eventCityRepository.findEventEntitiesByCityId(eventCity.getId());
        eventEntities2.add(savedEventEntity);
        eventCity.setEventEntities(eventEntities2);
        eventCityRepository.saveAndFlush(eventCity);

        return savedEventEntity;
    }

    @PatchMapping("changeFavouriteStatus/{eventId}/{userId}")
    public EventEntity setOrRemoveEventFavorite (@PathVariable Long eventId, @PathVariable Long userId) {
        rolesController.checkAdminStatus(userId);
        EventEntity eventEntity = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Мероприятие не найдено"));
        eventEntity.setFavorite(!eventEntity.isFavorite());
        return eventRepository.saveAndFlush(eventEntity);
    }

    @PatchMapping("setImages/{userId}")
    public ResponseEntity<?> setImages (@RequestBody EventEntity eventEntity, @PathVariable Long userId) {
        rolesController.checkAdminStatus(userId);
        EventImage mainImage = eventEntity.getMainImage();
        eventImageRepository.saveAndFlush(mainImage);
        List<EventImage> otherImages = eventEntity.getEventImages();

        if (otherImages != null) {
            for (EventImage eventImage : otherImages) {
                eventImageRepository.saveAndFlush(eventImage);
            }
        }
        eventRepository.saveAndFlush(eventEntity);
        return ResponseEntity.ok().build();
    }
}
