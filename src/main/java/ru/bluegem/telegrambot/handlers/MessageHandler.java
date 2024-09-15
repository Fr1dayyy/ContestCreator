package ru.bluegem.telegrambot.handlers;

import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.bluegem.telegrambot.containers.ContestObjects;
import ru.bluegem.telegrambot.sqlite.SQLiteConn;
import ru.bluegem.telegrambot.GiveawayBot;
import ru.bluegem.telegrambot.botapi.markups.InlineMarkupBuilder;
import ru.bluegem.telegrambot.botapi.markups.ReplyMarkupBuilder;
import ru.bluegem.telegrambot.botapi.messages.MessageSender;
import ru.bluegem.telegrambot.cache.DataManager;
import ru.bluegem.telegrambot.cache.UserData;
import ru.bluegem.telegrambot.enums.BotState;
import ru.bluegem.telegrambot.schedule.ScheduledContest;
import ru.bluegem.telegrambot.schedule.ScheduledPost;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

public class MessageHandler {

    private final GiveawayBot giveawayBot;

    private final MessageSender messageSender;
    private final SQLiteConn sqLiteConn;
    private final DataManager dataManager;

    private final ContestObjects contestObjects;

    public MessageHandler(GiveawayBot giveawayBot, MessageSender messageSender, SQLiteConn sqLiteConn, DataManager dataManager) {
        this.giveawayBot = giveawayBot;
        this.messageSender = messageSender;
        this.sqLiteConn = sqLiteConn;
        this.dataManager = dataManager;
        this.contestObjects = giveawayBot.getContestObjects();
    }

    public void handleMessage(Update update) throws SQLException, TelegramApiException {
        Message message = update.getMessage();
        long chatId = message.getChatId();

        sqLiteConn.addUser(chatId);

        String messageText = message.getText();

        if (messageText != null) {
            String[] parts = messageText.split(" ");
            if (messageText.startsWith("/start") && parts.length > 1) {
                parts = parts[1].split("-");
                String contestId = parts[0];
                if (parts.length == 1) {
                    ResultSet contest = sqLiteConn.getContest(contestId);
                    if (contest == null) {
                        messageSender.sendMessage("Конкурс не найден! Возможно он был удален", chatId);
                        return;
                    }
                    messageSender.sendMessage(contestObjects.getContestText(contest, -1), ParseMode.HTML, chatId);
                    return;
                } else if (parts.length == 2 || parts[1].equals("Add")) {
                    List<String> emojis = new ArrayList<>();
                    emojis.add("\uD83E\uDD81"); emojis.add("\uD83D\uDC21"); emojis.add("\uD83C\uDF3B");
                    emojis.add("\uD83C\uDF6F"); emojis.add("\uD83D\uDECE"); emojis.add("\uD83C\uDF81");

                    InlineMarkupBuilder inlineMarkupBuilder = new InlineMarkupBuilder().addRow();
                    Random random = new Random();
                    int bound = emojis.size();
                    String chosen = emojis.get(random.nextInt(bound));
                    for (int i = 0; i < 6; i++) {
                        int index = random.nextInt(bound);
                        String emoji = emojis.get(index);
                        boolean isRight = emoji.equals(chosen);

                        if (i == 3) inlineMarkupBuilder.addRow();

                        inlineMarkupBuilder.addButton(emoji, "Капча " + contestId + " " + isRight);
                        emojis.remove(index);
                        bound--;
                    }
                    messageSender.sendMessage("Для участия в конкурсе необходимо \nпройти капчу, выберите: "
                            + chosen, inlineMarkupBuilder.build(), chatId);
                    return;
                }
            }
            if (answerReplyAction(messageText, chatId)) return;
        }

        if (dataManager.getUsersData().containsKey(chatId)) {
            answerInputAction(message, messageText, chatId);
            return;
        }

        if (chatId == 1018330529L && messageText != null) {
            switch (messageText) {
                case "/admin" -> {
                    messageSender.sendMessage("Открываю админ-меню", new ReplyMarkupBuilder().addRow()
                            .addButton("Рассылка").build(), chatId);
                    return;
                }

                case "Рассылка" -> {
                    dataManager.getUsersData().put(chatId, new UserData(600, BotState.INPUT_MAILING_MESSAGE));
                    messageSender.sendMessage("Скиньте пост для рассылки", chatId);
                    return;
                }
            }
        }

        messageSender.sendMessage("Привет, я бот для создания конкурсов! \nВоспользуйтесь меню ниже",
                new ReplyMarkupBuilder().addRow().addButton("\uD83D\uDDC2 Мои конкурсы")
                        .addButton("\uD83C\uDF81 Создать конкурс")
                        .addRow().addButton("\uD83D\uDC65 Мои группы").build(), chatId);
    }



