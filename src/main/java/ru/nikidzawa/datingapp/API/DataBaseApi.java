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
import ru.nikidzawa.datingapp.store.repositories.EventCityRepository;
import ru.nikidzawa.datingapp.store.repositories.EventImageRepository;
import ru.nikidzawa.datingapp.store.repositories.EventRepository;
import ru.nikidzawa.datingapp.store.repositories.EventTypeRepository;
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
public class DataBaseApi {

    private final EventRepository eventRepository;

    private final EventImageRepository eventImageRepository;

    private final EventTypeRepository eventTypeRepository;

    private final ExternalApi externalApi;

    private final EventCityRepository eventCityRepository;

    private final JsonParser jsonParser;

    private final DataBaseService dataBaseService;

    @Setter
    BotFunctions botFunctions;

    @GetMapping("api/getUserById/{id}")
    public ResponseEntity<?> getUser (@PathVariable Long id) {
        Optional<UserEntity> userEntity = dataBaseService.getUserById(id);
        if (userEntity.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userEntity.get());
    }

    @GetMapping("api/getUserStatus/{id}")
    public String getUserStatus (@PathVariable Long id) {
        return botFunctions.getChatMember(id).getStatus();
    }

    @GetMapping("api/getEvent/{id}")
    public EventEntity getEventById (@PathVariable Long id) {
        return eventRepository.findById(id).get();
    }

    @PostMapping("api/postEventCity/{userId}")
    public EventCity postEventCity (@RequestBody EventCity eventCity, @PathVariable Long userId) {
        checkAdminStatus(userId);
        Geocode geocode = jsonParser.parseGeocode(externalApi.getCoordinates(eventCity.getName()));
        eventCity.setLatitude(geocode.getLat());
        eventCity.setLongitude(geocode.getLon());
        return eventCityRepository.saveAndFlush(eventCity);
    }

    @GetMapping("api/getEventCities")
    public List<EventCity> getEventCities() {
        return eventCityRepository.findAll();
    }

    @DeleteMapping("api/deleteEventCity/{eventCityId}/{userId}")
    public ResponseEntity<?> deleteEventCity (@PathVariable Long eventCityId, @PathVariable Long userId) {
        checkAdminStatus(userId);
        eventCityRepository.deleteById(eventCityId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("api/getEventsByCityId/{cityId}")
    public List<EventEntity> getEventsByEventCityId (@PathVariable Long cityId) {
        return eventCityRepository.findEventEntitiesByCityId(cityId);
    }

    @PatchMapping("api/setOrRemoveEventFavorite/{eventId}/{userId}")
    public EventEntity setOrRemoveEventFavorite (@PathVariable Long eventId, @PathVariable Long userId) {
        checkAdminStatus(userId);
        EventEntity eventEntity = eventRepository.findById(eventId).get();
        eventEntity.setFavorite(!eventEntity.isFavorite());
        return eventRepository.saveAndFlush(eventEntity);
    }

    @DeleteMapping("api/deleteEventById/{eventId}/{userId}")
    public ResponseEntity<?> deleteEventById (@PathVariable Long eventId, @PathVariable Long userId) {
        checkAdminStatus(userId);
        EventEntity eventEntity = eventRepository.findById(eventId).get();
        EventCity eventCity = eventEntity.getCity();
        List<EventEntity> eventEntities = new ArrayList<>(eventCityRepository.findEventEntitiesByCityId(eventCity.getId()));
        eventEntities.remove(eventEntity);
        eventCity.setEventEntities(eventEntities);
        eventCityRepository.saveAndFlush(eventCity);
        eventRepository.delete(eventEntity);
        return ResponseEntity.ok().build();
    }

    @PostMapping("api/postEventType/{userId}")
    public EventType postEventType (@RequestBody EventType eventType, @PathVariable Long userId) {
        checkAdminStatus(userId);
        return eventTypeRepository.saveAndFlush(eventType);
    }

    @GetMapping("api/getEventsByType/{eventId}")
    public List<EventEntity> getEventTypes(@PathVariable Long eventId) {
        return eventTypeRepository.findEventEntitiesByEventTypeId(eventId);
    }

    @GetMapping("api/getAllEventTypes")
    public List<EventType> getAllEventTypes () {
        return eventTypeRepository.findAll();
    }

    @DeleteMapping("api/deleteEventType/{id}/{userId}")
    public ResponseEntity<?> deleteEventTypeById (@PathVariable Long id, @PathVariable Long userId) {
        checkAdminStatus(userId);
        eventTypeRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @SneakyThrows
    @PostMapping("api/postEvent/{userId}")
    public ResponseEntity<?> postEvent(@RequestBody EventEntity eventEntity, @PathVariable Long userId) {
        checkAdminStatus(userId);
        List<EventImage> eventImages = eventEntity.getEventImages();
        eventEntity.setEventImages(null);
        EventImage mainImage = eventImageRepository.saveAndFlush(eventEntity.getMainImage());
        eventEntity.setMainImage(null);
        eventEntity = eventRepository.saveAndFlush(eventEntity);
        Long id = eventEntity.getId();
        String basePath = "/" + id + "/";
        String mainImagePath = basePath + "main";

        ResponseEntity<?> createFolderResponse = externalApi.createFolder(id);
        if (!createFolderResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(createFolderResponse.getStatusCode()).body(createFolderResponse.getBody());
        }

        try {
            for (int i = 0; i < eventImages.size(); i++) {
                String imagePath = basePath + "image(" + i + ")";
                EventImage currentEventImage = eventImages.get(i);
                externalApi.uploadImage(imagePath, currentEventImage.getHref());
                currentEventImage.setHref(imagePath);
            }
            externalApi.uploadImage(mainImagePath, mainImage.getHref());
            mainImage.setHref(mainImagePath);
        } catch (Exception ex) {
            externalApi.deleteFolder(id);
            return ResponseEntity.notFound().build();
        }

        eventImages = eventImageRepository.saveAllAndFlush(eventImages);
        mainImage = eventImageRepository.saveAndFlush(mainImage);

        eventEntity.setEventImages(eventImages);
        eventEntity.setMainImage(mainImage);

        eventRepository.saveAndFlush(eventEntity);

        EventType eventType = eventEntity.getEventType();
        List<EventEntity> eventsByType = new ArrayList<>(eventTypeRepository.findEventEntitiesByEventTypeId(eventType.getId()));
        eventsByType.add(eventEntity);
        eventType.setEventEntities(eventsByType);
        eventTypeRepository.saveAndFlush(eventType);

        EventCity eventCity = eventEntity.getCity();

        List<EventEntity> eventEntities = new ArrayList<>(eventCityRepository.findEventEntitiesByCityId(eventCity.getId()));
        eventEntities.add(eventEntity);
        eventCity.setEventEntities(eventEntities);

        eventCityRepository.saveAndFlush(eventCity);
        return ResponseEntity.ok().build();
    }

    private void checkAdminStatus (Long userid) {
        String status = botFunctions.getChatMember(userid).getStatus();
        if (status.equals("administrator") || status.equals("creator")) {
            return;
        } else {
            throw new Unauthorized("Недостаточно прав для запроса");
        }
    }
}