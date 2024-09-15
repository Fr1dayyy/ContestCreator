package ru.bluegem.telegrambot.containers;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.bluegem.telegrambot.GiveawayBot;
import ru.bluegem.telegrambot.botapi.markups.InlineMarkupBuilder;
import ru.bluegem.telegrambot.schedule.ScheduledContest;
import ru.bluegem.telegrambot.sqlite.SQLiteConn;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

public class ContestObjects {

    private final GiveawayBot giveawayBot;
    private final SQLiteConn sqLiteConn;

    public ContestObjects(GiveawayBot giveawayBot, SQLiteConn sqLiteConn) {
        this.giveawayBot = giveawayBot;
        this.sqLiteConn = sqLiteConn;
    }

    public String getContestText(ResultSet contest, int contestNum) throws SQLException {
        String contestId = contest.getString("contestId");
        String middleSection;
        String status = contest.getString("status");
        if (status.equals("active")) {
            status = "Активен";
            middleSection = " Призовых мест: " + contest.getInt("prizes");
        }
        else {
            ResultSet winner = sqLiteConn.getContestWinners(contestId);
            status = "Завершён";
            StringBuilder sb = new StringBuilder();
            while (winner.next()) {
                sb.append(" @").append(winner.getString("username"));
            }
            middleSection = " Призовых мест: " + contest.getInt("prizes")
                    + "\n Победители:" + sb;
        }

        String maxP;
        int maxParticipants = contest.getInt("maxParticipants");
        if (maxParticipants != 0) maxP = String.valueOf(maxParticipants);
        else maxP = "∞";

        String num;
        if (contestNum != -1) num = "(№" + contestNum + ")";
        else num = "";

        ScheduledContest scheduledContest = giveawayBot.getScheduledContests().get(contestId);
        if (scheduledContest != null) {
            middleSection = middleSection + "\n<i>Розыгрыш запланирован на " + scheduledContest.getScheduleDateStr() + "</i>";
        }

        return "Конкурс \"<b>" + contest.getString("contestName") + "</b>\" " + num + "\n" +
                " Ключ <code>" + contestId + "</code> \n" +
                " \n" +
                " Участников " + contest.getInt("participants") + " из " + maxP + "\n" +
                middleSection + "\n" +
                "\n" +
                "Статус <b>" + status + "</b>";
    }

//    public String getContestText(String contestName, int contestNum, String contestId, String status, int participants,
//                                 String maxParticipants) {
//        if (status.equals("active")) status = "Активен";
//        else status = "Завершён";
//        return "Конкурс \"<b>" + contestName + "</b>\" (№" + contestNum + ")\n" +
//                " Ключ <code>" + contestId + "</code> \n" +
//                "\n" +
//                " Участников " + participants + " из " + maxParticipants + "\n" +
//                " Призовых мест: 1\n" +
//                "\n" +
//                "Статус  <b>" + status + "</b>";
//    }

    public InlineKeyboardMarkup getContestKeyboard(String contestId, String status, int contestNum) {
        InlineMarkupBuilder inlineMarkupBuilder = new InlineMarkupBuilder().addRow();
        if (status.equals("active")) inlineMarkupBuilder.addButton("Разыграть", "КонкурсРазыграть " + contestId);
        else inlineMarkupBuilder.addButton("Переиграть", "КПереиграть " + contestId);

        inlineMarkupBuilder.addButton("Запланировать", "РазыгратьПлан " + contestId)
                .addRow().addButton("Пост в группу", "КонкурсПост " + contestId)
                .addButton("Подписка", "КонкурсПодписка " + contestId)
                .addRow().addButton("Участники", "ExcelТаблица " + contestId)
                .addRow().addButton("Настройки", "КНастройки " + contestId)
                .addRow().addButton("Удалить", "УдалитьКонкурс1 " + contestId)
                .addRow().addButton("⏪", "Дальше first " + contestNum)
                .addButton("⬅️", "Дальше " + (contestNum-1))
                .addButton("➡️", "Дальше " + (contestNum+1))
                .addButton("⏩", "Дальше max " + contestNum);

        return inlineMarkupBuilder.build();
    }


    public String getGroupText(String groupName, int groupNum, String groupId) {
        return "<b>Группа</b> \"<b>" + groupName + "</b>\" (№" + groupNum + ")\n" +
                " ID  <code>" + groupId + "</code>\n";
    }

    public InlineKeyboardMarkup getGroupKeyboard(String groupId, int groupNum) {
        InlineMarkupBuilder inlineMarkupBuilder = new InlineMarkupBuilder().addRow();
        inlineMarkupBuilder.addButton("Удалить", "УдалитьГруппу " + groupId)
                .addRow().addButton("⏪", "ГруппаДальше first " + groupNum)
                .addButton("⬅️", "ГруппаДальше " + (groupNum-1))
                .addButton("➡️", "ГруппаДальше " + (groupNum+1))
                .addButton("⏩", "ГруппаДальше max " + groupNum)
                .addRow().addButton("➕", "ДобавитьГруппу");
        return inlineMarkupBuilder.build();
    }

    public String getContestFinishedText(ResultSet contest, ResultSet winner) throws SQLException {
        String contestName = contest.getString("contestName");
        int participants = contest.getInt("participants");
        int maxParticipants = contest.getInt("maxParticipants");
        int prizes = contest.getInt("prizes");

        StringBuilder sb = new StringBuilder();
        do {
            sb.append(" @").append(winner.getString("username"));
        } while (winner.next());

        return "Конкурс \"" + contestName + "\" завершен! \nУчастников " + participants + " из " + maxParticipants
                + "\nПризовых мест: " + prizes + "\n\nПобедители:" + sb;
    }

    public String getNoGroupsText() {
        return "<b>У вас ещё нет групп</b> \n\nВы можете добавить бота в чат," +
                "\nиспользуя кнопки <code>Канал</code> или <code>Группа</code> \n\n‼️ У бота обязательно " +
                "должны быть права на отправку, редактирование и удаление сообщений";
    }

    public InlineKeyboardMarkup getNoGroupsKeyboard() {
        return new InlineMarkupBuilder().addRow()
                .addUrlButton("Канал", "Null", "https://t.me/ContestCreatorBot?startchannel=true")
                .addUrlButton("Группа", "Null", "https://t.me/ContestCreatorBot?startgroup=true")
                .build();
    }

}
