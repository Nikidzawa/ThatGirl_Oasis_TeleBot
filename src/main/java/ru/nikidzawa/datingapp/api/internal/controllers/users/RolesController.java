package ru.nikidzawa.datingapp.api.internal.controllers.users;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.*;
import ru.nikidzawa.datingapp.api.internal.exceptions.Unauthorized;
import ru.nikidzawa.datingapp.telegramBot.botFunctions.BotFunctions;

@Setter
@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("api/users/roles/")
public class RolesController {

    BotFunctions botFunctions;

    @GetMapping("{id}")
    public String getUserStatus (@PathVariable Long id) {
        return botFunctions.getChatMember(id).getStatus();
    }

    public void checkAdminStatus(Long userid) {
        String status;
        try {
            status = botFunctions.getChatMember(userid).getStatus();
        } catch (Exception e) {
            throw new Unauthorized("Недостаточно прав для запроса");
        }
        if (!status.equals("administrator") && !status.equals("creator")) {
            throw new Unauthorized("Недостаточно прав для запроса");
        }
    }
}
