package ru.nikidzawa.datingapp.TelegramBot.stateMachine;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.nikidzawa.datingapp.TelegramBot.BotFunctions;
import ru.nikidzawa.datingapp.TelegramBot.cache.CacheService;
import ru.nikidzawa.datingapp.TelegramBot.helpers.Messages;
import ru.nikidzawa.datingapp.TelegramBot.services.DataBaseService;
import ru.nikidzawa.datingapp.TelegramBot.services.parser.Geocode;
import ru.nikidzawa.datingapp.TelegramBot.services.parser.JsonParser;
import ru.nikidzawa.datingapp.entities.UserEntity;

import java.util.HashMap;
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

    private final HashMap<StateEnum, State> textStates;
    private final HashMap<StateEnum, State> photoStates;
    private final HashMap<StateEnum, State> locationStates;

    @Setter
    public BotFunctions botFunctions;


    public StateMachine() {
        textStates = new HashMap<>();
        photoStates = new HashMap<>();
        locationStates = new HashMap<>();

        textStates.put(StateEnum.START, new StartState());
        textStates.put(StateEnum.WELCOME_BACK, new WelcomeBack());

        textStates.put(StateEnum.CHECK_GROUP_MEMBER, new CheckGroupMember());
        textStates.put(StateEnum.LEFT, new Left());

        textStates.put(StateEnum.PRE_REGISTER, new PreRegister());
        textStates.put(StateEnum.ASK_NAME, new AskName());
        textStates.put(StateEnum.ASK_AGE, new AskAge());
        textStates.put(StateEnum.ASK_CITY, new AskCity());
        textStates.put(StateEnum.ASK_HOBBY, new AskHobby());
        textStates.put(StateEnum.ASK_ABOUT_ME, new AskAboutMe());
        textStates.put(StateEnum.RESULT, new Result());

        textStates.put(StateEnum.MENU, new Menu());

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

        photoStates.put(StateEnum.ASK_PHOTO, new AskPhoto());
        photoStates.put(StateEnum.EDIT_PHOTO, new EditPhoto());

        locationStates.put(StateEnum.ASK_CITY, new AskCityGeo());
        locationStates.put(StateEnum.EDIT_CITY, new EditCityGeo());
    }

    public void handleInput(StateEnum currentState, Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
        State state = null;
        if (message.hasText() || message.isSuperGroupMessage()) {state = textStates.get(currentState);}
        else if (message.hasPhoto()) {
            state = photoStates.get(currentState);
        }
        else if (message.hasLocation()) {
            state = locationStates.get(currentState);
        }

        if (state != null) {
            state.handleInput(userId, userEntity, message, hasBeenRegistered);
        } else {
            botFunctions.sendMessageNotRemoveMarkup(userId, messages.getINVALID_FORMAT_EXCEPTION());
        }
    }

    private class StartState implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageAndRemoveMarkup(userId,
                    "Привет, " + message.getFrom().getFirstName() + "\n" +
                            "Я рада, что ты присоединилась к нашему сообществу \uD83D\uDC96\n" +
                            "\n" +
                            "Здесь ты найдешь не только подруг, но и множество возможностей для личного роста и творческого самовыражения на наших мероприятиях.\n" +
                            "Давай вместе создадим яркие и запоминающиеся моменты!");
            botFunctions.sendMessageAndMarkup(userId, "Давай заполним тебе анкету?", botFunctions.startButton());
            cacheService.setState(userId, StateEnum.PRE_REGISTER);

        }
    }

    private class CheckGroupMember implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageAndRemoveMarkup(userId, "Для продолжения, необходимо вступить в нашу группу t.me/nikidzawa_group");
        }
    }

    private class Left implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            botFunctions.sendMessageAndRemoveMarkup(userId, messages.getLEFT());
            cacheService.evictState(userId);
        }
    }

    // РЕГИСТРАЦИЯ
    private class PreRegister implements State {
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
            if (message.getText().equals("Включить анкету")) {
                botFunctions.sendMessageAndMarkup(userId, messages.getMENU(), botFunctions.menuButtons());
                userEntity.setActive(true);
                dataBaseService.saveUser(userEntity);
                cacheService.setState(userId, StateEnum.MENU);
            }
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
                Geocode coordinates = jsonParser.getGeocode(response(messageText).getBody());
                user.setLongitude(coordinates.getLon());
                user.setLatitude(coordinates.getLat());
                cacheService.putCachedUser(userId, user);
            }).start();
        }
        private ResponseEntity<String> response (String goeObjectName) {
            String url = "https://nominatim.openstreetmap.org/search?format=json&q=" + goeObjectName + "&addressdetails=1&limit=1&featureType=city";
            RestTemplate restTemplate = new RestTemplate();
            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    String.class
            );
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
                cachedUser.setLocation(jsonParser.getName(response(latitude, longitude).getBody()));

                cacheService.putCachedUser(userId, cachedUser);
            }).start();
        }
        private ResponseEntity<String> response (double latitude, double longitude) {
            String url = "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=" + latitude + "&longitude=" + longitude + "&localityLanguage=ru";
            RestTemplate restTemplate = new RestTemplate();
            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    String.class
            );
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
                if (messageText.equals("Оставить текущее описание") && hasBeenRegistered) {
                    user.setAboutMe(userEntity.getAboutMe());
                } else {
                    user.setAboutMe(message.getText());
                }
                cacheService.putCachedUser(userId, user);
            }
            if (hasBeenRegistered) {
                botFunctions.sendMessageAndMarkup(userId, messages.getASK_PHOTO(), botFunctions.customButton("Оставить текущие фотографии"));
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
                botFunctions.sendDatingSiteProfile(userId, user);
                cacheService.setState(userId, StateEnum.EDIT_RESULT);
            } else {
                botFunctions.sendMessageAndRemoveMarkup(userId, messages.getRESULT());
                botFunctions.sendDatingSiteProfile(userId, user);
                botFunctions.sendMessageAndMarkup(userId, "Всё верно?", botFunctions.resultButtons());
                cacheService.setState(userId, StateEnum.RESULT);
            }
        }
    }
    private class SkipAskPhoto implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            if (message.getText().equals("Оставить текущие фотографии") && hasBeenRegistered) {
                UserEntity user = cacheService.getCachedUser(userId);
                user.setPhoto(userEntity.getPhoto());
                cacheService.putCachedUser(userId, user);
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_RESULT(), botFunctions.editResultButtons());
                botFunctions.sendDatingSiteProfile(userId, user);
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
                botFunctions.sendMessageAndMarkup(userId, messages.getMENU(), botFunctions.menuButtons());
                cacheService.setState(userId, StateEnum.MENU);
                UserEntity user = cacheService.getCachedUser(userId);
                user.setActive(true);
                dataBaseService.saveUser(user);
                cacheService.evictCachedUser(userId);
            }
        }
    }
    private class Menu implements State {
        @Override
        public void handleInput(Long userId, UserEntity userEntity, Message message, boolean hasBeenRegistered) {
            String messageText = message.getText();
            switch (messageText) {
                case "1" -> {

                }
                case "2" -> {
                    botFunctions.sendDatingSiteProfile(userId, userEntity);
                    botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_PROFILE(), botFunctions.editProfileButtons());
                    cacheService.setState(userId, StateEnum.EDIT_PROFILE);
                }
                case "3" -> {
                    botFunctions.sendMessageAndMarkup(userId, messages.getASK_BEFORE_OFF(), botFunctions.askBeforeOffButtons());
                    cacheService.setState(userId, StateEnum.ASK_BEFORE_OFF);
                }
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
                botFunctions.sendMessageAndMarkup(userId, messages.getMENU(), botFunctions.menuButtons());
                cacheService.setState(userId, StateEnum.MENU);
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
                botFunctions.sendMessageAndMarkup(userId, messages.getMENU(), botFunctions.menuButtons());
                cacheService.setState(userId, StateEnum.MENU);
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
                botFunctions.sendDatingSiteProfile(userId, user);
                cacheService.setState(userId, StateEnum.EDIT_RESULT);
            } else {
                botFunctions.sendMessageAndRemoveMarkup(userId, messages.getNULL_DATA_EDIT());
                botFunctions.sendDatingSiteProfile(userId, userEntity);
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
                Geocode geocode = jsonParser.getGeocode(response(messageText).getBody());
                userEntity.setLongitude(geocode.getLon());
                userEntity.setLatitude(geocode.getLat());
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_RESULT(), botFunctions.editResultButtons());
                botFunctions.sendDatingSiteProfile(userId, userEntity);
                cacheService.putCachedUser(userId, userEntity);
                cacheService.setState(userId, StateEnum.EDIT_RESULT);
            } else {
                botFunctions.sendMessageAndRemoveMarkup(userId, messages.getNULL_DATA_EDIT());
                botFunctions.sendDatingSiteProfile(userId, userEntity);
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_PROFILE(), botFunctions.editProfileButtons());
                cacheService.setState(userId, StateEnum.EDIT_PROFILE);
            }
        }
        private ResponseEntity<String> response (String geoObjectName) {
            String url = "https://nominatim.openstreetmap.org/search?format=json&q=" + geoObjectName + "&addressdetails=1&limit=1&featureType=city";
            RestTemplate restTemplate = new RestTemplate();
            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    String.class
            );
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
            userEntity.setLocation(jsonParser.getName(response(latitude, longitude).getBody()));

            botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_RESULT(), botFunctions.editResultButtons());
            botFunctions.sendDatingSiteProfile(userId, userEntity);
            cacheService.putCachedUser(userId, userEntity);
            cacheService.setState(userId, StateEnum.EDIT_RESULT);
        }
        private ResponseEntity<String> response (double latitude, double longitude) {
            String url = "https://nominatim.openstreetmap.org/search?format=json&q=" + latitude + "," + longitude + "&addressdetails=1&limit=1&featureType=city";
            RestTemplate restTemplate = new RestTemplate();
            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    String.class
            );
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
            cacheService.putCachedUser(userId, cachedUser);
            if (userEntity.getAboutMe() == null) {
                botFunctions.sendMessageAndMarkup(userId, messages.getASK_ABOUT_ME(), botFunctions.skipButton());
            } else {
                botFunctions.sendMessageAndMarkup(userId, messages.getASK_ABOUT_ME(), botFunctions.removeAndCustomButtons(messages.getUNEDITED_ABOUT_ME()));
            }
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
                botFunctions.sendDatingSiteProfile(userId, cachedUser);
                cacheService.putCachedUser(userId, cachedUser);
                cacheService.setState(userId, StateEnum.EDIT_RESULT);
            } else {
                botFunctions.sendMessageAndRemoveMarkup(userId, messages.getNULL_DATA_EDIT());
                botFunctions.sendDatingSiteProfile(userId, cachedUser);
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
                botFunctions.sendDatingSiteProfile(userId, userEntity);
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
            botFunctions.sendDatingSiteProfile(userId, userEntity);
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
                botFunctions.sendDatingSiteProfile(userId, user);
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_PROFILE(), botFunctions.editProfileButtons());
                cacheService.setState(userId, StateEnum.EDIT_PROFILE);
                cacheService.evictCachedUser(userId);
                dataBaseService.saveUser(user);
            }
            else if (messageText.equals("Отменить")) {
                botFunctions.sendDatingSiteProfile(userId, userEntity);
                botFunctions.sendMessageAndMarkup(userId, messages.getEDIT_PROFILE(), botFunctions.editProfileButtons());
                cacheService.setState(userId, StateEnum.EDIT_PROFILE);
                cacheService.evictCachedUser(userId);
            }
        }
    }
}
