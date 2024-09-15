package ru.bluegem.telegrambot.schedule;

import ru.bluegem.telegrambot.GiveawayBot;
import ru.bluegem.telegrambot.botapi.markups.InlineMarkupBuilder;
import ru.bluegem.telegrambot.sqlite.SQLiteConn;
import ru.bluegem.telegrambot.botapi.messages.MessageSender;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ScheduledContest {

    private final GiveawayBot giveawayBot;

    private final MessageSender messageSender;
    private final SQLiteConn sqLiteConn;

    private final String contestId;
    private final long chatId;
    private final Date scheduleDate;
    private final String scheduleDateStr;

    public ScheduledContest(GiveawayBot giveawayBot, MessageSender messageSender, SQLiteConn sqLiteConn, String contestId,
                            long chatId, Date scheduleDate, String scheduleDateStr) {
        this.giveawayBot = giveawayBot;
        this.messageSender = messageSender;
        this.sqLiteConn = sqLiteConn;
        this.contestId = contestId;
        this.chatId = chatId;
        this.scheduleDate = scheduleDate;
        this.scheduleDateStr = scheduleDateStr;
    }

    public void schedule() {
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    finishContest();
                } catch (SQLException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        };
        timer.schedule(timerTask, scheduleDate);
    }

    private void finishContest() throws SQLException {
        ResultSet contest = sqLiteConn.getContest(contestId);

        int participants = contest.getInt("participants");
        int prizes = contest.getInt("prizes");

        if (prizes > participants) {
            messageSender.sendMessage("Участников слишком мало для розыгрыша!", chatId);
            return;
        }

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

        messageSender.sendMessage("Случайным образом были выбраны победители:" + sb,
                new InlineMarkupBuilder().addRow().addButton("К конкурсу", "Конкурс " + contestId).build(), chatId);
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

    public String getScheduleDateStr() {
        return scheduleDateStr;
    }

}
