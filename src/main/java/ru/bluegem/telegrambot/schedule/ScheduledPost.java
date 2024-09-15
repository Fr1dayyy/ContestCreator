package ru.bluegem.telegrambot.schedule;

import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.bluegem.telegrambot.GiveawayBot;
import ru.bluegem.telegrambot.sqlite.SQLiteConn;
import ru.bluegem.telegrambot.botapi.markups.InlineMarkupBuilder;
import ru.bluegem.telegrambot.botapi.messages.MessageSender;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class ScheduledPost {

    private final GiveawayBot giveawayBot;

    private final MessageSender messageSender;
    private final SQLiteConn sqLiteConn;

    private final String contestId;
    private final long chatId;
    private final Date scheduleDate;

    private final Timer timer = new Timer();

    public ScheduledPost(GiveawayBot giveawayBot, MessageSender messageSender, SQLiteConn sqLiteConn, String contestId,
                         long chatId, Date scheduleDate) {
        this.giveawayBot = giveawayBot;
        this.messageSender = messageSender;
        this.sqLiteConn = sqLiteConn;
        this.contestId = contestId;
        this.chatId = chatId;
        this.scheduleDate = scheduleDate;
    }

    public void schedule() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                sendPost();
            }
        };
        timer.schedule(timerTask, scheduleDate);
    }

    private void sendPost() {
        ResultSet contest = sqLiteConn.getContest(contestId);

        String postText;
        String postMediaType;
        String postMediaId;
        String postKeyboard;
        int participants;
        try {
            postMediaType = contest.getString("postMediaType");
            postMediaId = contest.getString("postMediaId");
            postKeyboard = contest.getString("postKeyboard");
            participants = contest.getInt("participants");

            int maxParticipants = contest.getInt("maxParticipants");
            String maxP;
            if (maxParticipants == 0) maxP = "∞";
            else maxP = String.valueOf(maxParticipants);

            postText = contest.getString("postText").replace("{members}", String.valueOf(participants))
                    .replace("{max_members}", maxP)
                    .replace("{prizes}", String.valueOf(contest.getInt("prizes")));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        InlineKeyboardMarkup keyboardMarkup = giveawayBot.parseKeyboard(postKeyboard, participants, contestId, false);

        switch (postMediaType) {
            case "Photo" -> {
                messageSender.sendPhoto(new InputFile(postMediaId), postText, keyboardMarkup, chatId);
            }
            case "Video" -> {
                messageSender.sendVideo(new InputFile(postMediaId), postText, keyboardMarkup, chatId);
            }
            case "Animation" -> {
                messageSender.sendAnimation(new InputFile(postMediaId), postText, keyboardMarkup, chatId);
            }
            default -> {
                messageSender.sendMessage(postText, keyboardMarkup, chatId);
            }
        }
        giveawayBot.getScheduledPosts().remove(this);
    }

    public String getContestId() {
        return contestId;
    }

    public long getChatId() {
        return chatId;
    }

    public Date getScheduleDate() {
        return scheduleDate;
    }

    public Timer getTimer() {
        return timer;
    }

}
