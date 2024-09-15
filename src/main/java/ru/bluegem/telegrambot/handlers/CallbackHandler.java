package ru.bluegem.telegrambot.handlers;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaAnimation;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.bluegem.telegrambot.containers.ContestObjects;
import ru.bluegem.telegrambot.sqlite.SQLiteConn;
import ru.bluegem.telegrambot.GiveawayBot;
import ru.bluegem.telegrambot.botapi.markups.InlineMarkupBuilder;
import ru.bluegem.telegrambot.botapi.messages.MessageSender;
import ru.bluegem.telegrambot.cache.DataManager;
import ru.bluegem.telegrambot.cache.UserData;
import ru.bluegem.telegrambot.enums.BotState;
import ru.bluegem.telegrambot.schedule.ScheduledContest;
import ru.bluegem.telegrambot.schedule.ScheduledPost;

import javax.xml.transform.Result;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class CallbackHandler {

    private final GiveawayBot giveawayBot;

    private final MessageSender messageSender;
    private final SQLiteConn sqLiteConn;
    private final DataManager dataManager;

    private final ContestObjects contestObjects;

    public CallbackHandler(GiveawayBot giveawayBot, MessageSender messageSender, SQLiteConn sqLiteConn, DataManager dataManager) {
        this.giveawayBot = giveawayBot;
        this.messageSender = messageSender;
        this.sqLiteConn = sqLiteConn;
        this.dataManager = dataManager;
        this.contestObjects = giveawayBot.getContestObjects();
    }

    public void handleCallback(Update update) throws TelegramApiException, SQLException {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String callbackData = callbackQuery.getData();

        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
        answerCallbackQuery.setCallbackQueryId(callbackQuery.getId());

        String[] data = callbackData.split(" ");
        String contestId = null;
        if (data.length > 1) contestId = data[1];

        if (data[0].equals("Учавствовать")) {
            sqLiteConn.addParticipationPost(contestId, callbackQuery.getInlineMessageId());
            answerCallbackQuery.setShowAlert(true);

            ResultSet contest = sqLiteConn.getContest(contestId);

            int participants = contest.getInt("participants");
            if (contest.getBoolean("captcha")) {
                String postKeyboard = contest.getString("postKeyboard");
                InlineKeyboardMarkup keyboardMarkup = giveawayBot.parseKeyboard(postKeyboard, participants, contestId,
                        true);

                EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
                editMessageReplyMarkup.setInlineMessageId(callbackQuery.getInlineMessageId());
                editMessageReplyMarkup.setReplyMarkup(keyboardMarkup);
                giveawayBot.execute(editMessageReplyMarkup);

                answerCallbackQuery.setText("Нажмите \"Учавствовать\" ещё раз, подгружали капчу!");
                giveawayBot.execute(answerCallbackQuery);
                return;
            }
            int maxParticipants = contest.getInt("maxParticipants");

            if (maxParticipants != 0 && participants+1 > maxParticipants) {
                answerCallbackQuery.setText("Достигнут лимит по участникам!");
                giveawayBot.execute(answerCallbackQuery);
            }

            User user = callbackQuery.getFrom();
            long userId = user.getId();
            if (sqLiteConn.getContestParticipant(userId, contestId) != null) {
                answerCallbackQuery.setText("Вы уже учавствуете в конкурсе!");
                giveawayBot.execute(answerCallbackQuery);
                return;
            }

            ResultSet subCheck = sqLiteConn.getSubCheck(contestId);
            if (subCheck != null) {
                GetChatMember getChatMember = new GetChatMember();
                getChatMember.setUserId(userId);
                do {
                    long groupId = subCheck.getLong("groupId");
                    getChatMember.setChatId(groupId);
                    ChatMember chatMember = giveawayBot.execute(getChatMember);
                    if (!chatMember.getStatus().equals("member") && !chatMember.getStatus().equals("administrator")
                            && !chatMember.getStatus().equals("creator")) {
                        answerCallbackQuery.setText("Подпишитесь на группы для участия в конкурсе!");
                        giveawayBot.execute(answerCallbackQuery);
                        return;
                    }
                } while (subCheck.next());

            }

//            if (contest.getBoolean("captcha")) {
//                answerCallbackQuery.setText("Нажмите \"Учавствовать\" ещё раз, подгружали капчу!");
//                giveawayBot.execute(answerCallbackQuery);
//                keyboardMarkup = giveawayBot.parseKeyboard(postKeyboard, participants, contestId, true);
//
//                EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
//                editMessageReplyMarkup.setInlineMessageId(callbackQuery.getInlineMessageId());
//                editMessageReplyMarkup.setReplyMarkup(keyboardMarkup);
//                giveawayBot.execute(editMessageReplyMarkup);
//                return;
//                List<String> emojis = new ArrayList<>();
//                emojis.add("\uD83E\uDD81"); emojis.add("\uD83D\uDC21"); emojis.add("\uD83C\uDF3B");
//                emojis.add("\uD83C\uDF6F"); emojis.add("\uD83D\uDECE"); emojis.add("\uD83C\uDF81");
//
//                InlineMarkupBuilder inlineMarkupBuilder = new InlineMarkupBuilder().addRow();
//                Random random = new Random();
//                int bound = emojis.size();
//                String chosen = emojis.get(random.nextInt(bound));
//                for (int i = 0; i < 6; i++) {
//                    int index = random.nextInt(bound);
//                    String emoji = emojis.get(index);
//                    boolean isRight = emoji.equals(chosen);
//
//                    if (i == 3) inlineMarkupBuilder.addRow();
//
//                    inlineMarkupBuilder.addButton(emoji, "Капча " + contestId + " " + isRight + " "
//                            + callbackQuery.getInlineMessageId());
//                    emojis.remove(index);
//                    bound--;
//                }
//                messageSender.sendMessage("Для участия в конкурсе необходимо \nпройти капчу, выберите: "
//                        + chosen, inlineMarkupBuilder.build(), userId);
//                return;
//            }

            String postKeyboard = contest.getString("postKeyboard");
            InlineKeyboardMarkup keyboardMarkup = giveawayBot.parseKeyboard(postKeyboard, sqLiteConn.addContestParticipant(user, contestId),
                    contestId, false);

            EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
            editMessageReplyMarkup.setInlineMessageId(callbackQuery.getInlineMessageId());
            editMessageReplyMarkup.setReplyMarkup(keyboardMarkup);
            giveawayBot.execute(editMessageReplyMarkup);

            answerCallbackQuery.setText("Теперь вы учавствуете в конкурсе!");
            giveawayBot.execute(answerCallbackQuery);
            return;
        }

        Message message = (Message) callbackQuery.getMessage();
        if (message == null) return;
        long chatId = message.getChatId();

        switch (data[0]) {

            case "Конкурс" -> {
                giveawayBot.execute(answerCallbackQuery);

                ResultSet contests = sqLiteConn.getContests(chatId);
                int contestNum = 0;
                do {
                    if (contests.getString("contestId").equals(contestId)) {
                        contestNum = contests.getRow();
                        break;
                    }
                } while (contests.next());

                ResultSet contest = sqLiteConn.getContest(contestId);

                messageSender.deleteMessage(chatId, message.getMessageId());
                messageSender.sendMessage(contestObjects.getContestText(contest, contestNum),
                        contestObjects.getContestKeyboard(contestId, contest.getString("status"), contestNum), ParseMode.HTML, chatId);
            }

            case "КонкурсРазыграть" -> {
                giveawayBot.execute(answerCallbackQuery);

                String contestName = sqLiteConn.getContest(contestId).getString("contestName");
                messageSender.editMessageText("Подвести итоги конкурса \"<b>" + contestName + "</b>\" прямо сейчас?",
                        new InlineMarkupBuilder().addRow().addButton("Разыграть", "КонкурсРазыграть2 " + contestId)
                        .addRow().addButton("Запланировать", "РазыгратьПлан " + contestId)
                        .addRow().addButton("Назад", "Конкурс " + contestId).build(), ParseMode.HTML,
                        chatId, message.getMessageId());
            }

            case "КонкурсРазыграть2" -> {
                ResultSet contest = sqLiteConn.getContest(contestId);
                if (contest.getString("status").equals("finished")) {
                    messageSender.deleteMessage(chatId, message.getMessageId());
                    answerCallbackQuery.setShowAlert(true);
                    answerCallbackQuery.setText("Конкурс уже разыгран, вы можете переиграть его в меню конкурса!");
                    giveawayBot.execute(answerCallbackQuery);
                    return;
                }

                int participants = contest.getInt("participants");
                int prizes = contest.getInt("prizes");

                if (prizes > participants) {
                    answerCallbackQuery.setShowAlert(true);
                    answerCallbackQuery.setText("Участников слишком мало для розыгрыша!");
                    giveawayBot.execute(answerCallbackQuery);
                    return;
                }
                giveawayBot.execute(answerCallbackQuery);

                ResultSet participant = sqLiteConn.getParticipants(contestId);
                Random random = new Random();
                Set<Integer> winners = new HashSet<>();

                while (winners.size() < prizes) {
                    int winner = random.nextInt(participants) + 1;
                    winners.add(winner);
                }

                StringBuilder sb = new StringBuilder();
                while (participant.next()) {
                    if (winners.contains(participant.getRow())) {
                        sqLiteConn.setContestWinner(contestId, participant.getLong("userId"));
                        sb.append(" @").append(participant.getString("username"));
                    }
                }

                sqLiteConn.setContestFinished(contestId);
                giveawayBot.getScheduledContests().remove(contestId);

                messageSender.deleteMessage(chatId, message.getMessageId());
                messageSender.sendMessage("Конкурс \"<b>" + contest.getString("contestName")
                        + "</b>\" разыгран! \n Победители: " + sb, new InlineMarkupBuilder()
                        .addRow().addButton("К конкурсу", "Конкурс " + contestId).build(), ParseMode.HTML, chatId);
            }

            case "КонкурсПост" -> {
                giveawayBot.execute(answerCallbackQuery);

                ResultSet contest = sqLiteConn.getContest(contestId);
                String postText = contest.getString("postText");
                String postMediaType = contest.getString("postMediaType");
                String postMediaId = contest.getString("postMediaId");

                messageSender.deleteMessage(chatId, message.getMessageId());
                giveawayBot.sendPost(postText, postMediaType, postMediaId, contestId, chatId);
            }

            case "РедактироватьПост" -> {
                giveawayBot.execute(answerCallbackQuery);

                UserData userData = new UserData(600, BotState.INPUT_POST_TEXT);
                userData.setContestId(contestId);
                dataManager.getUsersData().put(chatId, userData);

                messageSender.deleteMessage(chatId, message.getMessageId());
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("""
                        <b>Отправьте новый пост</b>\s

                        Плейсхолдеры:\s
                        <code>{members}</code> - Кол-во участников\s
                        <code>{max_members}</code> - Макс. участников\s
                        <code>{prizes}</code> - Кол-во призовых мест
                        
                        Форматирование:\s
                        &lt;i&gt;Курсив&lt;/i&gt; - <i>Курсив</i>
                        &lt;b&gt;Жирный&lt;/b&gt; - <b>Жирный</b>
                        &lt;code&gt;Моно&lt;/code&gt; - <code>Моно</code>
                        &lt;u&gt;Подчеркнутый&lt;/u&gt; - <u>Подчеркнутый</u>
                        &lt;s&gt;Перечеркнутый&lt;/s&gt; - <s>Перечеркнутый</s>
                        &lt;tg-spoiler&gt;Спойлер&lt;/tg-spoiler&gt; - <tg-spoiler>Спойлер</tg-spoiler>
                        &lt;a href="https://telegram.com"&gt;Ссылка&lt;/a&gt; - <a href="https://telegram.com">Ссылка</a>""");
                sendMessage.setReplyMarkup(new InlineMarkupBuilder().addRow()
                        .addUrlButton("FAQ | Форматирование", "Null", "https://telegra.ph/Formatirovanie-08-29")
                        .addRow().addButton("Назад", "КонкурсПост " + contestId).build());
                sendMessage.setParseMode(ParseMode.HTML);
                sendMessage.setDisableWebPagePreview(true);
                giveawayBot.execute(sendMessage);
            }

            case "ЗапланироватьПост" -> {
                giveawayBot.execute(answerCallbackQuery);
                dataManager.getUsersData().remove(chatId);

                ResultSet groups = sqLiteConn.getGroups(chatId);

                messageSender.deleteMessage(chatId, message.getMessageId());

                if (groups == null) {
                    messageSender.sendMessage("Для планировки постов нужно добавить хотябы 1 Канал/Группу", chatId);
                    return;
                }

                InlineMarkupBuilder inlineMarkupBuilder = new InlineMarkupBuilder();
                inlineMarkupBuilder.addRow().addButton(groups.getString("groupName"), "Пост2 "
                        + contestId + " " + groups.getString("groupId"));

                while (groups.next()) {
                    inlineMarkupBuilder.addRow().addButton(groups.getString("groupName"), "Пост2 "
                            + contestId + " " + groups.getLong("groupId"));
                }
                inlineMarkupBuilder.addRow().addButton("Назад", "КонкурсПост " + contestId);

                messageSender.sendMessage("Выберите чат для запланированного поста",
                        inlineMarkupBuilder.build(), chatId);
            }

            case "ЗапланированныеП" -> {
                InlineMarkupBuilder inlineMarkupBuilder = new InlineMarkupBuilder();
                for(ScheduledPost scheduledPost: giveawayBot.getScheduledPosts()) {
                    if (scheduledPost.getContestId().equals(contestId)) {
                        inlineMarkupBuilder.addRow().addButton(scheduledPost.getScheduleDate().toString(),
                                "Запланирован " + contestId + " " + scheduledPost.getScheduleDate().getTime());
                    }
                }

                if (inlineMarkupBuilder.getRows() == 0) {
                    answerCallbackQuery.setShowAlert(true);
                    answerCallbackQuery.setText("Запланированных постов нет!");
                    giveawayBot.execute(answerCallbackQuery);
                    return;
                }

                giveawayBot.execute(answerCallbackQuery);

                inlineMarkupBuilder.addRow().addButton("Назад", "КонкурсПост " + contestId);

                messageSender.deleteMessage(chatId, message.getMessageId());
                messageSender.sendMessage("Запланированные посты", inlineMarkupBuilder.build(), chatId);
            }

            case "Запланирован" -> {
                giveawayBot.execute(answerCallbackQuery);
                long scheduledDate = Long.parseLong(data[2]);
                ScheduledPost scheduledPost = null;
                for(ScheduledPost scheduledPost1: giveawayBot.getScheduledPosts()) {
                    if (scheduledPost1.getContestId().equals(contestId)
                            || scheduledPost1.getScheduleDate().getTime() == scheduledDate) scheduledPost = scheduledPost1;
                }

                messageSender.editMessageText("<b>Запланированный пост</b> \n Дата: " + new Date(scheduledDate)
                        + "\n ID чата: " + scheduledPost.getChatId(), new InlineMarkupBuilder().addRow()
                        .addButton("Удалить", "ЗапланУдалить " + contestId + " " + scheduledDate)
                        .addRow().addButton("Назад", "ЗапланированныеП " + contestId)
                        .build(), ParseMode.HTML, chatId, message.getMessageId());
            }

            case "ЗапланУдалить" -> {
                answerCallbackQuery.setShowAlert(true);
                answerCallbackQuery.setText("Запланированный пост удален!");
                giveawayBot.execute(answerCallbackQuery);
                long scheduledDate = Long.parseLong(data[2]);

                for(ScheduledPost scheduledPost: giveawayBot.getScheduledPosts()) {
                    if (scheduledPost.getContestId().equals(contestId)
                            || scheduledPost.getScheduleDate().getTime() == scheduledDate) {
                        scheduledPost.getTimer().cancel();
                        giveawayBot.getScheduledPosts().remove(scheduledPost);
                    }
                }

                giveawayBot.getScheduledContests().remove(contestId);

                messageSender.editMessageText("Запланированный пост удален!", new InlineMarkupBuilder().addRow()
                        .addButton("К другим постам", "ЗапланированныеП").build(), chatId, message.getMessageId());
            }

            case "Пост2" -> {
                UserData userData = new UserData(600, BotState.INPUT_POST_DATE);
                userData.setGroupId(data[2]);
                userData.setContestId(contestId);
                dataManager.getUsersData().put(chatId, userData);
                messageSender.editMessageText("Введите дату в формате  <code>12.08.2024 15:11</code>",
                        new InlineMarkupBuilder().addRow().addButton("Назад", "ЗапланироватьПост "
                                        + contestId).build(), ParseMode.HTML, chatId, message.getMessageId());
            }

            case "КонкурсУчастники" -> {
                giveawayBot.execute(answerCallbackQuery);

                ResultSet contest = sqLiteConn.getContest(contestId);

                messageSender.editMessageText("Конкурс \"<b>" + contest.getString("contestName")
                                + "</b>\" \n",
                        new InlineMarkupBuilder().addRow().addButton("Excel Таблица", "ExcelТаблица " + contestId)
                                .addRow().addButton("Назад", "Конкурс " + contestId).build(), ParseMode.HTML,
                        chatId, message.getMessageId());
            }

            case "ExcelТаблица" -> {
                giveawayBot.execute(answerCallbackQuery);
                Workbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("Users");

                Row header = sheet.createRow(0);
                CellStyle headerStyle = workbook.createCellStyle();

                XSSFFont font = ((XSSFWorkbook) workbook).createFont();
                font.setFontName("Arial");
                font.setFontHeightInPoints((short) 18);
                headerStyle.setFont(font);

                Cell headerCell = header.createCell(0);
                headerCell.setCellValue("ID пользователя");
                headerCell.setCellStyle(headerStyle);

                headerCell = header.createCell(1);
                headerCell.setCellValue("ID конкурса");
                headerCell.setCellStyle(headerStyle);

                headerCell = header.createCell(2);
                headerCell.setCellValue("Юзернейм");
                headerCell.setCellStyle(headerStyle);

                headerCell = header.createCell(3);
                headerCell.setCellValue("Имя");
                headerCell.setCellStyle(headerStyle);

                headerCell = header.createCell(4);
                headerCell.setCellValue("Время записи");
                headerCell.setCellStyle(headerStyle);

                sheet.setColumnWidth(0, 6500);
                sheet.setColumnWidth(1, 6500);
                sheet.setColumnWidth(2, 6500);
                sheet.setColumnWidth(3, 6500);
                sheet.setColumnWidth(4, 7000);
                header.setHeight((short) 420);

                CellStyle cellStyle = workbook.createCellStyle();
                XSSFFont font2 = ((XSSFWorkbook) workbook).createFont();
                font.setFontName("Arial");
                font.setFontHeightInPoints((short) 16);
                cellStyle.setFont(font2);

                ResultSet resultSet = sqLiteConn.getParticipants(contestId);

                int i = 1;
                while (resultSet.next()){
                    Row row = sheet.createRow(i);
                    Cell cell = row.createCell(0);
                    cell.setCellValue(resultSet.getString("userId"));

                    cell.setCellStyle(cellStyle);
                    cell = row.createCell(1);
                    cell.setCellValue(contestId);
                    cell.setCellStyle(cellStyle);

                    cell = row.createCell(2);
                    cell.setCellValue(resultSet.getString("username"));
                    cell.setCellStyle(cellStyle);

                    cell = row.createCell(3);
                    cell.setCellValue(resultSet.getString("firstname"));
                    cell.setCellStyle(cellStyle);

                    cell = row.createCell(4);
                    cell.setCellValue(resultSet.getString("registerDate"));
                    cell.setCellStyle(cellStyle);

                    i++;
                }

                SendDocument sendDocument = new SendDocument();
                sendDocument.setChatId(chatId);
                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    workbook.write(bos);
                    byte[] barray = bos.toByteArray();
                    InputStream is = new ByteArrayInputStream(barray);
                    sendDocument.setDocument(new InputFile(is, "users.xlsx"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                giveawayBot.execute(sendDocument);
            }

            case "КНастройки" -> {
                giveawayBot.execute(answerCallbackQuery);

                dataManager.getUsersData().remove(chatId);

                String captchaEmoji;
                if (sqLiteConn.getContest(contestId).getBoolean("captcha")) captchaEmoji = "✅";
                else captchaEmoji = "❌";

                messageSender.editMessageText("Настройки конкурса \"<b>" + sqLiteConn.getContest(contestId)
                        .getString("contestName") + "</b>\"", new InlineMarkupBuilder()
                                .addRow().addButton("Капча  " + captchaEmoji, "КонкурсКапча " + contestId)
                        .addRow().addButton("Призовые места", "КМеста " + contestId)
                        .addRow().addButton("Макс. участников", "КМаксУчастников " + contestId)
                        .addRow().addButton("Назад", "Конкурс " + contestId).build(), ParseMode.HTML, chatId,
                        message.getMessageId());
            }

            case "КМеста" -> {
                giveawayBot.execute(answerCallbackQuery);
                UserData userData = new UserData(600, BotState.INPUT_CONTEST_PRIZES);
                userData.setContestId(contestId);
                dataManager.getUsersData().put(chatId, userData);
                messageSender.editMessageText("Введите количество призовых мест для конкурса <code>(макс. 1000)</code>",
                        new InlineMarkupBuilder().addRow().addButton("Отмена", "КНастройки " + contestId).build(),
                        ParseMode.HTML, chatId, message.getMessageId());
            }

            case "КМаксУчастников" -> {
                giveawayBot.execute(answerCallbackQuery);
                UserData userData = new UserData(600, BotState.INPUT_CONTEST_MAX_PARTICIPANTS);
                userData.setContestId(contestId);
                dataManager.getUsersData().put(chatId, userData);
                messageSender.editMessageText("Введите максимальное количество участников для конкурса " +
                        "\n<code>(0 - бесконечность, макс. 100000)</code>",
                        new InlineMarkupBuilder().addRow().addButton("Отмена", "КНастройки " + contestId)
                                .build(), ParseMode.HTML, chatId, message.getMessageId());
            }

            case "КонкурсПодписка" -> {
                giveawayBot.execute(answerCallbackQuery);
                ResultSet groups = sqLiteConn.getGroups(chatId);

                if (groups == null) {
                    messageSender.sendMessage(contestObjects.getNoGroupsText(), contestObjects.getNoGroupsKeyboard(),
                            ParseMode.HTML, chatId);
                    return;
                }

                messageSender.deleteMessage(chatId, message.getMessageId());
                ResultSet subCheck = sqLiteConn.getSubCheck(contestId);
                if (subCheck != null) {
                    sendSubAddedGroups(subCheck, contestId, chatId);
                } else sendSubAllGroups(groups, contestId, chatId);

            }

            case "ПодпискаУдалить" -> {
                answerCallbackQuery.setShowAlert(true);
                answerCallbackQuery.setText("Проверка на подписку отключена для группы!");
                giveawayBot.execute(answerCallbackQuery);

                long groupId = Long.parseLong(data[2]);
                sqLiteConn.deleteSubCheck(contestId, groupId);

                messageSender.deleteMessage(chatId, message.getMessageId());
                ResultSet subCheck = sqLiteConn.getSubCheck(contestId);
                if (subCheck != null) {
                    sendSubAddedGroups(subCheck, contestId, chatId);
                } else sendSubAllGroups(sqLiteConn.getGroups(chatId), contestId, chatId);

            }

            case "ПодпискаДобавить" -> {
                ResultSet groups = sqLiteConn.getGroups(chatId);

                if (groups == null) {
                    giveawayBot.execute(answerCallbackQuery);
                    messageSender.sendMessage(contestObjects.getNoGroupsText(), contestObjects.getNoGroupsKeyboard(),
                            ParseMode.HTML, chatId);
                    return;
                }

                ResultSet subCheck = sqLiteConn.getSubCheck(contestId);

                Set<String> addedGroups = new HashSet<>();
                do {
                    addedGroups.add(subCheck.getString("groupId"));
                } while (subCheck.next());

                InlineMarkupBuilder inlineMarkupBuilder = new InlineMarkupBuilder();
                String groupId = groups.getString("groupId");
                if (!addedGroups.contains(groupId)) inlineMarkupBuilder.addRow().addButton(groups.getString("groupName"),
                        "КонкурсПодписка2 " + contestId + " " + groupId);

                while (groups.next()) {
                    groupId = groups.getString("groupId");
                    if (!addedGroups.contains(groupId))
                        inlineMarkupBuilder.addRow().addButton(groups.getString("groupName"),
                                "КонкурсПодписка2 " + contestId + " " + groupId);
                }

                if (inlineMarkupBuilder.getRows() == 0) {
                    answerCallbackQuery.setShowAlert(true);
                    answerCallbackQuery.setText("Нету групп для добавления!");
                    giveawayBot.execute(answerCallbackQuery);
                    return;
                }

                inlineMarkupBuilder.addRow().addButton("Назад", "КонкурсПодписка " + contestId);

                messageSender.deleteMessage(chatId, message.getMessageId());
                messageSender.sendMessage("Выберите чат для проверки подписки",
                        inlineMarkupBuilder.build(), chatId);
            }

            case "КонкурсПодписка2" -> {
                giveawayBot.execute(answerCallbackQuery);
                long groupId = Long.parseLong(data[2]);
                sqLiteConn.addSubCheck(contestId, groupId);

                messageSender.deleteMessage(chatId, message.getMessageId());
                sendSubAddedGroups(sqLiteConn.getSubCheck(contestId), contestId, chatId);
            }

            case "Дальше" -> {
                giveawayBot.execute(answerCallbackQuery);
                int contestNum;
                int max = sqLiteConn.countContests(chatId);
                if (data[1].equals("max")) {
                    contestNum = max;
                    int current = Integer.parseInt(data[2]);
                    if (current == contestNum) return;
                }
                else if (data[1].equals("first")) {
                    contestNum = 1;
                    int current = Integer.parseInt(data[2]);
                    if (current == contestNum) return;
                }
                else {
                    contestNum = Integer.parseInt(data[1]);
                    if (contestNum > max) return;
                }

                if (contestNum < 1) return;

                ResultSet contest = sqLiteConn.getContests(chatId);
                while (contest.getRow() != contestNum) {
                    if (!contest.next()) return;
                }
                contestId = contest.getString("contestId");

                messageSender.editMessageText(contestObjects.getContestText(contest, contestNum),
                        contestObjects.getContestKeyboard(contestId, contest.getString("status"), contestNum),
                        ParseMode.HTML, chatId, message.getMessageId());
            }

            case "ГруппаДальше" -> {
                giveawayBot.execute(answerCallbackQuery);
                int groupNum;
                int max = sqLiteConn.countGroups(chatId);
                if (data[1].equals("max")) {
                    groupNum = max;
                    int current = Integer.parseInt(data[2]);
                    if (current == groupNum) return;
                }
                else if (data[1].equals("first")) {
                    groupNum = 1;
                    int current = Integer.parseInt(data[2]);
                    if (current == groupNum) return;
                }
                else {
                    groupNum = Integer.parseInt(data[1]);
                    if (groupNum > max) return;
                }

                if (groupNum < 1) return;

                ResultSet group = sqLiteConn.getGroups(chatId);
                while (group.getRow() != groupNum) {
                    if (!group.next()) return;
                }

                String groupId = group.getString("groupId");
                String groupName = group.getString("groupName");

                messageSender.editMessageText(contestObjects.getGroupText(groupName, groupNum, groupId),
                        contestObjects.getGroupKeyboard(groupId, groupNum), ParseMode.HTML, chatId, message.getMessageId());
            }

            case "УдалитьГруппу" -> {
                giveawayBot.execute(answerCallbackQuery);
                sqLiteConn.deleteGroup(data[1]);

                long groupId = Long.parseLong(data[1]);

                for (ScheduledPost scheduledPost: giveawayBot.getScheduledPosts()) {
                    if (scheduledPost.getChatId() == groupId) giveawayBot.getScheduledPosts().remove(scheduledPost);
                }

                LeaveChat leaveChat = new LeaveChat();
                leaveChat.setChatId(data[1]);
                try { giveawayBot.execute(leaveChat); }
                catch (TelegramApiException ignored) { }

                messageSender.editMessageText("Группа успешно удалена!", new InlineMarkupBuilder()
                        .addRow().addButton("К списку групп", "Группы").build(), chatId, message.getMessageId());
            }

            case "ДобавитьГруппу" -> {
                giveawayBot.execute(answerCallbackQuery);
                messageSender.sendMessage("<b>Добавить чат</b> \n\nВы можете добавить бота в чат," +
                        "\nиспользуя кнопки <code>Канал</code> или <code>Группа</code> \n\n‼\uFE0F У бота обязательно " +
                        "должны быть права на отправку, редактирование и удаление сообщений", new InlineMarkupBuilder().addRow()
                        .addUrlButton("Канал", "Null", "https://t.me/ContestCreatorBot?startchannel=true")
                        .addUrlButton("Группа", "Null", "https://t.me/ContestCreatorBot?startgroup=true")
                        .build(), ParseMode.HTML, chatId);
            }

            case "Группы" -> {
                giveawayBot.execute(answerCallbackQuery);
                ResultSet groups = sqLiteConn.getGroups(chatId);

                if (groups == null) {
                    messageSender.editMessageText(contestObjects.getNoGroupsText(), contestObjects.getNoGroupsKeyboard(),
                            ParseMode.HTML, chatId, message.getMessageId());
                    return;
                }

                String groupName = groups.getString("groupName");
                String groupId = groups.getString("groupId");

                messageSender.editMessageText(contestObjects.getGroupText(groupName, 1, groupId),
                        contestObjects.getGroupKeyboard(groupId, 1), ParseMode.HTML, chatId, message.getMessageId());
            }

            case "Запланировано" -> {
                giveawayBot.execute(answerCallbackQuery);
                long groupId = Long.parseLong(data[1]);

                InlineMarkupBuilder inlineMarkupBuilder = new InlineMarkupBuilder();

                for (ScheduledPost scheduledPost: giveawayBot.getScheduledPosts()) {
                    if (scheduledPost.getChatId() == groupId) {
                        inlineMarkupBuilder.addRow().addButton(scheduledPost.getScheduleDate().toString(),
                                "Запланирован " + groupId);
                    }
                }

                messageSender.editMessageText("Запланированные посты для группы " + groupId,
                        inlineMarkupBuilder.build(), chatId, message.getMessageId());
            }

            case "УдалитьКонкурс1" -> {
                giveawayBot.execute(answerCallbackQuery);

                messageSender.editMessageText("Вы уверены, что хотите удалить конкурс \"<b>"
                        + sqLiteConn.getContest(contestId).getString("contestName") + "</b>\"?",
                        new InlineMarkupBuilder().addRow().addButton("Удалить", "УдалитьКонкурс2 " + contestId)
                        .addRow().addButton("Назад", "Конкурс " + contestId).build(), ParseMode.HTML,
                        chatId, message.getMessageId());
            }

            case "УдалитьКонкурс2" -> {
                answerCallbackQuery.setShowAlert(true);
                answerCallbackQuery.setText("Конкурс успешно удален!");
                giveawayBot.execute(answerCallbackQuery);

                sqLiteConn.deleteContest(contestId);

                for (ScheduledPost scheduledPost: giveawayBot.getScheduledPosts()) {
                    if (scheduledPost.getContestId().equals(contestId)) giveawayBot.getScheduledPosts().remove(scheduledPost);
                }


                ResultSet contest = sqLiteConn.getContests(chatId);
                if (contest == null) {
                    messageSender.editMessageText("У вас ещё нет конкурсов!", new InlineMarkupBuilder()
                            .addRow().addButton("Создать новый", "СоздатьКонкурс").build(), chatId, message.getMessageId());
                    return;
                }

                    messageSender.editMessageText(contestObjects.getContestText(contest, 1),
                            contestObjects.getContestKeyboard(contest.getString("contestId"), contest.getString("status"),
                                    1), ParseMode.HTML, chatId, message.getMessageId());
            }

//            case "МоиКонкурсы" -> {
//                giveawayBot.execute(answerCallbackQuery);
//
//                messageSender.deleteMessage(chatId, message.getMessageId());
//                ResultSet contests = sqLiteConn.getContests(chatId);
//                if (contests == null) {
//                    messageSender.sendMessage("У вас ещё нет конкурсов!", chatId);
//                    return;
//                }
//
//                contestId = contests.getString("contestId");
//                String contestName = contests.getString("contestName");
//            }

            case "РазыгратьПлан" -> {
                giveawayBot.execute(answerCallbackQuery);
                dataManager.getUsersData().remove(chatId);

                giveawayBot.getScheduledContests().remove(contestId);

                UserData userData = new UserData(600, BotState.INPUT_RESULTS_DATE);
                userData.setContestId(contestId);
                dataManager.getUsersData().put(chatId, userData);
                messageSender.editMessageText("Введите дату в формате  <code>12.08.2024 15:11</code>",
                        new InlineMarkupBuilder().addRow().addButton("Назад", "КонкурсРазыграть "
                                + contestId).build(), ParseMode.HTML, chatId, message.getMessageId());
            }

            case "КПереиграть" -> {
                giveawayBot.execute(answerCallbackQuery);
                messageSender.editMessageText("Вы уверены что хотите переиграть конкурс?", new InlineMarkupBuilder()
                        .addRow().addButton("Переиграть", "КПереиграть2 " + contestId)
                        .addRow().addButton("Назад", "Конкурс " + contestId).build(), chatId, message.getMessageId());
            }

            case "КПереиграть2" -> {
                ResultSet contest = sqLiteConn.getContest(contestId);

                int participants = contest.getInt("participants");
                int prizes = contest.getInt("prizes");

                if (prizes > participants) {
                    answerCallbackQuery.setShowAlert(true);
                    answerCallbackQuery.setText("Участников слишком мало для розыгрыша!");
                    giveawayBot.execute(answerCallbackQuery);
                    return;
                }
                giveawayBot.execute(answerCallbackQuery);

                sqLiteConn.resetContestWinners(contestId);

                ResultSet participant = sqLiteConn.getParticipants(contestId);
                Random random = new Random();
                Set<Integer> winners = new HashSet<>();

                while (winners.size() < prizes) {
                    int winner = random.nextInt(participants) + 1;
                    winners.add(winner);
                }

                StringBuilder sb = new StringBuilder();
                while (participant.next()) {
                    if (winners.contains(participant.getRow())) {
                        sqLiteConn.setContestWinner(contestId, participant.getLong("userId"));
                        sb.append(" @").append(participant.getString("username"));
                    }
                }

                giveawayBot.getScheduledContests().remove(contestId);

                messageSender.deleteMessage(chatId, message.getMessageId());
                messageSender.sendMessage("Случайным образом были выбраны победители:" + sb,
                        new InlineMarkupBuilder().addRow().addButton("К конкурсу", "Конкурс " + contestId).build(), chatId);
            }

            case "ПостПропустить" -> {
                giveawayBot.execute(answerCallbackQuery);

                UserData userData = dataManager.getUsersData().get(chatId);
                sqLiteConn.setPostMessage(userData.getPostText(), userData.getPostMediaType(), userData.getPostMediaId(),
                        "Учавствовать", contestId);

                messageSender.deleteMessage(chatId, message.getMessageId());
                giveawayBot.sendPost(userData.getPostText(), userData.getPostMediaType(), userData.getPostMediaId(), contestId, chatId);
            }

            case "СоздатьКонкурс" -> {
                giveawayBot.execute(answerCallbackQuery);
                messageSender.deleteMessage(chatId, message.getMessageId());
                messageSender.sendMessage("Введите название для конкурса \n(макс. длина - 16 символов)",
                        new InlineMarkupBuilder().addRow().addButton("Отмена", "КонкурсОтмена").build(), chatId);
                dataManager.getUsersData().put(chatId, new UserData(600, BotState.INPUT_CONTEST_NAME));
            }

            case "КонкурсОтмена" -> {
                dataManager.getUsersData().remove(chatId);

                answerCallbackQuery.setShowAlert(true);
                answerCallbackQuery.setText("Отменено!");
                giveawayBot.execute(answerCallbackQuery);
                messageSender.deleteMessage(chatId, message.getMessageId());
            }

            case "КонкурсКапча" -> {
                giveawayBot.execute(answerCallbackQuery);

                String captchaEmoji;
                boolean isCaptchaEnabled = sqLiteConn.switchCaptcha(contestId);
                if (isCaptchaEnabled) captchaEmoji = "✅";
                else captchaEmoji = "❌";

                messageSender.editMessageText("Настройки конкурса \"<b>" + sqLiteConn.getContest(contestId)
                                .getString("contestName") + "</b>\"", new InlineMarkupBuilder()
                                .addRow().addButton("Капча  " + captchaEmoji, "КонкурсКапча " + contestId)
                                .addRow().addButton("Призовые места", "КМеста " + contestId)
                                .addRow().addButton("Макс. участников", "КМаксУчастников " + contestId)
                                .addRow().addButton("Назад", "Конкурс " + contestId).build(), ParseMode.HTML, chatId,
                        message.getMessageId());
            }

            case "Капча" -> {
                answerCallbackQuery.setShowAlert(true);
                boolean isRight = Boolean.parseBoolean(data[2]);
                if (!isRight) {
                    answerCallbackQuery.setText("Капча не пройдена!");
                    giveawayBot.execute(answerCallbackQuery);
                    return;
                }

                messageSender.editMessageText("✅", null, chatId, message.getMessageId());

                ResultSet contest = sqLiteConn.getContest(contestId);
                if (contest == null || contest.getString("status").equals("finished")) {
                    answerCallbackQuery.setText("Конкурс недействителен, возможно он был завершен или удален!");
                    giveawayBot.execute(answerCallbackQuery);
                    return;
                }

                int maxParticipants = contest.getInt("maxParticipants");
                if (maxParticipants != 0 && contest.getInt("participants") + 1 > maxParticipants) {
                    answerCallbackQuery.setText("Достигнут лимит по участникам!");
                    giveawayBot.execute(answerCallbackQuery);
                    return;
                }

                if (sqLiteConn.getContestParticipant(chatId, contestId) != null) {
                    answerCallbackQuery.setText("Вы уже учавствуете в конкурсе!");
                    giveawayBot.execute(answerCallbackQuery);
                    return;
                }

                sqLiteConn.addContestParticipant(callbackQuery.getFrom(), contestId);

//                InlineKeyboardMarkup keyboardMarkup = giveawayBot.parseKeyboard(contest.getString("postKeyboard"),
//                        participants, contestId, true);
//
//                ResultSet post = sqLiteConn.getParticipationPosts(contestId);
//                while (post.next()) {
//                    EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
//                    editMessageReplyMarkup.setInlineMessageId(post.getString("inlineMessageId"));
//                    editMessageReplyMarkup.setReplyMarkup(keyboardMarkup);
//                    giveawayBot.execute(editMessageReplyMarkup);
//                }
                answerCallbackQuery.setText("Теперь вы учавствуете в конкурсе!");
                giveawayBot.execute(answerCallbackQuery);
            }

            case "AddPost" -> {
                System.out.println("AddParticipationPost");
                sqLiteConn.addParticipationPost(contestId, callbackQuery.getInlineMessageId());
            }

            case "ОбновитьП" -> {
                answerCallbackQuery.setShowAlert(true);
                answerCallbackQuery.setText("Обновляю посты!");
                giveawayBot.execute(answerCallbackQuery);

                ResultSet contest = sqLiteConn.getContest(contestId);
                String postText = contest.getString("postText")
                        .replace("{members}", String.valueOf(contest.getInt("participants")))
                        .replace("{max_members}", String.valueOf(contest.getInt("maxParticipants")));
                String postMediaId = contest.getString("postMediaId");
                String postMediaType = contest.getString("postMediaType");
                InlineKeyboardMarkup postKeyboard = giveawayBot.parseKeyboard(contest.getString("postKeyboard"),
                        contest.getInt("participants"), contestId, true);

                ResultSet post = sqLiteConn.getParticipationPosts(contestId);
                switch (postMediaType) {
                    case "Photo" -> {
                        while (post.next()) {
                            String inlineMessageId = post.getString("inlineMessageId");
                            EditMessageMedia editMessageMedia = new EditMessageMedia();
                            editMessageMedia.setInlineMessageId(inlineMessageId);
                            editMessageMedia.setReplyMarkup(postKeyboard);

                            InputMediaPhoto inputMediaPhoto = new InputMediaPhoto();
                            inputMediaPhoto.setMedia(postMediaId);
                            inputMediaPhoto.setCaption(postText);
                            inputMediaPhoto.setParseMode(ParseMode.HTML);

                            editMessageMedia.setMedia(inputMediaPhoto);
                            try { giveawayBot.execute(editMessageMedia); }
                            catch (TelegramApiException ex) {
                                if (ex.getMessage().endsWith("MESSAGE_ID_INVALID"))
                                    sqLiteConn.deleteParticipationPost(contestId, inlineMessageId);
                            }
                        }
                    }
                    case "Video" -> {
                        while (post.next()) {
                            String inlineMessageId = post.getString("inlineMessageId");
                            EditMessageMedia editMessageMedia = new EditMessageMedia();
                            editMessageMedia.setInlineMessageId(inlineMessageId);
                            editMessageMedia.setReplyMarkup(postKeyboard);

                            InputMediaVideo inputMediaVideo = new InputMediaVideo();
                            inputMediaVideo.setMedia(postMediaId);
                            inputMediaVideo.setCaption(postText);
                            inputMediaVideo.setParseMode(ParseMode.HTML);

                            editMessageMedia.setMedia(inputMediaVideo);
                            try { giveawayBot.execute(editMessageMedia); }
                            catch (TelegramApiException ex) {
                                if (ex.getMessage().endsWith("MESSAGE_ID_INVALID"))
                                    sqLiteConn.deleteParticipationPost(contestId, inlineMessageId);
                            }
                        }
                    }
                    case "Animation" -> {
                        while (post.next()) {
                            String inlineMessageId = post.getString("inlineMessageId");
                            EditMessageMedia editMessageMedia = new EditMessageMedia();
                            editMessageMedia.setInlineMessageId(inlineMessageId);
                            editMessageMedia.setReplyMarkup(postKeyboard);

                            InputMediaAnimation inputMediaAnimation = new InputMediaAnimation();
                            inputMediaAnimation.setMedia(postMediaId);
                            inputMediaAnimation.setCaption(postText);
                            inputMediaAnimation.setParseMode(ParseMode.HTML);

                            editMessageMedia.setMedia(inputMediaAnimation);
                            try { giveawayBot.execute(editMessageMedia); }
                            catch (TelegramApiException ex) {
                                if (ex.getMessage().endsWith("MESSAGE_ID_INVALID"))
                                    sqLiteConn.deleteParticipationPost(contestId, inlineMessageId);
                            }
                        }
                    }
                    default -> {
                        while (post.next()) {
                            String inlineMessageId = post.getString("inlineMessageId");
                            EditMessageText editMessageText = new EditMessageText();
                            editMessageText.setInlineMessageId(inlineMessageId);
                            editMessageText.setReplyMarkup(postKeyboard);
                            editMessageText.setText(postText);
                            editMessageText.setParseMode(ParseMode.HTML);
                            try { giveawayBot.execute(editMessageText); }
                            catch (TelegramApiException ex) {
                                if (ex.getMessage().endsWith("MESSAGE_ID_INVALID"))
                                    sqLiteConn.deleteParticipationPost(contestId, inlineMessageId);
                            }
                        }
                    }
                }
            }

            default -> giveawayBot.execute(answerCallbackQuery);

        }

    }

    private void sendSubAllGroups(ResultSet group, String contestId, long chatId) throws SQLException {
        InlineMarkupBuilder inlineMarkupBuilder = new InlineMarkupBuilder();
        inlineMarkupBuilder.addRow().addButton(group.getString("groupName"), "КонкурсПодписка2 "
                + contestId + " " + group.getString("groupId"));

        while (group.next()) {
            inlineMarkupBuilder.addRow().addButton(group.getString("groupName"), "КонкурсПодписка2 "
                    + contestId + " " + group.getLong("groupId"));
        }
        inlineMarkupBuilder.addRow().addButton("Назад", "Конкурс " + contestId);

        messageSender.sendMessage("Выберите чат для проверки подписки",
                inlineMarkupBuilder.build(), chatId);
    }

    private void sendSubAddedGroups(ResultSet subCheck, String contestId, long chatId) throws SQLException {
        InlineMarkupBuilder inlineMarkupBuilder = new InlineMarkupBuilder();
        long groupId = subCheck.getLong("groupId");
        String groupName = sqLiteConn.getGroup(groupId).getString("groupName");

        inlineMarkupBuilder.addRow().addButton(groupName, "Null")
                .addButton("❌", "ПодпискаУдалить " + contestId + " " + groupId);

        while (subCheck.next()) {
            groupId = subCheck.getLong("groupId");
            groupName = sqLiteConn.getGroup(groupId).getString("groupName");
            inlineMarkupBuilder.addRow().addButton(groupName, "Null")
                    .addButton("❌", "ПодпискаУдалить " + contestId + " " + groupId);
        }

        inlineMarkupBuilder.addRow().addButton("➕", "ПодпискаДобавить " + contestId)
                .addRow().addButton("Назад", "Конкурс " + contestId);

        messageSender.sendMessage("Чаты в которых проверяется подписка",
                inlineMarkupBuilder.build(), chatId);
    }

}
