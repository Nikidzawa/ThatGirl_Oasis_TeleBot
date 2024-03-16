package ru.nikidzawa.datingapp.TelegramBot;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import ru.nikidzawa.datingapp.TelegramBot.botFunctions.BotFunctions;
import ru.nikidzawa.datingapp.TelegramBot.cache.CacheService;
import ru.nikidzawa.datingapp.TelegramBot.helpers.Messages;
import ru.nikidzawa.datingapp.TelegramBot.helpers.UserAndState;
import ru.nikidzawa.datingapp.TelegramBot.services.DataBaseService;
import ru.nikidzawa.datingapp.TelegramBot.stateMachines.commands.CommandStateMachine;
import ru.nikidzawa.datingapp.TelegramBot.stateMachines.states.StateEnum;
import ru.nikidzawa.datingapp.TelegramBot.stateMachines.states.StateMachine;
import ru.nikidzawa.datingapp.store.entities.complain.ComplainEntity;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.store.repositories.ComplaintRepository;

import java.util.List;
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
    CommandStateMachine commandStateMachine;

    @Autowired
    DataBaseService dataBaseService;

    @Autowired
    ComplaintRepository complaintRepository;

    @Autowired
    CacheService cacheService;

    @PostConstruct
    @SneakyThrows
    private void init () {
        botFunctions = new BotFunctions(this);
        stateMachine.setBotFunctions(botFunctions);
        commandStateMachine.setBotFunctions(botFunctions);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            Optional<UserEntity> optionalUser = dataBaseService.getUserById(chatId);
            optionalUser.ifPresentOrElse(userEntity -> {
                if (!userEntity.isBanned() && userEntity.isActive()) {
                    String[] response = update.getCallbackQuery().getData().split(",");
                    switch (response[0]) {
                        case "complaint" -> {
                            botFunctions.sendMessageAndMarkup(chatId, "Вы собираетесь отправить жалобу. Пожалуйста, опишите в деталях вашу претензию и мы немедленно на неё отреагируем", botFunctions.cancelButton());
                            cacheService.putComplaintUser(chatId, Long.valueOf(response[1]));
                            cacheService.setState(chatId, StateEnum.CALL_BACK_QUERY_COMPLAIN);
                        }
                        case "block" -> {
                            botFunctions.sendMessageAndRemoveMarkup(chatId, "Пользователь заблокирован");
                            UserEntity complaintUser = dataBaseService.getUserById(Long.valueOf(response[1])).get();
                            List<ComplainEntity> complainEntities = complaintRepository.findByComplaintUser(complaintUser);
                            complaintUser.setComplaints(null);
                            complaintUser.setBanned(true);
                            dataBaseService.saveUser(complaintUser);
                            complaintRepository.deleteAll(complainEntities);
                        }
                        case "peace" -> {
                            botFunctions.sendMessageAndRemoveMarkup(chatId, "Пользователь помилован");
                            UserEntity complaintUser = dataBaseService.getUserById(Long.valueOf(response[1])).get();
                            List<ComplainEntity> complainEntities = complaintRepository.findByComplaintUser(complaintUser);
                            complaintUser.setComplaints(null);
                            complaintUser.setBanned(false);
                            dataBaseService.saveUser(complaintUser);
                            complaintRepository.deleteAll(complainEntities);
                        }
                    }
                } else {botFunctions.sendMessageNotRemoveMarkup(chatId, "Вы не имеете прав отправлять жалобы");}
            }, () -> botFunctions.sendMessageNotRemoveMarkup(chatId, "Сначала необходимо зарегистрироваться"));
            return;
        }
        Message message = update.getMessage();
        long userId = update.getMessage().getFrom().getId();
        Optional<UserEntity> optionalUser = dataBaseService.getUserById(userId);
        ChatMember chatMember = botFunctions.getChatMember(userId);
        String role = chatMember.getStatus();
        if (!message.isSuperGroupMessage()) {
             if (message.hasText() && message.getText().startsWith("/")) {
                commandStateMachine.handleInput(userId, message, role, optionalUser);
            } else {
                if (isSubscribe(userId, role)) {
                    UserAndState userAndState = userAndStateIdentification(userId, optionalUser, message);
                    stateMachine.handleInput(userAndState.getStateEnum(), userId, userAndState.getUser(), message, userAndState.isHasBeenRegistered());
                }
            }
        } else {
            if (role.equals("left")) {stateMachine.handleInput(StateEnum.LEFT, userId, null, message, false);}
        }

        new Thread(() -> checkNewUsers(message)).start();
    }

    @SneakyThrows
    private boolean isSubscribe (long userId, String role) {
        if (role.equals("member") || role.equals("creator") || role.equals("admin")) {return true;}
        else {
            botFunctions.sendMessageAndRemoveMarkup(userId, messages.getNOT_GROUP_MEMBER_EXCEPTION());
            return false;
        }
    }

    private void checkNewUsers (Message message) {
        message.getNewChatMembers().forEach(chatMember -> stateMachine.handleInput(StateEnum.START,  chatMember.getId(), null, message, false));
    }

    private UserAndState userAndStateIdentification (Long userId, Optional<UserEntity> optionalUser, Message message) {
        Cache.ValueWrapper optionalCurrentState = cacheService.getCurrentState(userId);
        if (optionalCurrentState == null) {
            return optionalUser.map(user -> {
                if (user.isActive()) {
                    if (!message.getText().equals("1") && !message.getText().equals("2") && !message.getText().equals("3")) {
                        botFunctions.sendMessageAndRemoveMarkup(userId, "Время ожидания истекло, возвращаемся в главное меню");
                        stateMachine.goToMenu(userId, user);
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