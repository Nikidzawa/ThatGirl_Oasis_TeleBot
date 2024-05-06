package ru.nikidzawa.datingapp.API;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nikidzawa.datingapp.API.exceptions.Unauthorized;
import ru.nikidzawa.datingapp.store.entities.event.EventCity;
import ru.nikidzawa.datingapp.store.entities.event.EventEntity;
import ru.nikidzawa.datingapp.store.entities.event.EventImage;
import ru.nikidzawa.datingapp.store.entities.event.EventType;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.store.repositories.*;
import ru.nikidzawa.datingapp.telegramBot.botFunctions.BotFunctions;
import ru.nikidzawa.datingapp.telegramBot.services.DataBaseService;
import ru.nikidzawa.datingapp.telegramBot.services.parsers.Geocode;
import ru.nikidzawa.datingapp.telegramBot.services.parsers.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("api/")
public class DataBaseApi {

    private final EventRepository eventRepository;

    private final EventImageRepository eventImageRepository;

    private final EventTypeRepository eventTypeRepository;

    private final ExternalApi externalApi;

    private final EventCityRepository eventCityRepository;

    private final JsonParser jsonParser;

    private final DataBaseService dataBaseService;

    private final PaymentRepository paymentRepository;

    @Setter
    BotFunctions botFunctions;

    @GetMapping("getUserById/{id}")
    public ResponseEntity<?> getUser (@PathVariable Long id) {
        Optional<UserEntity> userEntity = dataBaseService.getUserById(id);
        if (userEntity.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userEntity.get());
    }

    @GetMapping("getUserStatus/{id}")
    public String getUserStatus (@PathVariable Long id) {
        return botFunctions.getChatMember(id).getStatus();
    }

    @GetMapping("getEvent/{id}")
    public EventEntity getEventById (@PathVariable Long id) {
        return eventRepository.findById(id).get();
    }

    @PostMapping("postEventCity/{userId}")
    public EventCity postEventCity (@RequestBody EventCity eventCity, @PathVariable Long userId) {
        checkAdminStatus(userId);
        Geocode geocode = jsonParser.parseGeocode(externalApi.getCoordinates(eventCity.getName()));
        eventCity.setLatitude(geocode.getLat());
        eventCity.setLongitude(geocode.getLon());
        return eventCityRepository.saveAndFlush(eventCity);
    }

    @GetMapping("getEventCities")
    public List<EventCity> getEventCities() {
        return eventCityRepository.findAll();
    }

    @DeleteMapping("deleteEventCity/{eventCityId}/{userId}")
    public ResponseEntity<?> deleteEventCity (@PathVariable Long eventCityId, @PathVariable Long userId) {
        checkAdminStatus(userId);
        eventCityRepository.deleteById(eventCityId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("getEventsByCityId/{cityId}")
    public List<EventEntity> getEventsByEventCityId (@PathVariable Long cityId) {
        return eventCityRepository.findEventEntitiesByCityId(cityId);
    }

    @PatchMapping("setOrRemoveEventFavorite/{eventId}/{userId}")
    public EventEntity setOrRemoveEventFavorite (@PathVariable Long eventId, @PathVariable Long userId) {
        checkAdminStatus(userId);
        EventEntity eventEntity = eventRepository.findById(eventId).get();
        eventEntity.setFavorite(!eventEntity.isFavorite());
        return eventRepository.saveAndFlush(eventEntity);
    }

    @DeleteMapping("deleteEventById/{eventId}/{userId}")
    public ResponseEntity<?> deleteEventById (@PathVariable Long eventId, @PathVariable Long userId) {
        checkAdminStatus(userId);
        EventEntity eventEntity = eventRepository.findById(eventId).get();
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

    @PostMapping("postEventType/{userId}")
    public EventType postEventType (@RequestBody EventType eventType, @PathVariable Long userId) {
        checkAdminStatus(userId);
        return eventTypeRepository.saveAndFlush(eventType);
    }

    @GetMapping("getEventsByType/{eventId}")
    public List<EventEntity> getEventTypes(@PathVariable Long eventId) {
        return eventTypeRepository.findEventEntitiesByEventTypeId(eventId);
    }

    @GetMapping("getAllEventTypes")
    public List<EventType> getAllEventTypes () {
        return eventTypeRepository.findAll();
    }

    @DeleteMapping("deleteEventType/{id}/{userId}")
    public ResponseEntity<?> deleteEventTypeById (@PathVariable Long id, @PathVariable Long userId) {
        checkAdminStatus(userId);
        eventTypeRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @SneakyThrows
    @PostMapping("postEvent/{userId}")
    public EventEntity postEvent(@RequestBody EventEntity eventEntity, @PathVariable Long userId) {
        checkAdminStatus(userId);
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

    @PatchMapping("setImages/{userId}")
    public ResponseEntity<?> setImages (@RequestBody EventEntity eventEntity, @PathVariable Long userId) {
        checkAdminStatus(userId);
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

    private void checkAdminStatus (Long userid) {
        String status;
        try {
            status = botFunctions.getChatMember(userid).getStatus();
            if (status.equals("administrator") || status.equals("creator")) {
                return;
            } else {
                throw new Unauthorized("Недостаточно прав для запроса");
            }
        } catch (Exception e) {
            throw new Unauthorized("Недостаточно прав для запроса");
        }
    }
}