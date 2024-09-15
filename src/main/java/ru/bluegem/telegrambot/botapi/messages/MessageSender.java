package ru.bluegem.telegrambot.botapi.messages;

import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.bluegem.telegrambot.botapi.markups.InlineMarkupBuilder;

import javax.validation.constraints.NotNull;
import java.sql.ResultSet;

public class MessageSender {

    private final DefaultAbsSender telegramBot;

    public MessageSender(@NotNull DefaultAbsSender telegramBot) {
        if (telegramBot == null) throw new NullPointerException("telegrambot is marked non-null but is null");
        this.telegramBot = telegramBot;
    }

    public void sendMessage(@NotNull String messageText, @NotNull long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(messageText);
        sendMessage.setChatId(chatId);

        try { telegramBot.execute(sendMessage); }
        catch (TelegramApiException exception) { exception.printStackTrace(); }
    }

    public void sendMessage(@NotNull String messageText, String parseMode, @NotNull long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(messageText);
        sendMessage.setParseMode(parseMode);
        sendMessage.setChatId(chatId);

        try { telegramBot.execute(sendMessage); }
        catch (TelegramApiException exception) { exception.printStackTrace(); }
    }

    public void sendMessage(@NotNull String messageText, ReplyKeyboard replyKeyboard, @NotNull long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(messageText);
        sendMessage.setReplyMarkup(replyKeyboard);
        sendMessage.setChatId(chatId);

        try { telegramBot.execute(sendMessage); }
        catch (TelegramApiException exception) { exception.printStackTrace(); }
    }

    public void sendMessage(@NotNull String messageText, ReplyKeyboard replyKeyboard, String parseMode, @NotNull long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(messageText);
        sendMessage.setReplyMarkup(replyKeyboard);
        sendMessage.setParseMode(parseMode);
        sendMessage.setChatId(chatId);

        try { telegramBot.execute(sendMessage); }
        catch (TelegramApiException exception) { exception.printStackTrace(); }
    }

    public void deleteMessage(@NotNull long chatId, @NotNull int messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(messageId);

        try { telegramBot.execute(deleteMessage); }
        catch (TelegramApiException exception) { exception.printStackTrace(); }
    }

