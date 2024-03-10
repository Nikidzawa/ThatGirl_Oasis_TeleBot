package ru.nikidzawa.datingapp.TelegramBot;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import ru.nikidzawa.datingapp.TelegramBot.helpers.Messages;
import ru.nikidzawa.datingapp.TelegramBot.helpers.UserAndState;
import ru.nikidzawa.datingapp.TelegramBot.cache.CacheService;
import ru.nikidzawa.datingapp.TelegramBot.stateMachine.StateEnum;
import ru.nikidzawa.datingapp.TelegramBot.stateMachine.StateMachine;
import ru.nikidzawa.datingapp.entities.UserEntity;

import java.util.Optional;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {return "ThatGirl Oasis";}

    @Override
    public String getBotToken() {return "6892584290:AAEqe_WDIf1oRBZW5kVOgg2OjIBPK9mPihA";}

    public BotFunctions botFunctions;

    @Autowired
    Messages messages;

    @Autowired
    StateMachine stateMachine;

    @Autowired
    DataBaseService dataBaseService;

    @Autowired
    CacheService cacheService;

    @PostConstruct
    @SneakyThrows
    private void init () {
        botFunctions = new BotFunctions(this);
        stateMachine.setBotFunctions(botFunctions);
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        long userId = update.getMessage().getFrom().getId();
        new Thread(() -> checkNewUsers(message)).start();

        Optional<UserEntity> optionalUser = dataBaseService.getUserById(userId);
        if (message.getText() != null && message.getText().equals("/main")) {
            optionalUser.ifPresentOrElse(user -> stateMachine.handleInput(StateEnum.MENU, userId, user, message, true), () -> {
                botFunctions.sendMessageAndRemoveMarkup(userId, "Сначала необходимо зарегистрироваться");
            });
        } else {
            if (isSubscribe("@nikidzawa_group", userId, message)) {
                UserAndState userAndState = userAndStateIdentification(userId,optionalUser, update.getMessage());
                stateMachine.handleInput(userAndState.getStateEnum(), userId, userAndState.getUser(), message, userAndState.isHasBeenRegistered());
            }
        }
    }

    @SneakyThrows
    public boolean isSubscribe (String chatName, long userId, Message message) {
        ChatMember chatMember = execute(new GetChatMember(chatName, userId));
        String status = chatMember.getStatus();
        if (message.isSuperGroupMessage()) {
            if (status.equals("left")) {stateMachine.handleInput(StateEnum.LEFT, userId, null, message, false);}
        } else {
            if (status.equals("member") || status.equals("creator") || status.equals("admin")) {return true;}
            else {stateMachine.handleInput(StateEnum.CHECK_GROUP_MEMBER, userId, null, message, false);}
        }
        return false;
    }

    public void checkNewUsers (Message message) {
        message.getNewChatMembers().forEach(chatMember -> stateMachine.handleInput(StateEnum.START,  chatMember.getId(), null, message, false));
    }

    public UserAndState userAndStateIdentification (Long userId, Optional<UserEntity> optionalUser, Message message) {
        Cache.ValueWrapper optionalCurrentState = cacheService.getCurrentState(userId);
        if (optionalCurrentState == null) {
            return optionalUser.map(user -> {
                if (user.getIsActive()) {
                    if (!message.getText().equals("1") && !message.getText().equals("2") && !message.getText().equals("3")) {
                        botFunctions.sendMessageAndRemoveMarkup(userId, "Время ожидания истекло, возвращаемся в главное меню");
                        botFunctions.sendMessageAndMarkup(userId, messages.getMENU(), botFunctions.menuButtons());
                    }
                    return new UserAndState(user, cacheService.setState(userId, StateEnum.MENU), true);
                } else {
                    botFunctions.sendMessageAndMarkup(userId,
                            "Привет, " + message.getFrom().getFirstName() + "\n" +
                                    "Я рада, что ты вернулась в наше сообщество! \uD83D\uDC96\n" +
                                    "\n" +
                                    "Давай включим тебе анкету?\n", botFunctions.welcomeBackButton());
                    return new UserAndState(user, cacheService.setState(userId, StateEnum.WELCOME_BACK), true);
                }
            }).orElseGet(() -> new UserAndState(null, cacheService.setState(userId, StateEnum.START), false));
        } else {
            StateEnum currentState = (StateEnum) optionalCurrentState.get();
            return optionalUser.map(user -> new UserAndState(user, currentState, true))
                    .orElseGet(() -> new UserAndState(null, currentState, false));
        }
    }
}