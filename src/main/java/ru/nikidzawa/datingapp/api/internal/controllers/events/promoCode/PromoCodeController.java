package ru.nikidzawa.datingapp.api.internal.controllers.events.promoCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nikidzawa.datingapp.api.internal.controllers.users.RolesController;
import ru.nikidzawa.datingapp.api.internal.exceptions.NotFoundException;
import ru.nikidzawa.datingapp.api.internal.exceptions.OtherException;
import ru.nikidzawa.datingapp.store.entities.event.PromoCodeEntity;
import ru.nikidzawa.datingapp.store.repositories.PromoCodeRepository;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("api/promoCode/")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PromoCodeController {

    PromoCodeRepository promoCodeRepository;

    RolesController rolesController;

    @GetMapping("isPresent/{promoCode}")
    private boolean promoCodeIsActive (@PathVariable String promoCode) {
        try {
            return promoCodeRepository.findByPromoCode(promoCode).isPresent();
        } catch (Exception ex) {
            throw new OtherException("Ошибка при проверке подлинности промокода " + ex.getMessage());
        }
    }

    @GetMapping("{id}")
    private PromoCodeEntity getPromoCode (@PathVariable Long id) {
        return promoCodeRepository.findById(id).orElseThrow(() -> new NotFoundException("Промокод не найден"));
    }

    @PostMapping("{userId}")
    private ResponseEntity<?> createPromoCode (
            @PathVariable Long userId,
            @RequestBody PromoCodeEntity promoCodeEntity) {
        rolesController.checkAdminStatus(userId);
        try {
            promoCodeRepository.saveAndFlush(promoCodeEntity);
        } catch (Exception ex) {
            throw new OtherException("Ошибка при сохранении промокода " + ex.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}