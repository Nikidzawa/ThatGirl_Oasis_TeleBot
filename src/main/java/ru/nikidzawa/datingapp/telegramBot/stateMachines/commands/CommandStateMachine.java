package ru.nikidzawa.datingapp.telegramBot.stateMachines.commands;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.nikidzawa.datingapp.store.entities.complain.ComplainEntity;
import ru.nikidzawa.datingapp.store.entities.error.ErrorEntity;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.telegramBot.botFunctions.BotFunctions;
import ru.nikidzawa.datingapp.telegramBot.cache.CacheService;
import ru.nikidzawa.datingapp.telegramBot.helpers.Messages;
import ru.nikidzawa.datingapp.telegramBot.services.DataBaseService;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.mainStates.StateEnum;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.mainStates.StateMachine;
import ru.nikidzawa.datingapp.telegramBot.stateMachines.roles.RolesController;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class CommandStateMachine {

    private final HashMap<String, CommandState> commands;

    @Autowired
    private StateMachine stateMachine;

    @Autowired
    private Messages messages;

    @Autowired
    private DataBaseService dataBaseService;

    @Autowired
    private RolesController rolesController;

    @Autowired
    private CacheService cacheService;

    @Setter
    public BotFunctions botFunctions;

    public CommandStateMachine() {
        commands = new HashMap<>();
        commands.put("/start", new Start());
        commands.put("/menu", new Menu());
        commands.put("/faq", new FAQ());
        commands.put("/error", new Error());
        commands.put("/show_errors", new ShowErrors());
        commands.put("/analysis", new Analysis());
        commands.put("/complaints", new Complains());
    }

    public void handleInput(long userId, Message message, String status, Optional<UserEntity> optionalUser) {
        String messageText = message.getText();
        CommandState commandState = commands.get(messageText);
        if (commandState != null) {
            cacheService.evictCachedUser(userId);
            cacheService.evictComplaintUser(userId);
            commandState.handleInput(userId, message, status, optionalUser);
        } else {
            botFunctions.sendMessageNotRemoveKeyboard(userId, "Команда не найдена. Если вы не хотели указывать команду, то не начинайте сообщение со знака /");
        }
    }

    private class Start implements CommandState {
        @Override
        public void handleInput(long userId, Message message, String role, Optional<UserEntity> optionalUser) {
            if (rolesController.allRoles.contains(role)) {
                optionalUser.ifPresentOrElse(userEntity -> {
                    if (userEntity.isActive()) {
                        stateMachine.goToMenu(userId, userEntity);
                    } else {
                        stateMachine.handleInput(StateEnum.WELCOME_BACK, userId, userEntity, message, true);
                    }
                }, () -> stateMachine.handleInput(StateEnum.START, userId, null, message, false));
            } else botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getNOT_GROUP_MEMBER_EXCEPTION());
        }
    }

    private class Menu implements CommandState {
        @Override
        public void handleInput(long userId, Message message, String role, Optional<UserEntity> optionalUser) {
            if (rolesController.allRoles.contains(role)) {
                optionalUser.ifPresentOrElse(userEntity -> {
                    stateMachine.goToMenu(userId, userEntity);
                    if (!userEntity.isActive()) {
                        userEntity.setActive(true);
                        dataBaseService.saveUser(userEntity);
                    }
                }, () -> botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getNOT_REGISTER()));
            } else botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getNOT_GROUP_MEMBER_EXCEPTION());
        }
    }

    private class Error implements CommandState {
        @Override
        public void handleInput(long userId, Message message, String role, Optional<UserEntity> optionalUser) {
            if (rolesController.allRoles.contains(role)) {
                optionalUser.ifPresentOrElse(userEntity -> {
                    if (!userEntity.isActive()) {
                        stateMachine.handleInput(StateEnum.WELCOME_BACK, userId, userEntity, message, true);
                        return;
                    }
                    cacheService.setState(userId, StateEnum.SEND_ERROR);
                    botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getSEND_ERROR());
                }, () -> botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getNOT_REGISTER()));
            } else botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getNOT_GROUP_MEMBER_EXCEPTION());
        }
    }

    private class FAQ implements CommandState {
        @Override
        public void handleInput(long userId, Message message, String role, Optional<UserEntity> optionalUser) {
            if (rolesController.allRoles.contains(role)) {
                optionalUser.ifPresentOrElse(userEntity -> {
                    if (!userEntity.isActive()) {
                        stateMachine.handleInput(StateEnum.WELCOME_BACK, userId, userEntity, message, true);
                        return;
                    }
                    botFunctions.sendMessageAndKeyboard(userId, messages.getFAQ(), botFunctions.faqButtons());
                    cacheService.setState(userId, StateEnum.FAQ);
                }, () -> botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getNOT_REGISTER()));
            } else botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getNOT_GROUP_MEMBER_EXCEPTION());
        }
    }

    private class Analysis implements CommandState {
        @Override
        public void handleInput(long userId, Message message, String role, Optional<UserEntity> optionalUser) {
            if (rolesController.superRoles.contains(role)) {
                botFunctions.sendMessageAndRemoveKeyboard(userId, "Идёт анализ, пожалуйста, подождите...");
                String[] results = dataBaseService.findTop10CitiesByUserCount();
                Long size = dataBaseService.getCountActiveAndNotBannedUsers();
                StringBuilder stringBuilder = new StringBuilder();

                stringBuilder.append("Рейтинг топ 10 популярных городов:").append("\n");
                for (int i = 0; i < results.length; i++) {
                    String[] cityAndNumber = results[i].split(",");
                    String city = cityAndNumber[0];
                    String count = cityAndNumber[1];
                    stringBuilder.append(i + 1).append(". ").append(city).append(" - ").append(count).append(" ").append(wordParser(count)).append("\n");
                }

                stringBuilder.append("\n").append("Число активных пользователей: ").append(size);

                botFunctions.sendMessageNotRemoveKeyboard(userId, stringBuilder.toString());
            } else {
                botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getNOT_ENOUGH());
            }
        }
        private String wordParser (String count) {
            if (count.equals("1")) {
                return "анкета";
            } else if (count.equals("2") || count.equals("3") || count.equals("4")) {
                return "анкеты";
            } else {
                return "анкет";
            }
        }
    }

    private class ShowErrors implements CommandState {
        @Override
        public void handleInput(long userId, Message message, String role, Optional<UserEntity> optionalUser) {
            if (rolesController.superRoles.contains(role)) {
                List<ErrorEntity> errorEntities = dataBaseService.findAllErrors();
                Optional<ErrorEntity> optionalError = errorEntities.stream().findAny();
                optionalError.ifPresentOrElse(errorEntity -> {
                    botFunctions.sendMessageNotRemoveKeyboard(
                            userId,
                            "Ошибка номер: " + errorEntity.getId() + "\nОписание ошибки: " + errorEntity.getDescription()
                    );
                    dataBaseService.deleteError(errorEntity);
                }, () -> botFunctions.sendMessageNotRemoveKeyboard(userId, "Больше жалоб не поступало"));
            } else {
                botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getNOT_ENOUGH());
            }
        }
    }

    private class Complains implements CommandState {
        @Override
        public void handleInput(long userId, Message message, String role, Optional<UserEntity> optionalUser) {
            if (rolesController.superRoles.contains(role)) {
                getComplaint(userId);
            } else {
                botFunctions.sendMessageNotRemoveKeyboard(userId, messages.getNOT_ENOUGH());
            }
        }
    }
    public void getComplaint(long userId) {
        List<ComplainEntity> complainEntities = dataBaseService.findAllComplaints();
        Optional<ComplainEntity> optionalComplain = complainEntities.stream().findAny();
        optionalComplain.ifPresentOrElse(complainEntity -> {
            UserEntity complainUser = complainEntity.getComplaintUser();
            botFunctions.sendMessageAndRemoveKeyboard(
                    userId,
                    "Жалоба номер: " + complainEntity.getId()  +
                            "\nОбщее число жалоб на пользователя: " + countComplainsByUserId(complainEntities, complainUser.getId())
            );
            botFunctions.sendDatingProfileAndJudgeButtons(userId, complainUser);
            botFunctions.sendMessageNotRemoveKeyboard(userId, "Описание жалобы: " + complainEntity.getDescription());
        }, () -> botFunctions.sendMessageNotRemoveKeyboard(userId, "Больше жалоб не поступало"));
    }

    private long countComplainsByUserId (List<ComplainEntity> complainEntities, Long userId) {
        return complainEntities.stream().filter(complainEntity -> Objects.equals(complainEntity.getComplaintUser().getId(), userId)).count();
    }
}