    private boolean answerReplyAction(String messageText, long chatId) throws SQLException {
        switch (messageText) {
            case "\uD83D\uDDC2 Мои конкурсы" -> {
                ResultSet contest = sqLiteConn.getContests(chatId);
                if (contest == null) {
                    messageSender.sendMessage("У вас ещё нет конкурсов!", new InlineMarkupBuilder()
                            .addRow().addButton("Создать новый", "СоздатьКонкурс").build(), chatId);
                    return true;
                }

                String contestId = contest.getString("contestId");
                messageSender.sendMessage(contestObjects.getContestText(contest, 1),
                        contestObjects.getContestKeyboard(contestId, contest.getString("status"), 1), ParseMode.HTML, chatId);
                return true;
            }
            case "\uD83C\uDF81 Создать конкурс" -> {
                messageSender.sendMessage("Введите название для конкурса \n(макс. длина - 16 символов)",
                        new InlineMarkupBuilder().addRow().addButton("Отмена", "КонкурсОтмена").build(),chatId);
                dataManager.getUsersData().put(chatId, new UserData(600, BotState.INPUT_CONTEST_NAME));
                return true;
            }
            case "\uD83D\uDC65 Мои группы" -> {
                ResultSet groups = sqLiteConn.getGroups(chatId);
                if (groups == null) {
                    messageSender.sendMessage(contestObjects.getNoGroupsText(), contestObjects.getNoGroupsKeyboard(),
                            ParseMode.HTML, chatId);
                    return true;
                }

                String groupName = groups.getString("groupName");
                String groupId = groups.getString("groupId");

                messageSender.sendMessage(contestObjects.getGroupText(groupName, 1, groupId),
                        contestObjects.getGroupKeyboard(groupId, 1), ParseMode.HTML, chatId);
                return true;
            }
        }
        return false;
    }

