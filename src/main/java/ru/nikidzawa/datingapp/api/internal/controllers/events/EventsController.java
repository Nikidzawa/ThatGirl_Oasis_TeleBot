package ru.nikidzawa.datingapp.api.internal.controllers.events;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nikidzawa.datingapp.api.internal.controllers.users.RolesController;
import ru.nikidzawa.datingapp.api.internal.exceptions.NotFoundException;
import ru.nikidzawa.datingapp.api.internal.exceptions.OtherException;
import ru.nikidzawa.datingapp.store.entities.event.*;
import ru.nikidzawa.datingapp.store.repositories.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    PaymentRepository paymentRepository;

    @GetMapping("{eventId}")
    public EventEntity getEventById (@PathVariable Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Мероприятие не найдено"));
    }

    @DeleteMapping("{eventId}/{userId}")
    public ResponseEntity<?> deleteEventById (@PathVariable Long eventId, @PathVariable Long userId) {
        rolesController.checkAdminStatus(userId);

        EventEntity eventEntity = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Мероприятие не найдено"));

        try {
            EventCity eventCity = eventEntity.getCity();
            List<EventEntity> eventEntities = new ArrayList<>(eventCityRepository.findEventEntitiesByCityId(eventCity.getId()));
            eventEntities.remove(eventEntity);
            eventCity.setEventEntities(eventEntities);
            eventCityRepository.saveAndFlush(eventCity);
        } catch (Exception ex) {
            throw new OtherException("Ошибка при удалении мероприятия из списка мероприятий его типа " + ex.getMessage());
        }

        try {
            EventType eventType = eventEntity.getEventType();
            List<EventEntity> eventEntities2 = new ArrayList<>(eventTypeRepository.findEventEntitiesByEventTypeId(eventType.getId()));
            eventEntities2.remove(eventEntity);
            eventType.setEventEntities(eventEntities2);
            eventTypeRepository.saveAndFlush(eventType);
        } catch (Exception ex) {
            throw new OtherException("Ошибка при удалении мероприятия из списка мероприятий его города " + ex.getMessage());
        }

        paymentRepository.deleteAll(paymentRepository.getAllPaymentsByEventId(eventId));

        eventRepository.delete(eventEntity);
        return ResponseEntity.ok().build();
    }

    @SneakyThrows
    @PostMapping("{userId}")
    public EventEntity postEvent(@RequestBody EventEntity eventEntity, @PathVariable Long userId) {
        rolesController.checkAdminStatus(userId);

        EventEntity savedEventEntity = eventRepository.saveAndFlush(eventEntity);

        try {
            EventType eventType = savedEventEntity.getEventType();
            List<EventEntity> eventEntities1 = eventTypeRepository.findEventEntitiesByEventTypeId(eventType.getId());
            eventEntities1.add(savedEventEntity);
            eventType.setEventEntities(eventEntities1);
            eventTypeRepository.saveAndFlush(eventType);
        } catch (Exception ex) {
            throw new OtherException("Ошибка при добавлении мероприятия в списоок мероприятий его типа " + ex.getMessage());
        }

        try {
            EventCity eventCity = savedEventEntity.getCity();
            List<EventEntity> eventEntities2 = eventCityRepository.findEventEntitiesByCityId(eventCity.getId());
            eventEntities2.add(savedEventEntity);
            eventCity.setEventEntities(eventEntities2);
            eventCityRepository.saveAndFlush(eventCity);
        } catch (Exception ex) {
            throw new OtherException("Ошибка при добавлении мероприятия в списоок мероприятий его нового города " + ex.getMessage());
        }

        return savedEventEntity;
    }

    @PatchMapping("{userId}/{previousEventTypeId}/{previousCityId}")
    public EventEntity updateEvent (@RequestBody EventEntity eventEntity,
                                    @PathVariable Long userId,
                                    @PathVariable Long previousCityId,
                                    @PathVariable Long previousEventTypeId) {
        rolesController.checkAdminStatus(userId);

        Long actualEventTypeId = eventEntity.getEventType().getId();
        Long actualEventCityId = eventEntity.getCity().getId();

        if (!Objects.equals(actualEventTypeId, previousEventTypeId)) {
            EventType previousEventType = eventTypeRepository.findById(previousEventTypeId)
                    .orElseThrow(() -> new NotFoundException("Предыдущий тип мероприятия не найден"));

            try {
                List<EventEntity> eventEntities = eventTypeRepository.findEventEntitiesByEventTypeId(previousEventTypeId);
                eventEntities.remove(eventEntity);
                previousEventType.setEventEntities(eventEntities);
                eventTypeRepository.saveAndFlush(previousEventType);
            } catch (Exception ex) {
                throw new OtherException("Ошибка при удалении мероприятия из списка мероприятий его предыдущего типа " + ex.getMessage());
            }

            try {
                EventType actualEventType = eventEntity.getEventType();
                List<EventEntity> actualEventTypeEntities = eventTypeRepository.findEventEntitiesByEventTypeId(actualEventTypeId);
                actualEventTypeEntities.add(eventEntity);
                actualEventType.setEventEntities(actualEventTypeEntities);
                eventTypeRepository.saveAndFlush(actualEventType);
            } catch (Exception ex) {
                throw new OtherException("Ошибка при добавлении мероприятия в списоок мероприятий его нового типа " + ex.getMessage());
            }
        }

        if (!Objects.equals(actualEventCityId, previousCityId)) {
            EventCity previousEventCity = eventCityRepository.findById(previousCityId)
                    .orElseThrow(() -> new NotFoundException("Предыдущий город мероприятия не найден"));

            try {
                List<EventEntity> eventEntities = eventCityRepository.findEventEntitiesByCityId(previousCityId);
                eventEntities.remove(eventEntity);
                previousEventCity.setEventEntities(eventEntities);
                eventCityRepository.saveAndFlush(previousEventCity);
            } catch (Exception ex) {
                throw new OtherException("Ошибка при удалении мероприятия из списка мероприятий его предыдущего города " + ex.getMessage());
            }

            try {
                EventCity actualEventCity = eventEntity.getCity();
                List<EventEntity> actualEventCityEvents = eventCityRepository.findEventEntitiesByCityId(actualEventCityId);
                actualEventCityEvents.add(eventEntity);
                actualEventCity.setEventEntities(actualEventCityEvents);
                eventCityRepository.saveAndFlush(actualEventCity);
            } catch (Exception ex) {
                throw new OtherException("Ошибка при добавлении мероприятия в списоок мероприятий его нового города " + ex.getMessage());
            }
        }

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

    @GetMapping("checkMemberStatus/{eventId}/{token}")
    public EventEntity checkMemberStatus (@PathVariable Long eventId,
                                          @PathVariable String token) {
        return eventRepository.checkRegister(eventId, token)
                .orElseThrow(() -> new NotFoundException("Пользователь не зарегистрирвоан на мероприятие"));
    }

    @Transactional
    public void addTokenInEvent (Token token, EventEntity eventEntity) {
        List<Token> eventTokens = eventRepository.getTokensByEventId(eventEntity.getId());
        eventTokens.add(token);
        eventEntity.setTokens(eventTokens);
        eventRepository.saveAndFlush(eventEntity);
    }
}
