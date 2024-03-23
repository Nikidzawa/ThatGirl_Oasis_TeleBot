package ru.nikidzawa.datingapp;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import ru.nikidzawa.datingapp.store.entities.event.EventEntity;
import ru.nikidzawa.datingapp.store.entities.user.UserSiteAccountEntity;
import ru.nikidzawa.datingapp.store.repositories.EventRepository;
import ru.nikidzawa.datingapp.store.repositories.UserRepository;
import ru.nikidzawa.datingapp.store.repositories.UserSiteAccountRepository;
import ru.nikidzawa.datingapp.telegramBot.botFunctions.BotFunctions;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class DataBaseApi {
    @Autowired
    UserRepository userRepository;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    UserSiteAccountRepository userSiteAccountRepository;

    @Setter
    BotFunctions botFunctions;

    @CrossOrigin
    @GetMapping("api/getUserStatus/{id}")
    public String getUserStatus (@PathVariable Long id) {
        return botFunctions.getChatMember(id).getStatus();
    }

    @CrossOrigin
    @GetMapping("api/getUserSite/{id}")
    @Cacheable(cacheNames = "userSite", key = "#id")
    public Optional<UserSiteAccountEntity> getUserSite (@PathVariable Long id) {
        return userSiteAccountRepository.findById(id);
    }

    @CrossOrigin
    @GetMapping("api/getEvent/{id}")
    @Cacheable(cacheNames = "event", key = "#id")
    public Optional<EventEntity> getEventById (@PathVariable Long id) {
        return eventRepository.findById(id);
    }

    @CrossOrigin
    @GetMapping("api/getAllEvents")
    public List<EventEntity> getAllEvents () {
        return eventRepository.findAll();
    }

    @CrossOrigin
    @GetMapping("api/getUserEvents/{userId}")
    public List<EventEntity> getUserEvents (@PathVariable Long userId) {
        return userSiteAccountRepository.findById(userId).get().getEvents();
    }

    @CrossOrigin
    @PostMapping("api/subscribeEvent/{userId}/{eventId}")
    public ResponseEntity<?> subscribeToEvent(@PathVariable Long userId, @PathVariable Long eventId) {
        Optional<UserSiteAccountEntity> optionalUserSiteAccount = userSiteAccountRepository.findById(userId);
        Optional<EventEntity> optionalEvent = eventRepository.findById(eventId);
        if (optionalUserSiteAccount.isPresent() && optionalEvent.isPresent()) {
            UserSiteAccountEntity userSiteAccount = optionalUserSiteAccount.get();
            EventEntity event = optionalEvent.get();
            List<EventEntity> eventEntities = userSiteAccount.getEvents();
            List<UserSiteAccountEntity> userEntities = event.getUsers();
            if (eventEntities.contains(event) || userEntities.contains(userSiteAccount)) {
                return ResponseEntity.badRequest().body("Юзер уже зарегистрирован на это мероприятие");
            } else {
                userEntities.add(userSiteAccount);
                eventRepository.saveAndFlush(event);

                eventEntities.add(event);
                userSiteAccountRepository.saveAndFlush(userSiteAccount);
                return ResponseEntity.ok().body("Успешно");
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @CrossOrigin
    @PostMapping("api/postEvent")
    public String saveEvent(@RequestBody EventEntity eventEntity) {
        eventEntity = eventRepository.saveAndFlush(eventEntity);
        ResponseEntity<String> response = uploadImage(eventEntity.getId(), eventEntity.getImage());
        eventEntity.setImage(String.valueOf(eventEntity.getId()));
        eventRepository.saveAndFlush(eventEntity);
        return response.getStatusCode().toString();
    }

    private ResponseEntity<String> uploadImage(Long fileId, String url) {
        String postImageUrl = "https://cloud-api.yandex.net/v1/disk/resources/upload?path=" + fileId + "&url=" + url;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "OAuth y0_AgAAAAA6DSoUAADLWwAAAAD-7B2bAAAmShKyx9pJQ7i_DbM850VcHQn9NA");
        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

        RestTemplate restTemplate = new RestTemplate();

        return restTemplate.exchange(
                postImageUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );
    }
}