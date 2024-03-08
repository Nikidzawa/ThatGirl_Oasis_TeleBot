package ru.nikidzawa.datingapp.TelegramBot.stateMachine;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.nikidzawa.datingapp.TelegramBot.BotFunctions;
import ru.nikidzawa.datingapp.TelegramBot.DataBaseService;
import ru.nikidzawa.datingapp.TelegramBot.cache.CacheService;
import ru.nikidzawa.datingapp.TelegramBot.helpers.Messages;
import ru.nikidzawa.datingapp.entities.UserEntity;

import java.util.HashMap;
import java.util.List;

@Component
public class StateMachine {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private DataBaseService dataBaseService;

    @Autowired
    private Messages messages;

    private final HashMap<StateEnum, State> textStates = new HashMap<>();
    private final HashMap<StateEnum, State> photoStates = new HashMap<>();
    private final HashMap<StateEnum, State> exceptions = new HashMap<>();

    @Setter
    public BotFunctions botFunctions;


    public StateMachine() {
        textStates.put(StateEnum.START, new StartState());
        textStates.put(StateEnum.CHECK_GROUP_MEMBER, new CheckGroupMember());
        textStates.put(StateEnum.LEFT, new Left());

        textStates.put(StateEnum.PRE_REGISTER, new PreRegister());
        textStates.put(StateEnum.ASK_NAME, new AskName());
        textStates.put(StateEnum.ASK_AGE, new AskAge());
        textStates.put(StateEnum.ASK_CITY, new AskCity());
        textStates.put(StateEnum.ASK_HOBBY, new AskHobby());
        textStates.put(StateEnum.ASK_SPEND_TIME, new AskSpendTime());
        textStates.put(StateEnum.ASK_ABOUT_ME, new AskAboutMe());
        textStates.put(StateEnum.RESULT, new Result());

        photoStates.put(StateEnum.ASK_PHOTO, new AskPhoto());

        exceptions.put(StateEnum.FORMAT_EXCEPTION, new FormatException());
    }

    public void handleInput(StateEnum currentState, Long userId, UserEntity userEntity, Message message) {
        System.out.println(currentState.name());
        State state = null;
        if (message.hasText() || message.isSuperGroupMessage()) {state = textStates.get(currentState);}
        else if (message.hasPhoto()) {state = photoStates.get(currentState);}

        if (state != null) {
            state.handleInput(userId, userEntity, message);
        } else {
            State exception = exceptions.get(StateEnum.FORMAT_EXCEPTION);
            exception.handleInput(userId, userEntity, message);
        }
    }

