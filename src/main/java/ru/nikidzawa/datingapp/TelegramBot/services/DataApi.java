package ru.nikidzawa.datingapp.TelegramBot.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;
import ru.nikidzawa.datingapp.store.entities.event.EventEntity;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.store.repositories.EventRepository;
import ru.nikidzawa.datingapp.store.repositories.UserRepository;

import java.util.Optional;

@RestController
public class DataApi {
    @Autowired
    UserRepository userRepository;

    @Autowired
    EventRepository eventRepository;

    @CrossOrigin
    @GetMapping("api/getUser/{id}")
    @Cacheable(cacheNames = "user", key = "#id")
    public Optional<UserEntity> getUserById (@PathVariable Long id) {
        return userRepository.findById(id);
    }

    @CrossOrigin
    @GetMapping("api/getEvent/{id}")
    @Cacheable(cacheNames = "event", key = "#id")
    public Optional<EventEntity> getEventById (@PathVariable Long id) {
        return eventRepository.findById(id);
    }

    @CrossOrigin
    @PostMapping("api/postEvent")
    public EventEntity getEventById (@RequestBody EventEntity eventEntity) {
        return eventRepository.saveAndFlush(eventEntity);
    }
}