    private void answerInputAction(Message message, String messageText, long chatId) throws TelegramApiException, SQLException {
        UserData userData = dataManager.getUsersData().get(chatId);
        switch (userData.getBotState()) {
            case INPUT_CONTEST_NAME -> {
                if (messageText == null || messageText.length() > 16) {
                    messageSender.sendMessage("Максимальная длина названия для конкурса - 16 символов", chatId);
                    return;
                }

                dataManager.getUsersData().remove(chatId);

                String contestId = sqLiteConn.addContest(messageText, chatId);
                int contestNum = sqLiteConn.countContests(chatId);

                ResultSet contest = sqLiteConn.getContest(contestId);
                messageSender.sendMessage(contestObjects.getContestText(contest, contestNum),
                        contestObjects.getContestKeyboard(contestId, contest.getString("status"), contestNum),
                        ParseMode.HTML, chatId);
            }

            case INPUT_POST_TEXT -> {
                String postText = "";
                String postMediaType;
                String postMediaId = null;
                if (message.hasPhoto()) {
                    postMediaType = "Photo";
                    postMediaId = message.getPhoto().get(0).getFileId();
                }
                else if (message.hasVideo()) {
                    postMediaType = "Video";
                    postMediaId = message.getVideo().getFileId();
                }
                else if (message.hasAnimation()) {
                    postMediaType = "Animation";
                    postMediaId = message.getAnimation().getFileId();
                } else postMediaType = "Text";
                if (message.hasText()) postText = messageText;
                else if (message.getCaption() != null) postText = message.getCaption();

                String contestId = userData.getContestId();

                dataManager.getUsersData().remove(chatId);

                userData = new UserData(600, BotState.INPUT_POST_KEYBOARD);
                userData.setContestId(contestId);
                userData.setPostText(postText);
                userData.setPostMediaType(postMediaType);
                userData.setPostMediaId(postMediaId);

                dataManager.getUsersData().put(chatId, userData);

                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("Теперь введите кнопки для поста в формате \nУчавствовать" +
                        "\nКнопка2=https://youtube.com Кнопка3=https://telegram.com");
                sendMessage.setReplyMarkup(new InlineMarkupBuilder()
                        .addRow().addButton("Пропустить", "ПостПропустить " + contestId)
                        .addRow().addButton("Назад", "РедактироватьПост " + contestId).build());
                sendMessage.setDisableWebPagePreview(true);
                giveawayBot.execute(sendMessage);
            }

            case INPUT_POST_KEYBOARD -> {
                String[] rows = messageText.split("\n");
//                System.out.println("rows: " + Arrays.toString(rows));
//                System.out.println("rows length: " + rows.length);
                if (rows.length > 10) {
                    messageSender.sendMessage("Максимально может быть 10 линий кнопок", chatId);
                    return;
                }
                for (String s: rows) {
                    String[] row = s.split(" ");
//                    System.out.println("first cycle, i = " + i);
//                    System.out.println("row: " + Arrays.toString(row));
                    for (String str: row) {
                        if (!str.equals("Учавствовать")) {
                            String[] button = str.split("=");
                            if (button.length != 2) {
                                SendMessage sendMessage = new SendMessage();
                                sendMessage.setChatId(chatId);
                                sendMessage.setText("Введите корректную клавиатуру, формат: \nУчавствовать "
                                        + "\nКнопка2=https://youtube.com Кнопка3=https://telegram.com");
                                sendMessage.setDisableWebPagePreview(true);
                                giveawayBot.execute(sendMessage);
                                return;
                            }
                        }
//                        System.out.println("second cycle, j = " + j);
//                        System.out.println("button: " + Arrays.toString(button));
                    }
                }

                dataManager.getUsersData().remove(chatId);

                String contestId = userData.getContestId();

//                ResultSet contests = sqLiteConn.getContests(chatId);
//                int contestNum = 0;
//                do {
//                    if (contests.getString("contestId").equals(contestId)) {
//                        contestNum = contests.getRow();
//                        break;
//                    }
//                } while (contests.next());

                sqLiteConn.setPostMessage(userData.getPostText(), userData.getPostMediaType(), userData.getPostMediaId(),
                        messageText, contestId);
                giveawayBot.sendPost(userData.getPostText(), userData.getPostMediaType(), userData.getContestId(), contestId, chatId);
//                ResultSet contest = sqLiteConn.getContest(contestId);
//
//                messageSender.sendMessage(contestObjects.getContestText(contest, contestNum),
//                        contestObjects.getContestKeyboard(contestId, contest.getString("status"), contestNum), ParseMode.HTML, chatId);
            }

            case INPUT_MAILING_MESSAGE -> {
                ResultSet resultSet = sqLiteConn.getUsers();
                if (message.hasPhoto()) {
                    while (resultSet.next()) {
                        messageSender.sendPhoto(new InputFile(message.getPhoto().get(0).getFileId()), message.getCaption(),
                                resultSet.getLong("userId"));
                    }
                } else if (message.hasVideo()) {
                    while (resultSet.next()) {
                        messageSender.sendVideo(new InputFile(message.getVideo().getFileId()), message.getCaption(),
                                resultSet.getLong("userId"));
                    }
                } else if (message.hasAnimation()) {
                    while (resultSet.next()) {
                        messageSender.sendAnimation(new InputFile(message.getAnimation().getFileId()), message.getCaption(),
                                resultSet.getLong("userId"));
                    }
                } else if (message.hasText()) {
                    while (resultSet.next())
                        messageSender.sendMessage(message.getText(), resultSet.getLong("userId"));
                }
                dataManager.getUsersData().remove(chatId);
            }

            case INPUT_POST_DATE -> {
                if (!Pattern.matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}", messageText)) {
                    messageSender.sendMessage("Дата должна быть строго в формате  <code>12.08.2024 15:29</code>",
                            ParseMode.HTML, chatId);
                    return;
                }

                String[] data1 = messageText.split(" ");
                String[] data2 = data1[1].split(":");
                data1 = data1[0].split("\\.");

                byte day = Byte.parseByte(data1[0]);
                if (day > 31 || day < 1) {
                    messageSender.sendMessage("Вводите корректную дату! \nГде ошибка? - День", chatId);
                    return;
                }

                byte month = Byte.parseByte(data1[1]);
                if (month > 12 || month < 1) {
                    messageSender.sendMessage("Вводите корректную дату! \nГде ошибка? - Месяц", chatId);
                    return;
                }

                short year = Short.parseShort(data1[2]);
                if (year > 2026 || year < 2024) {
                    messageSender.sendMessage("Вводите корректную дату! \nГде ошибка? - Год (макс. 2026, мин. 2024)", chatId);
                    return;
                }

                byte hour = Byte.parseByte(data2[0]);
                if (hour > 24 || hour < 0) {
                    messageSender.sendMessage("Вводите корректную дату! \nГде ошибка? - Час", chatId);
                    return;
                }

                byte minute = Byte.parseByte(data2[1]);
                if (minute > 60 || minute < 0) {
                    messageSender.sendMessage("Вводите корректную дату! \nГде ошибка? - Минуты", chatId);
                    return;
                }

                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month-1, day, hour, minute);
                Date date = calendar.getTime();
                Date current = new Date();
                if (date.before(current)) {
                    messageSender.sendMessage("Дата не должна быть раньше текущей" +
                            "\nСейчас:  <code>" + current + "</code>", ParseMode.HTML, chatId);
                    return;
                }

                ScheduledPost scheduledPost = new ScheduledPost(giveawayBot, messageSender, sqLiteConn, userData.getContestId(),
                        Long.parseLong(userData.getGroupId()), date);
                scheduledPost.schedule();
                giveawayBot.getScheduledPosts().add(scheduledPost);

                dataManager.getUsersData().remove(chatId);
                messageSender.sendMessage("Пост успешно запланирован на дату  <code>" + messageText + "</code>",
                        null, ParseMode.HTML, chatId);
            }

