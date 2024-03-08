package ru.nikidzawa.datingapp.TelegramBot;

import lombok.SneakyThrows;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
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
    public void sendMessage (Long id, String message){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(id));
        sendMessage.setText(message);
        sendMessage.setReplyMarkup(new ReplyKeyboardRemove(true));
        telegramBot.execute(sendMessage);
    }

    @SneakyThrows
    public void sendMessageMarkup (Long id, String message, List<String> buttonLabels) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(id));
        sendMessage.setText(message);
        sendMessage.setReplyMarkup(keyboardMarkupBuilder(buttonLabels));
        telegramBot.execute(sendMessage);
    }

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
        InputFile inputFile = new InputFile(inputStream, "фото.jpg");
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(userId);
        sendPhoto.setPhoto(inputFile);
        sendPhoto.setCaption(userEntity.getName() + ", " + userEntity.getAge() + ", " + userEntity.getCity() + "\n"
                + "Мои хобби: " + userEntity.getHobby() + "\n"
                + "В свободное время люблю: " + userEntity.getSpendYourTime() + "\n"
                + "О себе: " + userEntity.getAboutMe());
        telegramBot.execute(sendPhoto);
    }
}
