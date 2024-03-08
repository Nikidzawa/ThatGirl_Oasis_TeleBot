package ru.nikidzawa.datingapp.TelegramBot;

import jakarta.annotation.PostConstruct;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import ru.nikidzawa.datingapp.TelegramBot.helpers.Messages;
import ru.nikidzawa.datingapp.TelegramBot.helpers.UserAndState;
import ru.nikidzawa.datingapp.TelegramBot.cache.CacheService;
import ru.nikidzawa.datingapp.TelegramBot.stateMachine.StateEnum;
import ru.nikidzawa.datingapp.TelegramBot.stateMachine.StateMachine;
import ru.nikidzawa.datingapp.entities.UserEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {return "Leonardo_Dayvinchick";}

    @Override
    public String getBotToken() {return "6892584290:AAEqe_WDIf1oRBZW5kVOgg2OjIBPK9mPihA";}

    public BotFunctions botFunctions;

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

        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/main", "вернуться в главное меню"));
        this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
    }

    @Override
    public void onUpdateReceived(Update update) {
        new Thread(() -> {
            Message message = update.getMessage();
            long userId = update.getMessage().getFrom().getId();
            new Thread(() -> checkNewUsers(message)).start();

            Optional<UserEntity> optionalUser = dataBaseService.getUserById(userId);
            if (message.getText() != null && message.getText().equals("/main")) {
                optionalUser.ifPresentOrElse(user -> stateMachine.handleInput(StateEnum.MENU, userId, user, message), () -> {
                    botFunctions.sendMessage(userId, "Сначала необходимо зарегистрироваться");
                });
            } else {
                if (isSubscribe("@nikidzawa_group", userId, message)) {
                    UserAndState userAndState = userAndStateIdentification(userId,optionalUser);
                    stateMachine.handleInput(userAndState.getStateEnum(), userId, userAndState.getUser(), message);
                }
            }
        }).start();
    }

    @SneakyThrows
    public boolean isSubscribe (String chatName, long userId, Message message) {
        ChatMember chatMember = execute(new GetChatMember(chatName, userId));
        String status = chatMember.getStatus();
        if (message.isSuperGroupMessage()) {
            if (status.equals("left")) {stateMachine.handleInput(StateEnum.LEFT, userId, null, message);}
        } else {
            if (status.equals("member") || status.equals("creator") || status.equals("admin")) {return true;}
            else {stateMachine.handleInput(StateEnum.CHECK_GROUP_MEMBER, userId, null, message);}
        }
        return false;
    }

    public void checkNewUsers (Message message) {
        message.getNewChatMembers().forEach(chatMember -> stateMachine.handleInput(StateEnum.START,  chatMember.getId(), null, message));
    }

    public UserAndState userAndStateIdentification (Long userId, Optional<UserEntity> optionalUser) {
        Cache.ValueWrapper optionalCurrentState = cacheService.getCurrentState(userId);
        if (optionalCurrentState == null) {
            return optionalUser.map(user -> new UserAndState(user, cacheService.setState(userId, StateEnum.MENU)))
                    .orElseGet(() -> new UserAndState(null, cacheService.setState(userId, StateEnum.START)));
        } else {
            StateEnum currentState = (StateEnum) optionalCurrentState.get();
            return optionalUser.map(user -> new UserAndState(user, currentState))
                    .orElseGet(() -> new UserAndState(null, currentState));
        }
    }
}