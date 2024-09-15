package ru.bluegem.telegrambot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.bluegem.telegrambot.containers.ContestObjects;
import ru.bluegem.telegrambot.sqlite.SQLiteConn;
import ru.bluegem.telegrambot.botapi.markups.InlineMarkupBuilder;
import ru.bluegem.telegrambot.botapi.messages.MessageSender;
import ru.bluegem.telegrambot.cache.DataManager;
import ru.bluegem.telegrambot.handlers.CallbackHandler;
import ru.bluegem.telegrambot.handlers.InlineHandler;
import ru.bluegem.telegrambot.handlers.MessageHandler;
import ru.bluegem.telegrambot.schedule.ScheduledContest;
import ru.bluegem.telegrambot.schedule.ScheduledPost;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GiveawayBot extends TelegramLongPollingBot {

    private final MessageSender messageSender;
    private final SQLiteConn sqLiteConn;
    private final DataManager dataManager;

    private final ContestObjects contestObjects;

    private final CallbackHandler callbackHandler;
    private final MessageHandler messageHandler;
    private final InlineHandler inlineHandler;


    private final Set<ScheduledPost> scheduledPosts;
    private final Map<String, ScheduledContest> scheduledContests;

    public GiveawayBot() {
        super("6692065859:AAG0tyeTsrYg5BcZE0_ZNs1J9rRyDDObJS0");
        this.messageSender = new MessageSender(this);
        this.sqLiteConn = new SQLiteConn();
        this.dataManager = new DataManager(messageSender);
        this.contestObjects = new ContestObjects(this, sqLiteConn);
        this.callbackHandler = new CallbackHandler(this, messageSender, sqLiteConn, dataManager);
        this.messageHandler = new MessageHandler(this, messageSender, sqLiteConn, dataManager);
        this.inlineHandler = new InlineHandler(this, messageSender, sqLiteConn);
        scheduledPosts = new HashSet<>();
        scheduledContests = new HashMap<>();
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("receiving update");

        if (update.hasInlineQuery()) {
            try {
                System.out.println("handling inlineQuery");
                inlineHandler.handleInlineQuery(update);
            } catch (TelegramApiException | SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            return;
        }

        if (update.hasCallbackQuery()) {
            try {
                System.out.println("handling callbackQuery");
                callbackHandler.handleCallback(update);
            } catch (TelegramApiException | SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            return;
        }

        if (update.hasMyChatMember()) {
            ChatMemberUpdated chatMemberUpdated = update.getMyChatMember();
            ChatMember chatMember = chatMemberUpdated.getNewChatMember();
            User user = chatMember.getUser();
            if (user.getUserName().equals("ContestCreatorBot")) {
                if (chatMember.getStatus().equals("administrator")) {
                    long ownerId = chatMemberUpdated.getFrom().getId();
                    Chat chat = chatMemberUpdated.getChat();
                    sqLiteConn.addGroup(ownerId, chat.getId(), chat.getTitle());
                    messageSender.sendMessage("Добавлен чат \"<b>" + chat.getTitle() + "</b>\"",
                            ParseMode.HTML, ownerId);
                    return;
                }
                return;
            }
        }

        System.out.println("handling message");
        try {
            messageHandler.handleMessage(update);
        } catch (SQLException | TelegramApiException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    public InlineKeyboardMarkup parseKeyboard(String postKeyboard, int participants, String contestId, boolean isUrlButton) {
        if (postKeyboard == null) return null;
        
        String callbackData = null;
        String url = null;
        if (isUrlButton) url = "https://t.me/ContestCreatorBot?start=" + contestId + "-Add";
        else callbackData = "Учавствовать " + contestId;

        InlineMarkupBuilder inlineMarkupBuilder = new InlineMarkupBuilder();

        String[] rows = postKeyboard.split("\n");
        for (String s : rows) {
            String[] row = s.split(" ");
            inlineMarkupBuilder.addRow();
            for (String str : row) {
                if (str.equals("Учавствовать")) {
                    inlineMarkupBuilder.addUrlButton("[" + participants + "] Учавствовать",
                            callbackData, url);
                    continue;
                }
                String[] button = str.split("=");
                inlineMarkupBuilder.addUrlButton(button[0], "Null", button[1]);
            }
        }

        return inlineMarkupBuilder.build();
    }

    public void sendPost(String postText, String postMediaType, String postMediaId, String contestId, long chatId) throws SQLException {
        InlineKeyboardMarkup editPostKeyboard = new InlineMarkupBuilder().addRow()
                .addButton("Редактировать", "РедактироватьПост " + contestId)
                .addRow().addInlineButton("Выложить", contestId)
                .addButton("Запланировать", "ЗапланироватьПост " + contestId)
                .addRow().addButton("Запланированные", "ЗапланированныеП " + contestId)
                .addRow().addButton("Обновить посты", "ОбновитьП " + contestId)
                .addRow().addButton("Назад", "Конкурс " + contestId).build();

        ResultSet contest = sqLiteConn.getContest(contestId);

        int maxParticipants = contest.getInt("maxParticipants");
        String maxP;
        if (maxParticipants == 0) maxP = "∞";
        else maxP = String.valueOf(maxParticipants);

        postText = postText.replace("{members}", String.valueOf(contest.getInt("participants")))
                .replace("{max_members}", maxP)
                .replace("{prizes}", String.valueOf(contest.getInt("prizes")));

        switch (postMediaType) {
            case "Photo" -> {
                messageSender.sendPhoto(new InputFile(postMediaId), postText, editPostKeyboard, chatId);
            }
            case "Video" -> {
                messageSender.sendVideo(new InputFile(postMediaId), postText, editPostKeyboard, chatId);
            }
            case "Animation" -> {
                messageSender.sendAnimation(new InputFile(postMediaId), postText, editPostKeyboard, chatId);
            }
            default -> {
                if (postText == null) postText = "<b>Пост ещё не настроен!</b>\n" +
                        "Воспользуйтесь клавиатурой ниже для того чтобы создать и выложить пост";
                messageSender.sendMessage(postText, editPostKeyboard, ParseMode.HTML, chatId);
            }
        }
    }

    @Override
    public void onRegister() {
        super.onRegister();
        try {
            sqLiteConn.connect("giveawayDb.s3db");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        dataManager.getDataCleaner().startCleaner();
    }

    @Override
    public String getBotUsername() {
        return "@ContestCreatorBot";
    }

    public ContestObjects getContestObjects() {
        return contestObjects;
    }

    public Set<ScheduledPost> getScheduledPosts() {
        return scheduledPosts;
    }

    public Map<String, ScheduledContest> getScheduledContests() {
        return scheduledContests;
    }

}