    private class StartState implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            if (userEntity == null) {
                botFunctions.sendMessage(userId,
                        "Привет, " + message.getFrom().getFirstName() + "\n" +
                                "Я рада, что ты присоединилась к нашему сообществу \uD83D\uDC96\n" +
                                "\n" +
                                "Здесь ты найдешь не только подруг, но и множество возможностей для личного роста и творческого самовыражения на наших мероприятиях.\n" +
                                "Давай вместе создадим яркие и запоминающиеся моменты!");
                botFunctions.sendMessageMarkup(userId, "Давай заполним тебе анкету?", List.of("Начнём!"));
                cacheService.setState(userId, StateEnum.PRE_REGISTER);
            } else {
                botFunctions.sendMessage(userId,
                        "Привет, " + message.getFrom().getFirstName() + "\n" +
                                "Я рада, что ты вернулась в наше сообщество! \uD83D\uDC96\n" +
                                "\n" +
                                "Здесь ты найдешь не только подруг, но и множество возможностей для личного роста и творческого самовыражения на наших мероприятиях.\n");

            }
        }
    }

    private class CheckGroupMember implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            botFunctions.sendMessage(userId, "Для продолжения, необходимо вступить в нашу группу t.me/nikidzawa_group");
        }
    }

    private class Left implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            botFunctions.sendMessage(userId, messages.getLEFT());
            cacheService.evictState(userId);
        }
    }

    // РЕГИСТРАЦИЯ
    private class PreRegister implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            if (message.getText().equals("Начнём!")) {
                cacheService.setState(userId, StateEnum.ASK_NAME);
                cacheService.putEditUser(userId, new UserEntity(userId));
                botFunctions.sendMessage(userId, messages.getASK_NAME());
            }
        }
    }

    private class AskName implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            UserEntity user = cacheService.getEditUser(userId);
            user.setName(message.getText());
            cacheService.putEditUser(userId, user);

            botFunctions.sendMessage(userId, messages.getASK_CITY());
            cacheService.setState(userId, StateEnum.ASK_CITY);
        }
    }

    private class AskCity implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            UserEntity user = cacheService.getEditUser(userId);
            user.setCity(message.getText());
            cacheService.putEditUser(userId, user);

            botFunctions.sendMessage(userId, messages.getASK_AGE());
            cacheService.setState(userId, StateEnum.ASK_AGE);
        }
    }

    private class AskAge implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            UserEntity user = cacheService.getEditUser(userId);
            try {
                user.setAge(Integer.parseInt(message.getText()));
            } catch (Exception ex) {
                botFunctions.sendMessage(userId, messages.getIS_NOT_A_NUMBER_EXCEPTION());
                return;
            }
            cacheService.putEditUser(userId, user);

            botFunctions.sendMessage(userId, messages.getASK_HOBBY());
            cacheService.setState(userId, StateEnum.ASK_HOBBY);
        }
    }
    private class AskHobby implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            UserEntity user = cacheService.getEditUser(userId);
            user.setHobby(message.getText());
            cacheService.putEditUser(userId, user);

            botFunctions.sendMessage(userId, messages.getASK_SPEND_TIME());
            cacheService.setState(userId, StateEnum.ASK_SPEND_TIME);
        }
    }

    private class AskSpendTime implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            UserEntity user = cacheService.getEditUser(userId);
            user.setSpendYourTime(message.getText());
            cacheService.putEditUser(userId, user);

            botFunctions.sendMessageMarkup(userId, messages.getASK_ABOUT_ME(), List.of("Пропустить"));
            cacheService.setState(userId, StateEnum.ASK_ABOUT_ME);
        }
    }

    private class AskAboutMe implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            UserEntity user = cacheService.getEditUser(userId);
            if (!message.getText().equals("Пропустить")) {
                user.setAboutMe(message.getText());
            } else {user.setAboutMe("");}
            cacheService.putEditUser(userId, user);
            botFunctions.sendMessage(userId, messages.getASK_PHOTO());
            cacheService.setState(userId, StateEnum.ASK_PHOTO);
        }
    }

    private class AskPhoto implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            UserEntity user = cacheService.getEditUser(userId);
            user.setPhoto(botFunctions.loadPhoto(message.getPhoto()));
            cacheService.putEditUser(userId, user);

            botFunctions.sendMessage(userId, messages.getRESULT());
            botFunctions.sendDatingSiteProfile(userId, user);
            botFunctions.sendMessageMarkup(userId, "Всё верно?", List.of("Заполнить анкету заново", "Продолжить"));
            cacheService.setState(userId, StateEnum.RESULT);
        }
    }

    private class Result implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            if (message.getText().equals("Заполнить анкету заново")) {
                cacheService.setState(userId, StateEnum.ASK_NAME);
                cacheService.putEditUser(userId, new UserEntity(userId));
                botFunctions.sendMessage(userId, messages.getASK_NAME());
            } else if (message.getText().equals("Продолжить")) {
                UserEntity user = cacheService.getEditUser(userId);
                botFunctions.sendDatingSiteProfile(userId, user);
                dataBaseService.saveUser(user);
                cacheService.evictEditUser(userId);
            }
        }
    }

    private class FormatException implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            botFunctions.sendMessage(userId, messages.getINVALID_FORMAT_EXCEPTION());
        }
    }
}
