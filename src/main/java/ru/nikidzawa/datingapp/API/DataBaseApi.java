package ru.nikidzawa.datingapp.API;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nikidzawa.datingapp.store.entities.event.EventCart;
import ru.nikidzawa.datingapp.store.entities.event.EventEntity;
import ru.nikidzawa.datingapp.store.entities.event.EventImage;
import ru.nikidzawa.datingapp.store.entities.event.EventType;
import ru.nikidzawa.datingapp.store.entities.user.UserSiteAccount;
import ru.nikidzawa.datingapp.store.repositories.*;
import ru.nikidzawa.datingapp.telegramBot.botFunctions.BotFunctions;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@CrossOrigin
public class DataBaseApi {

    private final EventRepository eventRepository;

    private final UserSiteAccountRepository userSiteAccountRepository;

    private final EventImageRepository eventImageRepository;

    private final EventTypeRepository eventTypeRepository;

    private final ExternalApi externalApi;
    
    private final EventCartRepository eventCartRepository;

    @Setter
    BotFunctions botFunctions;

    @GetMapping("api/getUserStatus/{id}")
    public String getUserStatus (@PathVariable Long id) {
        return botFunctions.getChatMember(id).getStatus();
    }

    @GetMapping("api/getUserSite/{id}")
    @Cacheable(cacheNames = "userSite", key = "#id")
    public Optional<UserSiteAccount> getUserSite (@PathVariable Long id) {
        return userSiteAccountRepository.findById(id);
    }

    @GetMapping("api/getEvent/{id}")
    public EventEntity getEventById (@PathVariable Long id) {
        return eventRepository.findById(id).get();
    }

    @GetMapping("api/getAllEvents")
    public List<EventEntity> getAllEvents () {
        return eventRepository.findAll();
    }

    @GetMapping("api/getUserEvents/{userId}")
    public List<EventEntity> getUserEvents (@PathVariable Long userId) {
        return userSiteAccountRepository.findById(userId).get().getEvents();
    }

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

    @PostMapping("api/addEventToCart/{userId}/{eventId}")
    public EventCart addEventToCart (@PathVariable Long userId, @PathVariable Long eventId) {
        Optional<EventCart> eventCartOptional = eventCartRepository.findById(eventId);
        if (eventCartOptional.isPresent()) {
            EventCart eventCart = eventCartOptional.get();
            eventCart.setCount(eventCart.getCount() + 1);
            return eventCartRepository.saveAndFlush(eventCart);
        } else {
            EventCart eventCart = EventCart.builder()
                    .id(eventId)
                    .count(1)
                    .event(eventRepository.findById(eventId).get())
                    .build();
            eventCartRepository.saveAndFlush(eventCart);
            UserSiteAccount userSiteAccount = userSiteAccountRepository.findById(userId).get();
            userSiteAccount.getEventAddedToCart().add(eventCart);
            userSiteAccountRepository.saveAndFlush(userSiteAccount);
            return eventCart;
        }
    }

    @PostMapping("api/removeEventFromCart/{userId}/{eventId}")
    public EventCart removeEventFromCart (@PathVariable Long userId, @PathVariable Long eventId) {
        Optional<EventCart> eventCartOptional = eventCartRepository.findById(eventId);
        if (eventCartOptional.isPresent()) {
            EventCart eventCart = eventCartOptional.get();
            int count = eventCart.getCount() - 1;
            if (count == 0) {
                UserSiteAccount userSiteAccount = userSiteAccountRepository.findById(userId).get();
                userSiteAccount.getEventAddedToCart().remove(eventCart);
                userSiteAccountRepository.saveAndFlush(userSiteAccount);
                eventCartRepository.delete(eventCart);
                return eventCart;
            } else {
                eventCart.setCount(count);
                return eventCartRepository.saveAndFlush(eventCart);
            }
        }
        return null;
    }


    @GetMapping("api/getUserCartEvents/{userId}")
    public List<EventCart> getUserCartEvents (@PathVariable Long userId) {
        UserSiteAccount userSiteAccount = userSiteAccountRepository.findById(userId).get();
        return userSiteAccount.getEventAddedToCart();
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

        return ResponseEntity.ok().build();
    }
}