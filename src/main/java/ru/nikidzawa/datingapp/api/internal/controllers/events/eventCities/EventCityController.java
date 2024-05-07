package ru.nikidzawa.datingapp.api.internal.controllers.events.eventCities;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nikidzawa.datingapp.api.external.ExternalApi;
import ru.nikidzawa.datingapp.api.internal.controllers.users.RolesController;
import ru.nikidzawa.datingapp.store.entities.event.EventCity;
import ru.nikidzawa.datingapp.store.entities.event.EventEntity;
import ru.nikidzawa.datingapp.store.repositories.EventCityRepository;
import ru.nikidzawa.datingapp.telegramBot.services.parsers.Geocode;
import ru.nikidzawa.datingapp.telegramBot.services.parsers.JsonParser;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("api/eventCities/")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventCityController {

    RolesController rolesController;

    EventCityRepository eventCityRepository;

    JsonParser jsonParser;

    ExternalApi externalApi;

    @PostMapping("{userId}")
    public EventCity postEventCity (@RequestBody EventCity eventCity, @PathVariable Long userId) {
        rolesController.checkAdminStatus(userId);
        Geocode geocode = jsonParser.parseGeocode(externalApi.getCoordinates(eventCity.getName()));
        eventCity.setLatitude(geocode.getLat());
        eventCity.setLongitude(geocode.getLon());
        return eventCityRepository.saveAndFlush(eventCity);
    }

    @GetMapping("getAll")
    public List<EventCity> getEventCities() {
        return eventCityRepository.findAll();
    }

    @DeleteMapping("{eventCityId}/{userId}")
    public ResponseEntity<?> deleteEventCity (@PathVariable Long eventCityId, @PathVariable Long userId) {
        rolesController.checkAdminStatus(userId);
        eventCityRepository.deleteById(eventCityId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("getEventsByCityId/{cityId}")
    public List<EventEntity> getEventsByEventCityId (@PathVariable Long cityId) {
        return eventCityRepository.findEventEntitiesByCityId(cityId);
    }
}
