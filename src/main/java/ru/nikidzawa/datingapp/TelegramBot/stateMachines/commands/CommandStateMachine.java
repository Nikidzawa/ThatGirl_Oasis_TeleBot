package ru.nikidzawa.datingapp.TelegramBot.stateMachines.commands;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.nikidzawa.datingapp.TelegramBot.botFunctions.BotFunctions;
import ru.nikidzawa.datingapp.TelegramBot.helpers.Messages;
import ru.nikidzawa.datingapp.TelegramBot.services.DataBaseService;
import ru.nikidzawa.datingapp.TelegramBot.stateMachines.states.StateMachine;
import ru.nikidzawa.datingapp.store.entities.complain.ComplainEntity;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.store.repositories.ComplaintRepository;
import ru.nikidzawa.datingapp.store.repositories.LikeRepository;
import ru.nikidzawa.datingapp.store.repositories.UserRepository;

import java.util.HashMap;
import java.util.Optional;

@Component
public class CommandStateMachine {

    HashMap<String, CommandState> commands;

    @Autowired
    StateMachine stateMachine;

    @Autowired
    Messages messages;

    @Autowired
    UserRepository userRepository;

    @Autowired
    DataBaseService dataBaseService;

    @Autowired
    ComplaintRepository complaintRepository;

    @Setter
    public BotFunctions botFunctions;

    public CommandStateMachine() {
        commands = new HashMap<>();
        commands.put("/menu", new Menu());
        commands.put("/analysis", new Analysis());
        commands.put("/complains", new Complains());
    }

    public void handleInput(long userId, Message message, String status, Optional<UserEntity> optionalUser) {
        String messageText = message.getText();
        CommandState commandState = commands.get(messageText);
        if (commandState != null) {
            commandState.handleInput(userId, message, status, optionalUser);
        }
    }

    private class Menu implements CommandState {
        @Override
        public void handleInput(long userId, Message message, String role, Optional<UserEntity> optionalUser) {
            optionalUser.ifPresentOrElse(userEntity -> {
                stateMachine.goToMenu(userId, userEntity);
                if (!userEntity.isActive()) {
                    userEntity.setActive(true);
                    dataBaseService.saveUser(userEntity);
                }
            }, () -> botFunctions.sendMessageAndRemoveMarkup(userId, messages.getNOT_GROUP_MEMBER_EXCEPTION()));
        }
    }

    private class Analysis implements CommandState {
        @Override
        public void handleInput(long userId, Message message, String role, Optional<UserEntity> optionalUser) {
            if (role.equals("creator") || role.equals("admin")) {
                botFunctions.sendMessageAndRemoveMarkup(userId, "Идёт анализ, пожалуйста, подождите...");
                String[] results = userRepository.findTop10CitiesByUserCount();
                Long size = userRepository.countActiveAndNotBannedUsers();
                StringBuilder stringBuilder = new StringBuilder();

                stringBuilder.append("Рейтинг топ 10 популярных городов:").append("\n");
                for (int i = 0; i < results.length; i++) {
                    String[] cityAndNumber = results[i].split(",");
                    String city = cityAndNumber[0];
                    String count = cityAndNumber[1];
                    stringBuilder.append(i + 1).append(". ").append(city).append(" - ").append(count).append(" ").append(wordParser(count)).append("\n");
                }

                stringBuilder.append("\n").append("Число активных пользователей: ").append(size);

                botFunctions.sendMessageNotRemoveMarkup(userId, stringBuilder.toString());
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

    private class Complains implements CommandState {
        @Override
        public void handleInput(long userId, Message message, String role, Optional<UserEntity> optionalUser) {
            if (role.equals("creator") || role.equals("admin")) {
                Optional<ComplainEntity> optionalComplain = complaintRepository.findAnyComplain();
                optionalComplain.ifPresentOrElse(complainEntity -> {
                    botFunctions.sendMessageNotRemoveMarkup(userId, "Жалоба номер: " + complainEntity.getId());
                    UserEntity complainUser = complainEntity.getComplaintUser();
                    botFunctions.sendOtherProfileNotButtons(userId, complainUser);
                    botFunctions.sendMessageNotRemoveMarkup(userId, "Описание жалобы: " + complainEntity.getDescription());
                    complaintRepository.delete(complainEntity);
                }, () -> botFunctions.sendMessageNotRemoveMarkup(userId, "Больше жалоб не поступало"));
            }
        }
    }
}