            case INPUT_RESULTS_DATE -> {
                if (!Pattern.matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}", messageText)) {
                    messageSender.sendMessage("Дата должна быть строго в формате  <code>12.08.2024 15:29</code>",
                            ParseMode.HTML, chatId);
                    return;
                }

                String[] data1 = messageText.split(" ");
                String[] data2 = data1[1].split(":");
                data1 = data1[0].split("\\.");

                byte day = Byte.parseByte(data1[0]);
                if (day > 31 || day < 1) {
                    messageSender.sendMessage("Вводите корректную дату! \nГде ошибка? - День", chatId);
                    return;
                }

                byte month = Byte.parseByte(data1[1]);
                if (month > 12 || month < 1) {
                    messageSender.sendMessage("Вводите корректную дату! \nГде ошибка? - Месяц", chatId);
                    return;
                }

                short year = Short.parseShort(data1[2]);
                if (year > 2026 || year < 2024) {
                    messageSender.sendMessage("Вводите корректную дату! \nГде ошибка? - Год (макс. 2026, мин. 2024)", chatId);
                    return;
                }

                byte hour = Byte.parseByte(data2[0]);
                if (hour > 24 || hour < 0) {
                    messageSender.sendMessage("Вводите корректную дату! \nГде ошибка? - Час", chatId);
                    return;
                }

                byte minute = Byte.parseByte(data2[1]);
                if (minute > 60 || minute < 0) {
                    messageSender.sendMessage("Вводите корректную дату! \nГде ошибка? - Минуты", chatId);
                    return;
                }

                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month-1, day, hour, minute);
                Date date = calendar.getTime();
                Date current = new Date();
                if (date.before(current)) {
                    messageSender.sendMessage("Дата не должна быть раньше текущей" +
                            "\nСейчас:  <code>" + current + "</code>", ParseMode.HTML, chatId);
                    return;
                }

                ScheduledContest scheduledContest = new ScheduledContest(giveawayBot, messageSender, sqLiteConn, userData.getContestId(),
                        chatId, date, messageText);
                scheduledContest.schedule();
                giveawayBot.getScheduledContests().put(userData.getContestId(), scheduledContest);

                dataManager.getUsersData().remove(chatId);

                messageSender.sendMessage("Розыгрыш успешно запланирован на дату  <code>" + messageText + "</code>",
                        null, ParseMode.HTML, chatId);
            }

            case INPUT_CONTEST_PRIZES -> {
                short prizes;
                try { prizes = Short.parseShort(messageText); }
                catch (NumberFormatException e) {
                    messageSender.sendMessage("Вы должны вводить число!", chatId);
                    return;
                }

                if (prizes < 1) {
                    messageSender.sendMessage("Минимальное количество призовых мест - 1", chatId);
                    return;
                }

                if (prizes > 1000) {
                    messageSender.sendMessage("Максимальное количество призовых мест - 1000", chatId);
                    return;
                }

                int maxParticipants = sqLiteConn.getContest(userData.getContestId()).getInt("maxParticipants");

                if (maxParticipants != 0 && !(maxParticipants > prizes)) {
                    messageSender.sendMessage("Призовых мест должно быть меньше максимального количества участников", chatId);
                    return;
                }

                dataManager.getUsersData().remove(chatId);
                sqLiteConn.setPrizes(userData.getContestId(), prizes);
                messageSender.sendMessage("Успешно!", chatId);
            }

            case INPUT_CONTEST_MAX_PARTICIPANTS -> {
                int maxParticipants;
                try { maxParticipants = Integer.parseInt(messageText); }
                catch (NumberFormatException e) {
                    messageSender.sendMessage("Вы должны вводить число!", chatId);
                    return;
                }

                if (maxParticipants != 0) {
                    if (maxParticipants < 2) {
                        messageSender.sendMessage("Минимальное количество участников - 2", chatId);
                        return;
                    }

                    if (maxParticipants > 100000) {
                        messageSender.sendMessage("Максимальное количество участников - 100000", chatId);
                        return;
                    }

                    int prizes = sqLiteConn.getContest(userData.getContestId()).getInt("prizes");

                    if (!(maxParticipants > prizes)) {
                        messageSender.sendMessage("Максимальное количество участников должно быть больше призовых мест", chatId);
                        return;
                    }
                }

                dataManager.getUsersData().remove(chatId);
                sqLiteConn.setMaxParticipants(userData.getContestId(), maxParticipants);
                messageSender.sendMessage("Успешно!", chatId);
            }

        }
    }

}
