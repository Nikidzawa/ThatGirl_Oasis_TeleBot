package ru.nikidzawa.datingapp.telegramBot.stateMachines.states;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.nikidzawa.datingapp.store.entities.complain.ComplainEntity;
import ru.nikidzawa.datingapp.store.entities.error.ErrorEntity;
import ru.nikidzawa.datingapp.store.entities.like.LikeContentType;
import ru.nikidzawa.datingapp.store.entities.like.LikeEntity;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;
import ru.nikidzawa.datingapp.telegramBot.botFunctions.BotFunctions;
import ru.nikidzawa.datingapp.telegramBot.cache.CacheService;
import ru.nikidzawa.datingapp.telegramBot.helpers.Messages;
import ru.nikidzawa.datingapp.telegramBot.services.DataBaseService;
import ru.nikidzawa.datingapp.telegramBot.services.api.GeocodingApi;
import ru.nikidzawa.datingapp.telegramBot.services.parsers.Geocode;
import ru.nikidzawa.datingapp.telegramBot.services.parsers.JsonParser;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Component
public class StateMachine {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private DataBaseService dataBaseService;

    @Autowired
    private Messages messages;

    @Autowired
    private JsonParser jsonParser;

    @Autowired
    private GeocodingApi geocodingApi;

    private final HashMap<StateEnum, State> textStates;
    private final HashMap<StateEnum, State> photoStates;
    private final HashMap<StateEnum, State> locationStates;
    private final HashMap<StateEnum, State> audioStates;
    private final HashMap<StateEnum, State> videoStates;
    private final HashMap<StateEnum, State> videoNoteStates;

    @Setter
    public BotFunctions botFunctions;


    public StateMachine() {
        textStates = new HashMap<>();
        photoStates = new HashMap<>();
        locationStates = new HashMap<>();
        audioStates = new HashMap<>();
        videoStates = new HashMap<>();
        videoNoteStates = new HashMap<>();

        textStates.put(StateEnum.START, new Start());
        textStates.put(StateEnum.START_HANDLE, new StartHandle());
        textStates.put(StateEnum.WELCOME_BACK, new WelcomeBack());
        textStates.put(StateEnum.WELCOME_BACK_HANDLE, new WelcomeBackHandle());

        textStates.put(StateEnum.LEFT, new Left());

        textStates.put(StateEnum.ASK_NAME, new AskName());
        textStates.put(StateEnum.ASK_AGE, new AskAge());
        textStates.put(StateEnum.ASK_CITY, new AskCity());
        textStates.put(StateEnum.ASK_HOBBY, new AskHobby());
        textStates.put(StateEnum.ASK_ABOUT_ME, new AskAboutMe());
        textStates.put(StateEnum.RESULT, new Result());

        textStates.put(StateEnum.MENU, new Menu());
        textStates.put(StateEnum.SUPER_MENU, new SuperMenu());

        textStates.put(StateEnum.EDIT_NAME, new EditName());
        textStates.put(StateEnum.EDIT_AGE, new EditAge());
        textStates.put(StateEnum.EDIT_CITY, new EditCity());
        textStates.put(StateEnum.EDIT_RESULT, new EditResult());
        textStates.put(StateEnum.EDIT_PROFILE, new EditProfile());
        textStates.put(StateEnum.EDIT_HOBBY, new EditHobby());
        textStates.put(StateEnum.EDIT_ABOUT_ME, new EditAboutMe());
        textStates.put(StateEnum.ASK_BEFORE_OFF, new AskBeforeOff());
        textStates.put(StateEnum.EDIT_PHOTO, new SkipEditPhoto());
        textStates.put(StateEnum.ASK_PHOTO, new SkipAskPhoto());
        textStates.put(StateEnum.FIND_PEOPLES, new FindPeoples());
        textStates.put(StateEnum.SHOW_WHO_LIKED_ME, new ShowWhoLikedMe());
        textStates.put(StateEnum.SHOW_PROFILES_WHO_LIKED_ME, new ShowProfilesWhoLikedMe());
        textStates.put(StateEnum.STOP_SHOW_PROFILES_WHO_LIKED_ME, new StopShowProfilesWhoLikedMe());
        textStates.put(StateEnum.SEND_LIKE_AND_MESSAGE, new SendLikeAndMessageText());
        textStates.put(StateEnum.CALL_BACK_QUERY_COMPLAIN, new CallbackQueryComplain());
        textStates.put(StateEnum.FAQ, new FAQ());
        textStates.put(StateEnum.FAQ_RESPONSE, new FaqResponse());
        textStates.put(StateEnum.SEND_ERROR, new SendError());

        photoStates.put(StateEnum.ASK_PHOTO, new AskPhoto());
        photoStates.put(StateEnum.EDIT_PHOTO, new EditPhoto());
        photoStates.put(StateEnum.SEND_LIKE_AND_MESSAGE, new SendLikeAndMessagePhoto());

        locationStates.put(StateEnum.ASK_CITY, new AskCityGeo());
        locationStates.put(StateEnum.EDIT_CITY, new EditCityGeo());

        audioStates.put(StateEnum.SEND_LIKE_AND_MESSAGE, new SendLikeAndMessageAudio());
        videoStates.put(StateEnum.SEND_LIKE_AND_MESSAGE, new SendLikeAndMessageVideo());
        videoNoteStates.put(StateEnum.SEND_LIKE_AND_MESSAGE, new SendLikeAndMessageVideoNote());
    }

