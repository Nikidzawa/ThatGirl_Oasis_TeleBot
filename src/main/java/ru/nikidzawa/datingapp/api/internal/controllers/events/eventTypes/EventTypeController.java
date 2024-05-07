package ru.nikidzawa.datingapp.api.internal.controllers.events.eventTypes;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nikidzawa.datingapp.api.internal.controllers.users.RolesController;
import ru.nikidzawa.datingapp.store.entities.event.EventEntity;
import ru.nikidzawa.datingapp.store.entities.event.EventType;
import ru.nikidzawa.datingapp.store.repositories.EventTypeRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("api/eventTypes/")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventTypeController {

    EventTypeRepository eventTypeRepository;

    RolesController rolesController;

    @PostMapping("{userId}")
    public EventType postEventType (@RequestBody EventType eventType, @PathVariable Long userId) {
        rolesController.checkAdminStatus(userId);
        return eventTypeRepository.saveAndFlush(eventType);
    }

    @GetMapping("getEventsByTypeId/{typeId}")
    public List<EventEntity> getEventTypes(@PathVariable Long typeId) {
        return eventTypeRepository.findEventEntitiesByEventTypeId(typeId);
    }

    @GetMapping("getAll")
    public List<EventType> getAllEventTypes () {
        return eventTypeRepository.findAll();
    }

    @DeleteMapping("/{typeId}/{userId}")
    public ResponseEntity<?> deleteEventTypeById (@PathVariable Long typeId, @PathVariable Long userId) {
        rolesController.checkAdminStatus(userId);
        eventTypeRepository.deleteById(typeId);
        return ResponseEntity.ok().build();
    }
}