    public void editMessageText(@NotNull String messageText, InlineKeyboardMarkup inlineMarkup, @NotNull long chatId,
                                @NotNull int messageId) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setText(messageText);
        editMessageText.setReplyMarkup(inlineMarkup);
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId);

        try { telegramBot.execute(editMessageText); }
        catch (TelegramApiException exception) { exception.printStackTrace(); }
    }

    public void editMessageText(@NotNull String messageText, InlineKeyboardMarkup inlineMarkup, String parseMode,
                                @NotNull long chatId, @NotNull int messageId) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setText(messageText);
        editMessageText.setReplyMarkup(inlineMarkup);
        editMessageText.setParseMode(parseMode);
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId);

        try { telegramBot.execute(editMessageText); }
        catch (TelegramApiException exception) { exception.printStackTrace(); }
    }

    public void sendVideo(InputFile video, long chatId) {
        SendVideo sendVideo = new SendVideo();
        sendVideo.setVideo(video);
        sendVideo.setChatId(chatId);

        try { telegramBot.execute(sendVideo); }
        catch (TelegramApiException exception) { exception.printStackTrace(); }
    }

    public void sendVideo(InputFile video, String caption, long chatId) {
        SendVideo sendVideo = new SendVideo();
        sendVideo.setVideo(video);
        sendVideo.setChatId(chatId);
        sendVideo.setCaption(caption);
        sendVideo.setParseMode(ParseMode.HTML);

        try { telegramBot.execute(sendVideo); }
        catch (TelegramApiException exception) { exception.printStackTrace(); }
    }

    public void sendVideo(InputFile video, String caption, ReplyKeyboard replyKeyboard, long chatId) {
        SendVideo sendVideo = new SendVideo();
        sendVideo.setVideo(video);
        sendVideo.setChatId(chatId);
        sendVideo.setCaption(caption);
        sendVideo.setParseMode(ParseMode.HTML);
        sendVideo.setReplyMarkup(replyKeyboard);

        try { telegramBot.execute(sendVideo); }
        catch (TelegramApiException exception) { exception.printStackTrace(); }
    }

    public void sendPhoto(InputFile photo, long chatId) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setPhoto(photo);
        sendPhoto.setChatId(chatId);

        try { telegramBot.execute(sendPhoto); }
        catch (TelegramApiException exception) { exception.printStackTrace(); }
    }

    public void sendPhoto(InputFile photo, String caption, long chatId) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setPhoto(photo);
        sendPhoto.setChatId(chatId);
        sendPhoto.setCaption(caption);
        sendPhoto.setParseMode(ParseMode.HTML);

        try { telegramBot.execute(sendPhoto); }
        catch (TelegramApiException exception) { exception.printStackTrace(); }
    }

    public void sendPhoto(InputFile photo, String caption, ReplyKeyboard replyKeyboard, long chatId) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setPhoto(photo);
        sendPhoto.setChatId(chatId);
        sendPhoto.setCaption(caption);
        sendPhoto.setParseMode(ParseMode.HTML);
        sendPhoto.setReplyMarkup(replyKeyboard);

        try { telegramBot.execute(sendPhoto); }
        catch (TelegramApiException exception) { exception.printStackTrace(); }
    }

    public void sendAnimation(InputFile gif, long chatId) {
        SendAnimation sendAnimation = new SendAnimation();
        sendAnimation.setAnimation(gif);
        sendAnimation.setChatId(chatId);

        try { telegramBot.execute(sendAnimation); }
        catch (TelegramApiException exception) { exception.printStackTrace(); }
    }

    public void sendAnimation(InputFile gif, String caption, long chatId) {
        SendAnimation sendAnimation = new SendAnimation();
        sendAnimation.setAnimation(gif);
        sendAnimation.setChatId(chatId);
        sendAnimation.setCaption(caption);
        sendAnimation.setParseMode(ParseMode.HTML);

        try { telegramBot.execute(sendAnimation); }
        catch (TelegramApiException exception) { exception.printStackTrace(); }
    }

    public void sendAnimation(InputFile gif, String caption, ReplyKeyboard replyKeyboard, long chatId) {
        SendAnimation sendAnimation = new SendAnimation();
        sendAnimation.setAnimation(gif);
        sendAnimation.setChatId(chatId);
        sendAnimation.setCaption(caption);
        sendAnimation.setParseMode(ParseMode.HTML);
        sendAnimation.setReplyMarkup(replyKeyboard);

        try { telegramBot.execute(sendAnimation); }
        catch (TelegramApiException exception) { exception.printStackTrace(); }
    }

//    public void sendContestEditMessage(String contestName, String contestId, long chatId) {
//        this.sendMessage("Редактор конкурса " + contestName, new InlineMarkupBuilder()
//                .addRow().addButton("Название: " + contestName, "Null")
//                .addButton("ID конкурса: " + contestId, "Null")
//                .addRow().addButton("Разыграть", "КонкурсРазыграть " + contestId)
//                .addRow().addButton("Пост в группу", "КонкурсПост " + contestId)
//                .addRow().addButton("Участники", "КонкурсУчастники " + contestId)
//                .addRow().addButton("Удалить", "УдалитьКонкурс1 " + contestId)
//                .build(), chatId);
//    }
//
//    public void sendContestsListMessage(String contestName, String contestId, long chatId, int contestNum) {
//        this.sendMessage("Конкурс " + contestName,
//                new InlineMarkupBuilder().addRow().addButton("Название: " + contestName, "Null")
//                        .addButton("ID конкурса: " + contestId, "Null")
//                        .addRow().addButton("Разыграть", "КонкурсРазыграть " + contestId)
//                        .addRow().addButton("Пост в группу", "КонкурсПост " + contestId)
//                        .addRow().addButton("Участники", "КонкурсУчастники " + contestId)
//                        .addRow().addButton("Удалить", "УдалитьКонкурс1 " + contestId)
//                        .addRow().addButton("⬅️", "Дальше " + (contestNum-1))
//                        .addButton("➡️", "Дальше " + (contestNum+1)).build(), chatId);
//    }

}