    public void handleInput(StateEnum currentState, Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
        State state = null;
        if (message.hasText() || message.isSuperGroupMessage()) {state = textStates.get(currentState);}
        else if (message.hasPhoto()) {
            state = photoStates.get(currentState);
        } else if (message.hasLocation()) {
            state = locationStates.get(currentState);
        } else if (message.hasVoice()) {
            state = audioStates.get(currentState);
        } else if (message.hasVideo()) {
            state = videoStates.get(currentState);
        } else if (message.hasVideoNote()) {
            state = videoNoteStates.get(currentState);
        }

        if (state != null) {
            state.handleInput(userId, userEntity, message, hasBeenRegistered);
        } else {
            botFunctions.sendMessageNotRemoveMarkup(userId, messages.getINVALID_FORMAT_EXCEPTION());
        }
    }

    private class Start implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageAndRemoveMarkup(userId,
                    "Привет, " + message.getFrom().getFirstName() + "\n" +
                            "Я рада, что ты присоединилась к нашему сообществу \uD83D\uDC96\n" +
                            "\n" +
                            "Здесь ты найдешь не только подруг, но и множество возможностей для личного роста и творческого самовыражения на наших мероприятиях.\n" +
                            "Давай вместе создадим яркие и запоминающиеся моменты!");
            botFunctions.sendMessageAndMarkup(userId, "Давай заполним тебе анкету?", botFunctions.startButton());
            cacheService.setState(userId, StateEnum.START_HANDLE);

        }
    }

    private class StartHandle implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            if (message.getText().equals("Начнём!")) {
                cacheService.setState(userId, StateEnum.ASK_NAME);
                cacheService.putCachedUser(userId, new UserEntity(userId));
                botFunctions.sendMessageAndRemoveMarkup(userId, messages.getASK_NAME());
            }
        }
    }

    private class WelcomeBack implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageAndMarkup(userId,
                    "Привет, " + message.getFrom().getFirstName() + "\n" +
                            "Я рада, что ты вернулась в наше сообщество! \uD83D\uDC96\n" +
                            "\n" +
                            "Давай включим тебе анкету?\n", botFunctions.welcomeBackButton());
            cacheService.setState(userId, StateEnum.WELCOME_BACK_HANDLE);
        }
    }

    private class WelcomeBackHandle implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            if (message.getText().equals("Включить анкету")) {
                goToMenu(userId, userEntity);
                userEntity.setActive(true);
                dataBaseService.saveUser(userEntity);
            }
        }
    }

    private class Left implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageAndMarkup(userId, messages.getLEFT(), botFunctions.restartButton());
            dataBaseService.getUserById(userId).ifPresent(user -> {
                user.setActive(false);
                dataBaseService.saveUser(user);
            });
            cacheService.evictState(userId);
        }
    }

    private class AskName implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.length() >= 100) {
                botFunctions.sendMessageNotRemoveMarkup(userId, messages.getNAME_LIMIT_SYMBOLS_EXCEPTIONS());
                return;
            }

            if (hasBeenRegistered) {
                botFunctions.sendMessageAndMarkup(userId, messages.getASK_CITY(), botFunctions.customLocationButtons(userEntity.getLocation()));
            }
            else {
                botFunctions.sendMessageAndMarkup(userId, messages.getASK_CITY(), botFunctions.locationButton());
            }

            new Thread(() -> {
                cacheService.setState(userId, StateEnum.ASK_CITY);
                UserEntity user = cacheService.getCachedUser(userId);
                user.setName(message.getText());
                cacheService.putCachedUser(userId, user);
            }).start();
        }
    }

    private class AskCity implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.length() >= 100) {
                botFunctions.sendMessageNotRemoveMarkup(userId, messages.getCITY_LIMIT_SYMBOLS_EXCEPTIONS());
                return;
            }

            if (hasBeenRegistered) {
                botFunctions.sendMessageAndMarkup(userId, messages.getASK_AGE(), botFunctions.customButton(String.valueOf(userEntity.getAge())));
            } else {
                botFunctions.sendMessageAndRemoveMarkup(userId, messages.getASK_AGE());
            }
            cacheService.setState(userId, StateEnum.ASK_AGE);

            new Thread(() -> {
                UserEntity user = cacheService.getCachedUser(userId);
                user.setLocation(messageText);
                Geocode coordinates = jsonParser.parseGeocode(geocodingApi.getCoordinates(messageText));
                user.setLongitude(coordinates.getLon());
                user.setLatitude(coordinates.getLat());
                cacheService.putCachedUser(userId, user);
            }).start();
        }
    }
    private class AskCityGeo implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            if (hasBeenRegistered) {
                botFunctions.sendMessageAndMarkup(userId, messages.getASK_AGE(), botFunctions.customButton(String.valueOf(userEntity.getAge())));
            } else {
                botFunctions.sendMessageAndRemoveMarkup(userId, messages.getASK_AGE());
            }
            cacheService.setState(userId, StateEnum.ASK_AGE);

            new Thread(() -> {
                UserEntity cachedUser = cacheService.getCachedUser(userId);
                Location location = message.getLocation();
                double longitude = location.getLongitude();
                double latitude = location.getLatitude();

                cachedUser.setLongitude(longitude);
                cachedUser.setLatitude(latitude);
                cachedUser.setLocation(jsonParser.getName(geocodingApi.getCityName(latitude, longitude)));

                cacheService.putCachedUser(userId, cachedUser);
            }).start();
        }
    }

    private class AskAge implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            UserEntity user = cacheService.getCachedUser(userId);
            int age;
            try {
                age = Integer.parseInt(message.getText());
                if (age >= 100 || age < 6) {
                    botFunctions.sendMessageNotRemoveMarkup(userId, messages.getAGE_LIMIT_SYMBOLS_EXCEPTIONS());
                    return;
                }
            } catch (Exception ex) {
                botFunctions.sendMessageAndRemoveMarkup(userId, messages.getIS_NOT_A_NUMBER_EXCEPTION());
                return;
            }
            user.setAge(age);
            cacheService.putCachedUser(userId, user);
            if (hasBeenRegistered && userEntity.getHobby() != null) {
                botFunctions.sendMessageAndMarkup(userId, messages.getASK_HOBBY(), botFunctions.skipAndCustomButtons(messages.getUNEDITED_HOBBY()));
            } else {
                botFunctions.sendMessageAndMarkup(userId, messages.getASK_HOBBY(), botFunctions.skipButton());
            }
            cacheService.setState(userId, StateEnum.ASK_HOBBY);
        }
    }
    private class AskHobby implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.length() >= 150) {
                botFunctions.sendMessageNotRemoveMarkup(userId, messages.getASK_HOBBY());
                return;
            }
            if (!messageText.equals("Пропустить")) {
                UserEntity user = cacheService.getCachedUser(userId);
                if (messageText.equals("Оставить текущие хобби") && userEntity != null) {
                    user.setHobby(userEntity.getHobby());
                } else {
                    user.setHobby(messageText);
                }
                cacheService.putCachedUser(userId, user);
            }
            if (hasBeenRegistered && userEntity.getAboutMe() != null) {
                botFunctions.sendMessageAndMarkup(userId, messages.getASK_ABOUT_ME(), botFunctions.skipAndCustomButtons(messages.getUNEDITED_ABOUT_ME()));
            } else {
                botFunctions.sendMessageAndMarkup(userId, messages.getASK_ABOUT_ME(), botFunctions.skipButton());
            }
            cacheService.setState(userId, StateEnum.ASK_ABOUT_ME);
        }
    }

    private class AskAboutMe implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.length() >= 1000) {
                botFunctions.sendMessageNotRemoveMarkup(userId, messages.getABOUT_ME_LIMIT_SYMBOLS_EXCEPTIONS());
                return;
            }
            if (!messageText.equals("Пропустить")) {
                UserEntity user = cacheService.getCachedUser(userId);
                if (messageText.equals("Оставить текущую информацию") && hasBeenRegistered) {
                    user.setAboutMe(userEntity.getAboutMe());
                } else {
                    user.setAboutMe(message.getText());
                }
                cacheService.putCachedUser(userId, user);
            }
            if (hasBeenRegistered) {
                botFunctions.sendMessageAndMarkup(userId, messages.getASK_PHOTO(), botFunctions.customButton(messages.getUNEDITED_PHOTO()));
            } else {
                botFunctions.sendMessageAndRemoveMarkup(userId, messages.getASK_PHOTO());
            }
            cacheService.setState(userId, StateEnum.ASK_PHOTO);
        }
    }

    private class AskPhoto implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            UserEntity user = cacheService.getCachedUser(userId);
            user.setPhoto(botFunctions.loadPhoto(message.getPhoto()));
            cacheService.putCachedUser(userId, user);

            if (hasBeenRegistered) {
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_RESULT(), botFunctions.editResultButtons());
                botFunctions.sendDatingProfile(userId, user);
                cacheService.setState(userId, StateEnum.EDIT_RESULT);
            } else {
                botFunctions.sendMessageAndRemoveMarkup(userId, messages.getRESULT());
                botFunctions.sendDatingProfile(userId, user);
                botFunctions.sendMessageAndMarkup(userId, "Всё верно?", botFunctions.resultButtons());
                cacheService.setState(userId, StateEnum.RESULT);
            }
        }
    }
    private class SkipAskPhoto implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            if (message.getText().equals(messages.getUNEDITED_PHOTO()) && hasBeenRegistered) {
                UserEntity user = cacheService.getCachedUser(userId);
                user.setPhoto(userEntity.getPhoto());
                cacheService.putCachedUser(userId, user);
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_RESULT(), botFunctions.editResultButtons());
                botFunctions.sendDatingProfile(userId, user);
                cacheService.setState(userId, StateEnum.EDIT_RESULT);
            } else {
                botFunctions.sendMessageNotRemoveMarkup(userId, messages.getINVALID_FORMAT_EXCEPTION());
            }
        }
    }

    private class Result implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            if (message.getText().equals("Заполнить анкету заново")) {
                cacheService.setState(userId, StateEnum.ASK_NAME);
                cacheService.putCachedUser(userId, new UserEntity(userId));
                botFunctions.sendMessageAndRemoveMarkup(userId, messages.getASK_NAME());
            } else if (message.getText().equals("Продолжить")) {
                UserEntity user = cacheService.getCachedUser(userId);
                goToMenu(userId, user);
                new Thread(() -> {
                    user.setActive(true);
                    dataBaseService.saveUser(user);
                    cacheService.evictCachedUser(userId);
                }).start();
            }
        }
    }
    private class Menu implements State {

        HashMap<String, State> responses;

        public Menu() {
            responses = new HashMap<>();
            responses.put("1", new FindPeoples());
            responses.put("2", new EditProfile());
            responses.put("3", new OffProfile());
        }

        private class FindPeoples implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                UserEntity anotherUser = getRecommendation(userEntity, userId).getFirst();
                botFunctions.sendOtherProfile(userId, anotherUser, userEntity, botFunctions.searchButtons());
                cacheService.setState(userId, StateEnum.FIND_PEOPLES);
            }
        }

        private class EditProfile implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendDatingProfile(userId, userEntity);
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_PROFILE(), botFunctions.editProfileButtons());
                cacheService.setState(userId, StateEnum.EDIT_PROFILE);
            }
        }

        private class OffProfile implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndMarkup(userId, messages.getASK_BEFORE_OFF(), botFunctions.askBeforeOffButtons());
                cacheService.setState(userId, StateEnum.ASK_BEFORE_OFF);
            }
        }
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            State state = responses.get(messageText);
            if (state != null) {
                state.handleInput(userId, userEntity, message, hasBeenRegistered);
            }
        }
    }

    private class SuperMenu implements State {
        public HashMap<String, State> superMenuStates;

        public SuperMenu () {
            superMenuStates = new HashMap<>();
            superMenuStates.put("1", new ShowWhoLikedMe());
            superMenuStates.put("2", new FindPeople());
            superMenuStates.put("3", new MyProfile());
            superMenuStates.put("4", new OffProfile());
        }

        private class ShowWhoLikedMe implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                likeChecker(userId, userEntity);
            }
        }

        private class FindPeople implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                UserEntity anotherUser = getRecommendation(userEntity, userId).getFirst();
                botFunctions.sendOtherProfile(userId, anotherUser, userEntity, botFunctions.searchButtons());
                cacheService.setState(userId, StateEnum.FIND_PEOPLES);
            }
        }

        private class MyProfile implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendDatingProfile(userId, userEntity);
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_PROFILE(), botFunctions.editProfileButtons());
                cacheService.setState(userId, StateEnum.EDIT_PROFILE);
            }
        }

        private class OffProfile implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndMarkup(userId, messages.getASK_BEFORE_OFF(), botFunctions.askBeforeOffButtons());
                cacheService.setState(userId, StateEnum.ASK_BEFORE_OFF);
            }
        }
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            State state = superMenuStates.get(messageText);
            if (state != null) {
                state.handleInput(userId, userEntity, message, hasBeenRegistered);
            }
        }
    }
    private class AskBeforeOff implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.equals("Выключить анкету")) {
                botFunctions.sendMessageAndRemoveMarkup(userId, messages.getLEFT());
                userEntity.setActive(false);
                dataBaseService.saveUser(userEntity);
                cacheService.evictState(userId);
            } else if (messageText.equals("Я передумала")) {
                goToMenu(userId, userEntity);
            }
        }
    }

    private class EditProfile implements State {
        final HashMap<String, State> answers;
        public EditProfile () {
            answers = new HashMap<>();
            answers.put("БИО", new BIO());
            answers.put("Хобби, о себе", new Hobby());
            answers.put("Город", new City());
            answers.put("Фото", new Photo());
            answers.put("Изменить анкету полностью", new FullEdit());
            answers.put("Вернуться в меню", new Menu());
        }
        public class BIO implements State {

            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_NAME(), botFunctions.skipButton());
                cacheService.setState(userId, StateEnum.EDIT_NAME);
                cacheService.putCachedUser(userId, userEntity);
            }
        }

        public class Hobby implements State {

            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                if (userEntity.getHobby() == null) {
                    botFunctions.sendMessageAndMarkup(userId, messages.getASK_HOBBY(), botFunctions.skipButton());
                } else {
                    botFunctions.sendMessageAndMarkup(userId, messages.getASK_HOBBY(), botFunctions.removeAndCustomButtons(messages.getUNEDITED_HOBBY()));
                }
                cacheService.setState(userId, StateEnum.EDIT_HOBBY);
                cacheService.putCachedUser(userId, userEntity);
            }
        }

        public class City implements State {

            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_CITY(), botFunctions.customLocationButtons(userEntity.getLocation()));
                cacheService.setState(userId, StateEnum.EDIT_CITY);
            }
        }

        public class Photo implements State {

            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_PHOTO(), botFunctions.customButton(messages.getUNEDITED_PHOTO()));
                cacheService.setState(userId, StateEnum.EDIT_PHOTO);
            }
        }

        public class FullEdit implements State {

            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                cacheService.setState(userId, StateEnum.ASK_NAME);
                cacheService.putCachedUser(userId, new UserEntity(userId));
                botFunctions.sendMessageAndMarkup(userId, messages.getASK_NAME(), botFunctions.customButton(userEntity.getName()));
            }
        }

        public class Menu implements State {

            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                goToMenu(userId, userEntity);
            }
        }
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            State state = answers.get(messageText);
            if (state != null) {
                state.handleInput(userId, userEntity, message, hasBeenRegistered);
            }
        }
    }

    private class EditName implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (!messageText.equals("Пропустить")) {
                if (messageText.length() >= 100) {
                    botFunctions.sendMessageNotRemoveMarkup(userId, messages.getNAME_LIMIT_SYMBOLS_EXCEPTIONS());
                    return;
                }
                UserEntity user = cacheService.getCachedUser(userId);
                user.setName(messageText);
                cacheService.putCachedUser(userId, user);
            }
            botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_AGE(), botFunctions.customButton(String.valueOf(userEntity.getAge())));
            cacheService.setState(userId, StateEnum.EDIT_AGE);
        }
    }

    private class EditAge implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            UserEntity user = cacheService.getCachedUser(userId);
            if (!messageText.equals(String.valueOf(userEntity.getAge()))) {
                int age;
                try {
                    age = Integer.parseInt(message.getText());
                    if (age >= 100 || age <= 6) {
                        botFunctions.sendMessageNotRemoveMarkup(userId, messages.getAGE_LIMIT_SYMBOLS_EXCEPTIONS());
                        return;
                    }
                    user.setAge(age);
                } catch (Exception ex) {
                    botFunctions.sendMessageNotRemoveMarkup(userId, messages.getIS_NOT_A_NUMBER_EXCEPTION());
                    return;
                }
            }
            if (!user.getName().equals(userEntity.getName()) || user.getAge() != userEntity.getAge()) {
                cacheService.putCachedUser(userId, user);
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_RESULT(), botFunctions.editResultButtons());
                botFunctions.sendDatingProfile(userId, user);
                cacheService.setState(userId, StateEnum.EDIT_RESULT);
            } else {
                botFunctions.sendMessageAndRemoveMarkup(userId, messages.getNULL_DATA_EDIT());
                botFunctions.sendDatingProfile(userId, userEntity);
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_PROFILE(), botFunctions.editProfileButtons());
                cacheService.setState(userId, StateEnum.EDIT_PROFILE);
                cacheService.evictCachedUser(userId);
            }
        }
    }
    private class EditCity implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (!messageText.equals(userEntity.getLocation())) {
                if (messageText.length() >= 100) {
                    botFunctions.sendMessageNotRemoveMarkup(userId, messages.getCITY_LIMIT_SYMBOLS_EXCEPTIONS());
                    return;
                }
                userEntity.setLocation(messageText);
                Geocode geocode = jsonParser.parseGeocode(geocodingApi.getCoordinates(messageText));
                userEntity.setLongitude(geocode.getLon());
                userEntity.setLatitude(geocode.getLat());
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_RESULT(), botFunctions.editResultButtons());
                botFunctions.sendDatingProfile(userId, userEntity);
                cacheService.putCachedUser(userId, userEntity);
                cacheService.setState(userId, StateEnum.EDIT_RESULT);
            } else {
                botFunctions.sendMessageAndRemoveMarkup(userId, messages.getNULL_DATA_EDIT());
                botFunctions.sendDatingProfile(userId, userEntity);
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_PROFILE(), botFunctions.editProfileButtons());
                cacheService.setState(userId, StateEnum.EDIT_PROFILE);
            }
        }
    }
    private class EditCityGeo implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            Location location = message.getLocation();
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();

            userEntity.setLongitude(longitude);
            userEntity.setLatitude(latitude);
            userEntity.setLocation(jsonParser.getName(geocodingApi.getCityName(latitude, longitude)));
            userEntity.setShowGeo(true);

            botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_RESULT(), botFunctions.editResultButtons());
            botFunctions.sendDatingProfile(userId, userEntity);
            cacheService.putCachedUser(userId, userEntity);
            cacheService.setState(userId, StateEnum.EDIT_RESULT);
        }
    }

    private class EditHobby implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            UserEntity cachedUser = cacheService.getCachedUser(userId);
            if (!messageText.equals(messages.getUNEDITED_HOBBY()) && !messageText.equals("Пропустить")) {
                if (messageText.length() >= 150) {
                    botFunctions.sendMessageNotRemoveMarkup(userId, messages.getHOBBY_LIMIT_SYMBOLS_EXCEPTIONS());
                    return;
                }
                if (messageText.equals("Убрать")) {
                    cachedUser.setHobby(null);
                } else {
                    cachedUser.setHobby(messageText);
                }
            }
            if (userEntity.getAboutMe() == null) {
                botFunctions.sendMessageAndMarkup(userId, messages.getASK_ABOUT_ME(), botFunctions.skipButton());
            } else {
                botFunctions.sendMessageAndMarkup(userId, messages.getASK_ABOUT_ME(), botFunctions.removeAndCustomButtons(messages.getUNEDITED_ABOUT_ME()));
            }
            cacheService.putCachedUser(userId, cachedUser);
            cacheService.setState(userId, StateEnum.EDIT_ABOUT_ME);
        }
    }
    private class EditAboutMe implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            UserEntity cachedUser = cacheService.getCachedUser(userId);
            if (!messageText.equals(messages.getUNEDITED_ABOUT_ME()) && !messageText.equals("Пропустить")) {
                if (messageText.length() >= 1000) {
                    botFunctions.sendMessageNotRemoveMarkup(userId, messages.getABOUT_ME_LIMIT_SYMBOLS_EXCEPTIONS());
                    return;
                }
                if (messageText.equals("Убрать")) {
                    cachedUser.setAboutMe(null);
                } else {
                    cachedUser.setAboutMe(messageText);
                }
            }
            if (!Objects.equals(cachedUser.getHobby(), userEntity.getHobby()) || !Objects.equals(cachedUser.getAboutMe(), userEntity.getAboutMe())) {
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_RESULT(), botFunctions.editResultButtons());
                botFunctions.sendDatingProfile(userId, cachedUser);
                cacheService.putCachedUser(userId, cachedUser);
                cacheService.setState(userId, StateEnum.EDIT_RESULT);
            } else {
                botFunctions.sendMessageAndRemoveMarkup(userId, messages.getNULL_DATA_EDIT());
                botFunctions.sendDatingProfile(userId, cachedUser);
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_PROFILE(), botFunctions.editProfileButtons());
                cacheService.setState(userId, StateEnum.EDIT_PROFILE);
                cacheService.evictCachedUser(userId);
            }
        }
    }
    private class SkipEditPhoto implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.equals("Оставить текущее фото")) {
                botFunctions.sendMessageAndRemoveMarkup(userId, messages.getNULL_DATA_EDIT());
                botFunctions.sendDatingProfile(userId, userEntity);
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_PROFILE(), botFunctions.editProfileButtons());
                cacheService.setState(userId, StateEnum.EDIT_PROFILE);
            } else {
                botFunctions.sendMessageNotRemoveMarkup(userId, messages.getINVALID_FORMAT_EXCEPTION());
            }
        }
    }
    private class EditPhoto implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            userEntity.setPhoto(botFunctions.loadPhoto(message.getPhoto()));
            botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_RESULT(), botFunctions.editResultButtons());
            botFunctions.sendDatingProfile(userId, userEntity);
            cacheService.putCachedUser(userId, userEntity);
            cacheService.setState(userId, StateEnum.EDIT_RESULT);
        }
    }

    private class EditResult implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.equals("Сохранить")) {
                UserEntity user = cacheService.getCachedUser(userId);
                botFunctions.sendDatingProfile(userId, user);
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_PROFILE(), botFunctions.editProfileButtons());
                cacheService.setState(userId, StateEnum.EDIT_PROFILE);
                cacheService.evictCachedUser(userId);
                dataBaseService.saveUser(user);
            }
            else if (messageText.equals("Отменить")) {
                botFunctions.sendDatingProfile(userId, userEntity);
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_PROFILE(), botFunctions.editProfileButtons());
                cacheService.setState(userId, StateEnum.EDIT_PROFILE);
                cacheService.evictCachedUser(userId);
            }
        }
    }

    private class ShowWhoLikedMe implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.equals("Посмотреть")) {
                likeChecker(userId, userEntity);
            }
            else if (messageText.equals("В другой раз")) {
                goToMenu(userId, userEntity);
            }
        }
    }

    private class SendLikeAndMessagePhoto implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageAndRemoveMarkup(userId, "Сообщение отправлено");
            List<UserEntity> profiles = getRecommendation(userEntity, userId);
            UserEntity anotherUser = profiles.getFirst();
            String fileId = botFunctions.loadPhoto(message.getPhoto());
            new Thread(() -> sendLike(userEntity, anotherUser, false, LikeContentType.PHOTO, fileId)).start();
            cacheService.evictCachedProfiles(userId, anotherUser, profiles);
            botFunctions.sendOtherProfile(userId, getRecommendation(userEntity, userId).getFirst(), userEntity, botFunctions.reciprocityButtons());
            cacheService.setState(userId, StateEnum.FIND_PEOPLES);
        }
    }

    private class SendLikeAndMessageAudio implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageAndRemoveMarkup(userId, "Сообщение отправлено");
            List<UserEntity> profiles = getRecommendation(userEntity, userId);
            UserEntity anotherUser = profiles.getFirst();
            String fileId = message.getVoice().getFileId();
            new Thread(() -> sendLike(userEntity, anotherUser, false, LikeContentType.VOICE, fileId)).start();
            cacheService.evictCachedProfiles(userId, anotherUser, profiles);
            botFunctions.sendOtherProfile(userId, getRecommendation(userEntity, userId).getFirst(), userEntity, botFunctions.reciprocityButtons());
            cacheService.setState(userId, StateEnum.FIND_PEOPLES);
        }
    }

    private class SendLikeAndMessageVideo implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageAndRemoveMarkup(userId, "Сообщение отправлено");
            List<UserEntity> profiles = getRecommendation(userEntity, userId);
            UserEntity anotherUser = profiles.getFirst();
            String fileId = message.getVideo().getFileId();
            new Thread(() -> sendLike(userEntity, anotherUser, false, LikeContentType.VIDEO, fileId)).start();
            cacheService.evictCachedProfiles(userId, anotherUser, profiles);
            botFunctions.sendOtherProfile(userId, getRecommendation(userEntity, userId).getFirst(), userEntity, botFunctions.reciprocityButtons());
            cacheService.setState(userId, StateEnum.FIND_PEOPLES);
        }
    }

    private class SendLikeAndMessageText implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            List<UserEntity> profiles = getRecommendation(userEntity, userId);
            UserEntity anotherUser = profiles.getFirst();
            if (!messageText.equals("Отменить")) {
                botFunctions.sendMessageAndRemoveMarkup(userId, "Сообщение отправлено");
                cacheService.evictCachedProfiles(userId, anotherUser, profiles);
                botFunctions.sendOtherProfile(userId, getRecommendation(userEntity, userId).getFirst(), userEntity, botFunctions.searchButtons());
                new Thread(() -> sendLike(userEntity, anotherUser, false, LikeContentType.TEXT, messageText)).start();
            } else {
                botFunctions.sendOtherProfile(userId, anotherUser, userEntity, botFunctions.searchButtons());
            }
            cacheService.setState(userId, StateEnum.FIND_PEOPLES);
        }
    }

    private class SendLikeAndMessageVideoNote implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageAndRemoveMarkup(userId, "Сообщение отправлено");
            List<UserEntity> profiles = getRecommendation(userEntity, userId);
            UserEntity anotherUser = profiles.getFirst();
            String fileId = message.getVideoNote().getFileId();
            new Thread(() -> sendLike(userEntity, anotherUser, false, LikeContentType.VIDEO_NOTE, fileId)).start();
            cacheService.evictCachedProfiles(userId, anotherUser, profiles);
            botFunctions.sendOtherProfile(userId, getRecommendation(userEntity, userId).getFirst(), userEntity, botFunctions.searchButtons());
            cacheService.setState(userId, StateEnum.FIND_PEOPLES);
        }
    }

    private class ShowProfilesWhoLikedMe implements State {
        HashMap<String, State> response;
        public ShowProfilesWhoLikedMe () {
            response = new HashMap<>();
            response.put("❤", new SendReciprocity());
            response.put("\uD83D\uDC4E", new SendDislike());
            response.put("\uD83D\uDCA4", new GoToMenu());
        }

        private class SendReciprocity implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                UserEntity likedUser = removeLike(userEntity);
                String userName = botFunctions.getChatMember(likedUser.getId()).getUser().getUserName();
                botFunctions.sendMessageAndComplainButton(userId, likedUser.getId(), "Желаю вам хорошо провести время :)\nhttps://t.me/" + userName);
                likeChecker(userId, userEntity);
                new Thread(() -> sendLike(userEntity, likedUser, true, null, null)).start();
            }
        }

        private class SendDislike implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                removeLike(userEntity);
                likeChecker(userId, userEntity);
            }
        }

        private class GoToMenu implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                goToMenu(userId, userEntity);
            }
        }

        private UserEntity removeLike (UserEntity userEntity) {
            List<LikeEntity> likeEntityList = userEntity.getLikesGiven();
            LikeEntity like = likeEntityList.getFirst();
            UserEntity likedUser = dataBaseService.getUserById(like.getLikerUserId()).get();
            likeEntityList.remove(like);
            dataBaseService.saveUser(userEntity);
            dataBaseService.deleteLike(like.getId());
            return likedUser;
        }

        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            State state = response.get(message.getText());
            if (state != null) {
                state.handleInput(userId, userEntity, message, hasBeenRegistered);
            }
        }
    }

    private class StopShowProfilesWhoLikedMe implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.equals("Продолжить смотреть анкеты")) {
                UserEntity anotherUser = getRecommendation(userEntity, userId).getFirst();
                botFunctions.sendOtherProfile(userId, anotherUser, userEntity, botFunctions.reciprocityButtons());
                cacheService.setState(userId, StateEnum.FIND_PEOPLES);
            }
            else if (messageText.equals("Вернуться в меню")) {
                goToMenu(userId, userEntity);
            }
        }
    }

    private class FindPeoples implements State {

        HashMap<String, State> response;

        public FindPeoples() {
            response = new HashMap<>();
            response.put("❤", new SendLike());
            response.put("\uD83D\uDC8C", new SendLikeAndMessage());
            response.put("\uD83D\uDC4E", new SendDislike());
            response.put("\uD83D\uDCA4", new GoToMenu());
        }

        private class SendLike implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                List<UserEntity> profiles = getRecommendation(userEntity, userId);
                UserEntity anotherUser = profiles.getFirst();

                botFunctions.sendMessageNotRemoveMarkup(userId, "Лайк отправлен, ждём ответа");
                new Thread(() -> sendLike(userEntity, anotherUser, false, null, null)).start();
                cacheService.evictCachedProfiles(userId, anotherUser, profiles);

                UserEntity nextUser = getRecommendation(userEntity, userId).getFirst();
                botFunctions.sendOtherProfile(userId, nextUser, userEntity, botFunctions.searchButtons());
            }
        }
        private class SendLikeAndMessage implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndMarkup(userId, "Можешь отправить сообщение, кружок, голосовое, фото или видео. Я пришлю его этому человеку", botFunctions.cancelButton());
                cacheService.setState(userId, StateEnum.SEND_LIKE_AND_MESSAGE);
            }
        }

        private class SendDislike implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                List<UserEntity> profiles = getRecommendation(userEntity, userId);
                UserEntity anotherUser = profiles.getFirst();

                cacheService.evictCachedProfiles(userId, anotherUser, profiles);

                UserEntity nextUser = getRecommendation(userEntity, userId).getFirst();
                botFunctions.sendOtherProfile(userId, nextUser, userEntity, botFunctions.searchButtons());
            }
        }

        private class GoToMenu implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                goToMenu(userId, userEntity);
            }
        }

        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            State state = response.get(messageText);
            if (state != null) {
                state.handleInput(userId, userEntity, message, hasBeenRegistered);
            }
        }
    }

    private class SendError implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageNotRemoveMarkup(userId, "Благодарим за сотрудничество! Мы обязательно рассмотрим вашу проблему");
            goToMenu(userId, userEntity);
            new Thread(() -> {
                ErrorEntity errorEntity = ErrorEntity.builder()
                        .errorSenderId(userId)
                        .description(message.getText())
                        .build();
                dataBaseService.saveError(errorEntity);
            }).start();
        }
    }

    private class FaqResponse implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            if (messageText.equals("Назад")) {
                botFunctions.sendMessageAndMarkup(userId, messages.getFAQ(), botFunctions.faqButtons());
                cacheService.setState(userId, StateEnum.FAQ);
            } else if (messageText.equals("Вернуться в меню")) {
                goToMenu(userId, userEntity);
            }
        }
    }

    private class FAQ implements State {

        private final HashMap<String, State> response;

        public FAQ () {
            response = new HashMap<>();
            response.put("1", new FAQ1());
            response.put("2", new FAQ2());
            response.put("Вернуться в меню", new GoToMenu());
        }

        private class FAQ1 implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndMarkup(userId, messages.getFAQ_1(), botFunctions.faqResponseButtons());
                cacheService.setState(userId, StateEnum.FAQ_RESPONSE);
            }
        }

        private class FAQ2 implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                botFunctions.sendMessageAndMarkup(userId, messages.getFAQ_2(), botFunctions.faqResponseButtons());
                cacheService.setState(userId, StateEnum.FAQ_RESPONSE);
            }
        }

        private class GoToMenu implements State {
            @Override
            public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
                goToMenu(userId, userEntity);
            }
        }

        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            State state = response.get(message.getText());
            if (state != null) {
                state.handleInput(userId, userEntity, message, hasBeenRegistered);
            }
        }
    }

    private class CallbackQueryComplain implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            Long complaintUserId = cacheService.getComplaintUserId(userId);

            if (!messageText.equals("Отменить")) {
                UserEntity complaintUser = dataBaseService.getUserById(complaintUserId).get();
                ComplainEntity complainEntity = ComplainEntity.builder()
                        .complainSenderId(userId)
                        .description(message.getText())
                        .complaintUser(complaintUser)
                        .build();
                dataBaseService.saveComplain(complainEntity);
                botFunctions.sendMessageAndRemoveMarkup(userId, "Жалоба отправлена, мы её внимательно изучим");
            }
            goToMenu(userId, userEntity);
            cacheService.evictComplaintUser(userId);
        }
    }

    public void likeChecker (Long userId, UserEntity myProfile) {
        List<LikeEntity> likeList = myProfile.getLikesGiven();
        if (likeList.isEmpty()) {
            botFunctions.sendMessageAndMarkup(userId, "На этом всё, продолжить просмотр анкет?", botFunctions.stopShowProfilesWhoLikedMeButtons());
            cacheService.setState(userId, StateEnum.STOP_SHOW_PROFILES_WHO_LIKED_ME);
        } else {
            LikeEntity like = likeList.getFirst();
            if (like.isReciprocity()) {
                UserEntity likedUser = dataBaseService.getUserById(like.getLikerUserId()).get();
                botFunctions.sendMessageNotRemoveMarkup(userId, "Есть взаимная симпатия!");
                botFunctions.sendOtherProfile(userId, likedUser, myProfile, null);
                String userName = botFunctions.getChatMember(likedUser.getId()).getUser().getUserName();
                botFunctions.sendMessageAndComplainButton(userId, likedUser.getId(), "Желаю вам хорошо провести время :)\nhttps://t.me/" + userName);
                likeList.remove(like);
                dataBaseService.saveUser(myProfile);
                dataBaseService.deleteLike(like.getId());
                likeChecker(userId, myProfile);
            } else {
                showWhoLikedMe(userId, myProfile, like);
            }
        }
    }

    public void goToMenu (Long userId, UserEntity userEntity) {
        int likedMeCount = userEntity.getLikesGiven().size();
        if (likedMeCount == 0) {
            botFunctions.sendMessageAndMarkup(userId, messages.getMENU(), botFunctions.menuButtons());
            cacheService.setState(userId, StateEnum.MENU);
        } else {
            String likeCountText;
            if (likedMeCount == 1) {
                likeCountText = "1. Посмотреть, кому я понравилась\n";
            } else {
                likeCountText = "1. Твоя анкета понравилась " + likedMeCount + " людям, показать их?\n";
            }
            botFunctions.sendMessageAndMarkup(userId,
                    likeCountText +
                            "2. Начать поиск подруг ✨\n" +
                            "3. Моя анкета\n" +
                            "4. Выключить анкету",
                    botFunctions.superMenuButtons());
            cacheService.setState(userId, StateEnum.SUPER_MENU);
        }
    }

    public List<UserEntity> getRecommendation(UserEntity myProfile, Long userId) {
        List<UserEntity> profiles = cacheService.getCachedProfiles(userId);
        if (profiles == null || profiles.isEmpty()) {
            profiles = dataBaseService.getProfiles(myProfile);
            cacheService.putCachedProfiles(userId, profiles);
        }
        return profiles;
    }

    public void showWhoLikedMe (Long userId, UserEntity myProfile, LikeEntity like) {
        UserEntity anotherUser = dataBaseService.getUserById(like.getLikerUserId()).get();
        botFunctions.sendOtherProfile(userId, anotherUser, myProfile, botFunctions.reciprocityButtons());
        if (like.getLikeContentType() != null) {
            botFunctions.sendMessage.get(like.getLikeContentType()).handleInput(userId, like);
        }
        cacheService.setState(userId, StateEnum.SHOW_PROFILES_WHO_LIKED_ME);
    }

    public void sendLike(UserEntity myProfile, UserEntity anotherUser, boolean isReciprocity, LikeContentType likeContentType, String content) {
        if (!myProfile.isBanned()) {
            Long anotherUserId = anotherUser.getId();
            UserEntity realAnotherUser = dataBaseService.getUserById(anotherUserId).get();
            List<LikeEntity> likedUsers = realAnotherUser.getLikesGiven();
            if (likedUsers.stream().noneMatch(like -> like.getLikerUserId() == myProfile.getId())) {
                Cache.ValueWrapper optionalState = cacheService.getCurrentState(anotherUserId);
                if (optionalState == null || optionalState.get() == StateEnum.MENU) {
                    if (likedUsers.isEmpty()) {
                        botFunctions.sendMessageAndMarkup(anotherUserId, "твоя анкета кому-то понравилась", botFunctions.showWhoLikedMeButtons());
                    } else {
                        botFunctions.sendMessageAndMarkup(anotherUserId, "твоя анкета понравилась " + (likedUsers.size() + 1) + " людям", botFunctions.showWhoLikedMeButtons());
                    }
                    cacheService.setState(anotherUserId, StateEnum.SHOW_WHO_LIKED_ME);
                } else if (optionalState.get() == StateEnum.FIND_PEOPLES) {
                    botFunctions.sendMessageNotRemoveMarkup(anotherUserId, "Заканчивай с просмотром анкет, ты кому-то понравилась!");
                }
                LikeEntity like = dataBaseService.saveLike(
                        LikeEntity.builder()
                                .isReciprocity(isReciprocity)
                                .likeContentType(likeContentType)
                                .content(content)
                                .likedUser(realAnotherUser)
                                .likerUserId(myProfile.getId())
                                .build()
                );
                realAnotherUser.getLikesGiven().add(like);
                dataBaseService.saveUser(realAnotherUser);
            }
        }
    }
}
