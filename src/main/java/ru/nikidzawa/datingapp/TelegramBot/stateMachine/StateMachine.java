package ru.nikidzawa.datingapp.TelegramBot.stateMachine;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.nikidzawa.datingapp.TelegramBot.BotFunctions;
import ru.nikidzawa.datingapp.TelegramBot.cache.CacheService;
import ru.nikidzawa.datingapp.TelegramBot.helpers.Messages;
import ru.nikidzawa.datingapp.entities.UserEntity;

import java.util.HashMap;

@Component
public class StateMachine {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private Messages messages;

    private final HashMap<StateEnum, State> states = new HashMap<>();

    @Setter
    public BotFunctions botFunctions;


    public StateMachine() {
        states.put(StateEnum.START, new StartState());
        states.put(StateEnum.CHECK_GROUP_MEMBER, new CheckGroupMember());
        states.put(StateEnum.LEFT, new Left());

        states.put(StateEnum.ASK_NAME, new AskName());
        states.put(StateEnum.ASK_AGE, new AskAge());
        states.put(StateEnum.ASK_CITY, new AskCity());
        states.put(StateEnum.ASK_HOBBY, new AskHobby());
        states.put(StateEnum.ASK_SPEND_TIME, new AskSpendTime());
        states.put(StateEnum.ASK_ABOUT_ME, new AskAboutMe());
        states.put(StateEnum.ASK_PHOTO, new AskPhoto());
        states.put(StateEnum.RESULT, new Result());
    }

    public void handleInput (StateEnum currentState, Long userId, UserEntity userEntity, Message messageText) {
        State state = states.get(currentState);
        state.handleInput(userId, userEntity, messageText);
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
            } else {
                botFunctions.sendMessage(userId,
                        "Привет, " + message.getFrom().getFirstName() + "\n" +
                                "Я рада, что ты вернулась в наше сообщество! \uD83D\uDC96\n" +
                                "\n" +
                                "Здесь ты найдешь не только подруг, но и множество возможностей для личного роста и творческого самовыражения на наших мероприятиях.\n");
            }
            cacheService.setState(userId, StateEnum.ASK_NAME);
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
            botFunctions.sendMessage(userId, "Нам очень жаль, что тебе у нас не понравилось. Ждём тебя в другой раз!");
            cacheService.evictState(userId);
        }
    }

    private class AskName implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            botFunctions.sendMessage(userId, "Как тебя зовут?");
            cacheService.setState(userId, StateEnum.ASK_CITY);
        }
    }

    private class AskCity implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            botFunctions.sendMessage(userId, "Где ты живёшь?");
            cacheService.setState(userId, StateEnum.ASK_AGE);
        }
    }

    private class AskAge implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            botFunctions.sendMessage(userId, "Сколько тебе лет?");
            cacheService.setState(userId, StateEnum.ASK_HOBBY);
        }
    }
    private class AskHobby implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            botFunctions.sendMessage(userId, "Назови свои хобби");
            cacheService.setState(userId, StateEnum.ASK_SPEND_TIME);
        }
    }

    private class AskSpendTime implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            botFunctions.sendMessage(userId, "Как ты любишь проводить время?");
            cacheService.setState(userId, StateEnum.ASK_ABOUT_ME);
        }
    }

    private class AskAboutMe implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            botFunctions.sendMessage(userId, "Можешь дополнительно рассказать о себе, или о том, кого хочешь найти");
            cacheService.setState(userId, StateEnum.ASK_PHOTO);
        }
    }

    private class AskPhoto implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            botFunctions.sendMessage(userId, "Ну и последнее, давай загрузим тебе фото!");
            cacheService.setState(userId, StateEnum.RESULT);
        }
    }

    private class Result implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message) {
            botFunctions.sendMessage(userId, "третий стейт");
        }
    }
}
