package ru.nikidzawa.datingapp;

import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import ru.nikidzawa.datingapp.store.entities.event.EventEntity;
import ru.nikidzawa.datingapp.store.entities.event.EventImage;
import ru.nikidzawa.datingapp.store.entities.user.UserSiteAccount;
import ru.nikidzawa.datingapp.store.repositories.EventImageRepository;
import ru.nikidzawa.datingapp.store.repositories.EventRepository;
import ru.nikidzawa.datingapp.store.repositories.UserRepository;
import ru.nikidzawa.datingapp.store.repositories.UserSiteAccountRepository;
import ru.nikidzawa.datingapp.telegramBot.botFunctions.BotFunctions;
import ru.nikidzawa.datingapp.telegramBot.services.parsers.JsonParser;

import java.util.List;
import java.util.Optional;

import static org.aspectj.weaver.tools.cache.SimpleCacheFactory.path;

@RestController
public class DataBaseApi {
    @Autowired
    UserRepository userRepository;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    UserSiteAccountRepository userSiteAccountRepository;

    @Autowired
    EventImageRepository eventImageRepository;

    @Autowired
    JsonParser jsonParser;

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
    public Optional<UserSiteAccount> getUserSite (@PathVariable Long id) {
        return userSiteAccountRepository.findById(id);
    }

    @CrossOrigin
    @GetMapping("api/getEvent/{id}")
//    @Cacheable(cacheNames = "event", key = "#id")
    public EventEntity getEventById (@PathVariable Long id) {
        return eventRepository.findById(id).get();
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
        Optional<UserSiteAccount> optionalUserSiteAccount = userSiteAccountRepository.findById(userId);
        Optional<EventEntity> optionalEvent = eventRepository.findById(eventId);
        if (optionalUserSiteAccount.isPresent() && optionalEvent.isPresent()) {
            UserSiteAccount userSiteAccount = optionalUserSiteAccount.get();
            EventEntity event = optionalEvent.get();
            List<EventEntity> eventEntities = userSiteAccount.getEvents();
            if (eventEntities.contains(event)) {
                return ResponseEntity.badRequest().body("Юзер уже зарегистрирован на это мероприятие");
            } else {
                eventEntities.add(event);
                userSiteAccountRepository.saveAndFlush(userSiteAccount);
                return ResponseEntity.ok().body("Успешно");
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @SneakyThrows
    @CrossOrigin
    @PostMapping("api/postEvent")
    public ResponseEntity<?> saveEvent(@RequestBody EventEntity eventEntity) {
        List<EventImage> eventImages = eventEntity.getEventImages();
        eventEntity.setEventImages(null);
        EventImage mainImage = eventImageRepository.saveAndFlush(eventEntity.getMainImage());
        eventEntity.setMainImage(null);
        eventEntity = eventRepository.saveAndFlush(eventEntity);
        Long id = eventEntity.getId();
        String basePath = "/" + id + "/";
        String mainImagePath = basePath + "main";

        ResponseEntity<?> createFolderResponse = createFolder(id);
        if (!createFolderResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(createFolderResponse.getStatusCode()).body(createFolderResponse.getBody());
        }

        try {
            for (int i = 0; i < eventImages.size(); i++) {
                String imagePath = basePath + "image(" + i + ")";
                EventImage currentEventImage = eventImages.get(i);
                uploadImage(imagePath, currentEventImage.getHref());
            }
            uploadImage(mainImagePath, mainImage.getHref());
        } catch (Exception ex) {
            deleteFolder(id);
            return ResponseEntity.notFound().build();
        }

        Thread.sleep(5000);

        try {
            for (int i = 0; i < eventImages.size(); i++) {
                String imagePath = basePath + "image(" + i + ")";
                EventImage currentEventImage = eventImages.get(i);
                ResponseEntity<?> responseEntity = getDownloadLink(imagePath);
                String href = jsonParser.getHref((String) responseEntity.getBody());
                currentEventImage.setHref(href);
            }
            ResponseEntity<?> responseEntity = getDownloadLink(mainImagePath);
            String href = jsonParser.getHref((String) responseEntity.getBody());
            mainImage.setHref(href);
        } catch (Exception ex) {
            deleteFolder(id);
            return ResponseEntity.notFound().build();
        }

        eventImages = eventImageRepository.saveAllAndFlush(eventImages);
        mainImage = eventImageRepository.saveAndFlush(mainImage);

        eventEntity.setEventImages(eventImages);
        eventEntity.setMainImage(mainImage);

        eventRepository.saveAndFlush(eventEntity);
        return ResponseEntity.ok().build();
    }
    @CrossOrigin
    @GetMapping("api/event/{eventId}/images")
    public List<EventImage> saveEvent(@PathVariable Long eventId) {
        return eventRepository.findEventImagesByEventId(eventId);
    }

    private ResponseEntity<String> uploadImage(String path, String url) {
        String postImageUrl = "https://cloud-api.yandex.net/v1/disk/resources/upload?path=" + path + "&url=" + url;
        HttpEntity<String> requestEntity = new HttpEntity<>(null, getHeader());

        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.exchange(
                postImageUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );
    }
    private ResponseEntity<?> createFolder (Long path) {
        String url = "https://cloud-api.yandex.net/v1/disk/resources?path=" + path;
        HttpEntity<String> requestEntity = new HttpEntity<>(null, getHeader());

        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.exchange(
                url,
                HttpMethod.PUT,
                requestEntity,
                String.class
        );
    }
    private ResponseEntity<?> getDownloadLink (String path) {
        String url = "https://cloud-api.yandex.net/v1/disk/resources/download?path=" + path;
        HttpEntity<String> requestEntity = new HttpEntity<>(null, getHeader());

        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                String.class
        );
    }


    private ResponseEntity<?> deleteFolder (Long path) {
        String url = "https://cloud-api.yandex.net/v1/disk/resources?path=" + path;
        HttpEntity<String> requestEntity = new HttpEntity<>(null, getHeader());

        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                requestEntity,
                String.class
        );
    }

    private HttpHeaders getHeader () {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "OAuth y0_AgAAAAA6DSoUAADLWwAAAAD-7B2bAAAmShKyx9pJQ7i_DbM850VcHQn9NA");
        return headers;
    }
}