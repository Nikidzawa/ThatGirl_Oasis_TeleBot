package ru.nikidzawa.datingapp.api.internal.controllers.users;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;
import ru.nikidzawa.datingapp.api.internal.exceptions.NotFoundException;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.store.repositories.UserRepository;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("api/users/")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserRepository userRepository;

    @Cacheable(cacheNames = "user", key = "#userId")
    @GetMapping("{userId}")
    public UserEntity getUser (@PathVariable Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }
}
