package ru.nikidzawa.datingapp.TelegramBot;

import lombok.SneakyThrows;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.nikidzawa.datingapp.entities.UserEntity;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BotFunctions {

    private final TelegramBot telegramBot;

    public BotFunctions (TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    @SneakyThrows
    public void sendMessageAndRemoveMarkup(Long id, String message){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(id));
        sendMessage.setText(message);
        sendMessage.setReplyMarkup(new ReplyKeyboardRemove(true));
        telegramBot.execute(sendMessage);
    }

    @SneakyThrows
    public void sendMessageNotRemoveMarkup (Long id, String message){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(id));
        sendMessage.setText(message);
        telegramBot.execute(sendMessage);
    }

    @SneakyThrows
    public void sendMessageAndMarkup(Long id, String message, ReplyKeyboardMarkup replyKeyboardMarkup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(id));
        sendMessage.setText(message);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        telegramBot.execute(sendMessage);
    }

    @SneakyThrows
    public void sendLocation (long userId) {
        SendLocation sendLocationRequest = new SendLocation();
        sendLocationRequest.setChatId(String.valueOf(userId));
        sendLocationRequest.setLatitude(0.0);
        sendLocationRequest.setLongitude(0.0);
        telegramBot.execute(sendLocationRequest);
    }

    public ReplyKeyboardMarkup menuButtons() {return keyboardMarkupBuilder(List.of("1", "2", "3"));}
    public ReplyKeyboardMarkup resultButtons() {return keyboardMarkupBuilder(List.of("Заполнить анкету заново", "Продолжить"));}
    public ReplyKeyboardMarkup skipButton() {
        return keyboardMarkupBuilder(List.of("Пропустить"));
    }
    public ReplyKeyboardMarkup startButton() {return keyboardMarkupBuilder(List.of("Начнём!"));}
    public ReplyKeyboardMarkup editProfileButtons() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add("БИО");
        firstRow.add("Хобби, о себе");
        firstRow.add("Город");
        KeyboardRow secondRow = new KeyboardRow();
        secondRow.add("Фото");
        KeyboardRow thirdRow = new KeyboardRow();
        thirdRow.add("Изменить анкету полностью");
        thirdRow.add("Вернуться в меню");
        keyboardRows.add(firstRow);
        keyboardRows.add(secondRow);
        keyboardRows.add(thirdRow);
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;
    }
    public ReplyKeyboardMarkup askBeforeOffButtons() {return keyboardMarkupBuilder(List.of("Выключить анкету", "Я передумала"));}
    public ReplyKeyboardMarkup editResultButtons() {return keyboardMarkupBuilder(List.of("Сохранить", "Отменить"));}
    public ReplyKeyboardMarkup customButton(String button) {return keyboardMarkupBuilder(List.of(button));}
    public ReplyKeyboardMarkup customLocationButtons(String button) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(button);
        keyboardRows.add(firstRow);

        KeyboardRow secondRow = new KeyboardRow();
        KeyboardButton locationButton = new KeyboardButton();
        locationButton.setRequestLocation(true);
        locationButton.setText("\uD83D\uDCCDОтправить мою геолокацию");
        secondRow.add(locationButton);
        keyboardRows.add(secondRow);

        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;
    }
    public ReplyKeyboardMarkup locationButton() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow secondRow = new KeyboardRow();
        KeyboardButton locationButton = new KeyboardButton();
        locationButton.setRequestLocation(true);
        locationButton.setText("\uD83D\uDCCDОтправить мою геолокацию");
        secondRow.add(locationButton);
        keyboardRows.add(secondRow);

        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;}
    public ReplyKeyboardMarkup skipAndCustomButtons(String button) {return keyboardMarkupBuilder(List.of(button, "Пропустить"));}
    public ReplyKeyboardMarkup removeAndCustomButtons(String button) {return keyboardMarkupBuilder(List.of(button, "Убрать"));}
    public ReplyKeyboardMarkup welcomeBackButton() {return keyboardMarkupBuilder(List.of("Включить анкету"));}


    private ReplyKeyboardMarkup keyboardMarkupBuilder(List<String> buttonLabels) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow keyboardRow = new KeyboardRow();
        for (String label : buttonLabels) {
            keyboardRow.add(label);
        }
        keyboardRows.add(keyboardRow);
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;
    }

    public String loadPhoto (List<PhotoSize> photos) {
        String fileId = photos.stream().max(Comparator.comparing(PhotoSize::getFileSize))
                .orElse(null).getFileId();
        return fileId;
    }

    @SneakyThrows
    public void sendDatingSiteProfile (Long userId, UserEntity userEntity) {
        String fileId = userEntity.getPhoto();
        GetFile getFile = new GetFile(fileId);
        File file = telegramBot.execute(getFile);
        String filePath = file.getFilePath();
        URL fileUrl = new URL("https://api.telegram.org/file/bot" + telegramBot.getBotToken() + "/" + filePath);
        InputStream inputStream = fileUrl.openStream();
        InputFile inputFile = new InputFile(inputStream, "photo.jpg");
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(userId);
        sendPhoto.setPhoto(inputFile);
        String hobby = userEntity.getHobby();
        String aboutMe = userEntity.getAboutMe();
        String profileInfo = userEntity.getName() + ", " + userEntity.getAge() + ", " + userEntity.getLocation() +
                (hobby == null ? "" : "\nМои хобби:" + parseHobby(hobby)) + (aboutMe == null ? "" : (hobby == null ? "\n" : "\n\n") + aboutMe);
        sendPhoto.setCaption(profileInfo);
        telegramBot.execute(sendPhoto);
    }

    private String parseHobby(String allHobby) {
        StringBuilder stringBuilder = new StringBuilder();
        String[] hobbyArray = allHobby.split(",");
        for (String hobby : hobbyArray) {
            String trimmedHobby = hobby.trim();
            if (!trimmedHobby.isEmpty()) {
                char firstChar = Character.toUpperCase(trimmedHobby.charAt(0));
                stringBuilder.append("\n").append("● ").append(firstChar).append(trimmedHobby.substring(1));
            }
        }
        return stringBuilder.toString();
    }
}