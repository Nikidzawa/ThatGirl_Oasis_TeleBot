package ru.nikidzawa.datingapp.telegramBot;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import ru.nikidzawa.datingapp.api.internal.controllers.users.RolesController;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.telegramBot.botFunctions.BotFunctions;
import ru.nikidzawa.datingapp.telegramBot.cache.CacheService;
import ru.nikidzawa.datingapp.telegramBot.helpers.Messages;
import ru.nikidzawa.datingapp.telegramBot.services.DataBaseService;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.callBacks.CallBacksStateMachine;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.commands.CommandStateMachine;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.mainStates.StateEnum;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.mainStates.StateMachine;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.roles.RoleStates;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {return "@ThatGirl_Oasis_bot";}

    @Override
    public String getBotToken() {return "7008547107:AAF1XURy4dClvFnPOvS_daa3vsWryMBfscQ";}

    public BotFunctions botFunctions;

    @Autowired
    Messages messages;

    @Autowired
    StateMachine stateMachine;

    @Autowired
    CommandStateMachine commandStateMachine;

    @Autowired
    DataBaseService dataBaseService;

    @Autowired
    CacheService cacheService;

    @Autowired
    CallBacksStateMachine callBacksStateMachine;

    @Autowired
    RoleStates roleStates;

    @Autowired
    RolesController rolesController;


    private final HashSet<String> menuButtons;

    public TelegramBot () {
        menuButtons = new HashSet<>(List.of("1", "2", "3", "4"));
    }

    @PostConstruct
    @SneakyThrows
    private void init () {
        botFunctions = new BotFunctions(this);
        stateMachine.setBotFunctions(botFunctions);
        commandStateMachine.setBotFunctions(botFunctions);
        callBacksStateMachine.setBotFunctions(botFunctions);
        rolesController.setBotFunctions(botFunctions);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            Long userId = update.getCallbackQuery().getMessage().getChatId();
            Optional<UserEntity> optionalUser = dataBaseService.getUserById(userId);
            optionalUser.ifPresentOrElse(userEntity -> {
                if (!userEntity.isBanned() && userEntity.isActive()) {
                    String[] response = update.getCallbackQuery().getData().split(",");
                    callBacksStateMachine.handleCallback(response[0], userId, Long.parseLong(response[1]));
                } else {botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getROLE_EXCEPTION());}
            }, () -> botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getNOT_REGISTER()));
            return;
        }
        Message message = update.getMessage();
        Long userId = update.getMessage().getFrom().getId();
        Optional<UserEntity> optionalUser = dataBaseService.getUserById(userId);
        ChatMember chatMember = botFunctions.getChatMember(userId);
        String role = chatMember.getStatus();
        if (message.isUserMessage()) {
             if (message.isCommand()) {
                 commandStateMachine.handleInput(userId, message, role, optionalUser);
            } else {
                 userAndStateIdentification(userId, optionalUser, message);
            }
        }
    }

    private void userAndStateIdentification (Long userId, Optional<UserEntity> optionalUser, Message message) {
        Cache.ValueWrapper optionalCurrentState = cacheService.getCurrentState(userId);
        if (optionalCurrentState == null) {
             optionalUser.ifPresentOrElse(user -> {
                if (user.isActive()) {
                    if (message.hasText() && menuButtons.contains(message.getText())) {
                        if (user.getLikesGiven().isEmpty()) {
                            stateMachine.handleInput(StateEnum.MENU, userId, user, message, true);
                        } else {
                            stateMachine.handleInput(StateEnum.SUPER_MENU, userId, user, message, true);
                        }
                    } else {
                        botFunctions.sendMessageAndRemoveKeyboard(userId, messages.getWAIT_TIME_OUT_EXCEPTION());
                        stateMachine.goToMenu(userId, user);
                    }
                } else {
                    stateMachine.handleInput(StateEnum.WELCOME_BACK, userId, user, message, true);
                }
            }, () -> stateMachine.handleInput(StateEnum.START, userId, null, message, false));
        } else {
            StateEnum currentState = (StateEnum) optionalCurrentState.get();
            optionalUser.ifPresentOrElse(user -> stateMachine.handleInput(currentState, userId, user, message, true),
                    () -> stateMachine.handleInput(currentState, userId, null, message, false));
        }
    }
}