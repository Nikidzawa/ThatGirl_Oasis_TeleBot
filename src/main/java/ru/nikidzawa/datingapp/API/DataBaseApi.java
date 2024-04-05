package ru.nikidzawa.datingapp.API;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nikidzawa.datingapp.store.entities.event.*;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.store.repositories.*;
import ru.nikidzawa.datingapp.telegramBot.botFunctions.BotFunctions;
import ru.nikidzawa.datingapp.telegramBot.services.DataBaseService;
import ru.nikidzawa.datingapp.telegramBot.services.parsers.Geocode;
import ru.nikidzawa.datingapp.telegramBot.services.parsers.JsonParser;

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

    private final UserRepository userRepository;

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

    @PostMapping("api/postEventCity")
    public EventCity postEventCity (@RequestBody EventCity eventCity) {
        Geocode geocode = jsonParser.parseGeocode(externalApi.getCoordinates(eventCity.getName()));
        eventCity.setLatitude(geocode.getLat());
        eventCity.setLongitude(geocode.getLon());
        return eventCityRepository.saveAndFlush(eventCity);
    }

    @GetMapping("api/getEventsByCityId/{cityId}")
    public List<EventEntity> getEventsByCityId (@PathVariable Long cityId) {
        EventCity eventCity = eventCityRepository.findById(cityId).get();
        return eventCity.getEventEntities();
    }

    @GetMapping("api/getEventCities")
    public List<EventCity> getEventCities() {
        return eventCityRepository.findAll();
    }

    @DeleteMapping("api/deleteEventCity/{eventCityId}")
    public ResponseEntity<?> deleteEventCity (@PathVariable Long eventCityId) {
        eventCityRepository.deleteById(eventCityId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("api/getEventsByEventCityId/{eventCityId}")
    public List<EventEntity> getEventsByEventCityId (@PathVariable Long eventCityId) {
        EventCity eventCity = eventCityRepository.findById(eventCityId).get();
        return eventCity.getEventEntities();
    }

    @PostMapping("api/postEventType")
    public EventType postEventType (@RequestBody EventType eventType) {
        return eventTypeRepository.saveAndFlush(eventType);
    }

    @GetMapping("api/getAllEventTypes")
    public List<EventType> getAllEventTypes () {
        return eventTypeRepository.findAll();
    }

    @DeleteMapping("api/deleteEventType/{id}")
    public ResponseEntity<?> deleteEventTypeById (@PathVariable Long id) {
        eventTypeRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("api/getEventsByType/{typeName}")
    public List<EventEntity> getEventsByType (@PathVariable String typeName) {
        return eventRepository.findByEventTypeName(typeName);
    }

    @SneakyThrows
    @PostMapping("api/postEvent")
    public ResponseEntity<?> postEvent(@RequestBody EventEntity eventEntity) {
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

        EventType eventType = eventTypeRepository.findById(eventEntity.getEventType().getId()).get();
        eventType.getEventEntities().add(eventEntity);
        eventTypeRepository.saveAndFlush(eventType);

        EventCity eventCity = eventCityRepository.findById(eventEntity.getCity().getId()).get();
        eventCity.getEventEntities().add(eventEntity);
        eventCityRepository.saveAndFlush(eventCity);

        return ResponseEntity.ok().build();
    }
}