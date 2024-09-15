package ru.bluegem.telegrambot.handlers;

import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.cached.InlineQueryResultCachedGif;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.cached.InlineQueryResultCachedPhoto;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.cached.InlineQueryResultCachedVideo;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.bluegem.telegrambot.sqlite.SQLiteConn;
import ru.bluegem.telegrambot.GiveawayBot;
import ru.bluegem.telegrambot.botapi.markups.InlineMarkupBuilder;
import ru.bluegem.telegrambot.botapi.messages.MessageSender;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InlineHandler {

    private final GiveawayBot giveawayBot;
    private final MessageSender messageSender;
    private final SQLiteConn sqLiteConn;

    public InlineHandler(GiveawayBot giveawayBot, MessageSender messageSender, SQLiteConn sqLiteConn) {
        this.giveawayBot = giveawayBot;
        this.messageSender = messageSender;
        this.sqLiteConn = sqLiteConn;
    }

    public void handleInlineQuery(Update update) throws TelegramApiException, SQLException {
        System.out.println(update);
        InlineQuery inlinequery = update.getInlineQuery();
        String query = inlinequery.getQuery();
        System.out.println(query);

        ResultSet contest = sqLiteConn.getContest(query);

        List<InlineQueryResult> results = new ArrayList<>();
        if (contest == null) {
            InlineQueryResultArticle result = new InlineQueryResultArticle();
            result.setTitle("Конкурс с ID '" + query + "' не найден!");
            InputTextMessageContent inputTextMessageContent = new InputTextMessageContent();
            inputTextMessageContent.setMessageText("Конкурс не найден!");
            result.setInputMessageContent(inputTextMessageContent);
            result.setId("1");
            results.add(result);
        }
        else {
            String contestName = contest.getString("contestName");
            String status = contest.getString("status");
            int participants = contest.getInt("participants");
            if (status.equals("finished")) {
                InlineQueryResultArticle result = new InlineQueryResultArticle();
                result.setTitle("Конкурс '" + contestName + "'");
                InputTextMessageContent inputTextMessageContent = new InputTextMessageContent();

                ResultSet winner = sqLiteConn.getContestWinners(query);
                StringBuilder sb = new StringBuilder();
                while (winner.next()) {
                    sb.append(" @").append(winner.getString("username"));
                }

                inputTextMessageContent.setMessageText("Конкурс завершен! \nУчастники: " + participants
                        + "\n\nПобедители: " + sb + "\n\n<a href=\"https://t.me/ContestCreatorBot?start=" + query
                        + "\">Проверить результаты</a>");

                inputTextMessageContent.setParseMode(ParseMode.HTML);
                result.setInputMessageContent(inputTextMessageContent);
                result.setId("1");
                results.add(result);
            }
            else {

                int maxParticipants = contest.getInt("maxParticipants");
                String maxP;
                if (maxParticipants == 0) maxP = "∞";
                else maxP = String.valueOf(maxParticipants);

                String postText = contest.getString("postText")
                        .replace("{members}", String.valueOf(participants))
                        .replace("{max_members}", maxP)
                        .replace("{prizes}", String.valueOf(contest.getInt("prizes")));
                String postMediaType = contest.getString("postMediaType");
                String postMediaId = contest.getString("postMediaId");

                String postKeyboard = contest.getString("postKeyboard");

                InlineKeyboardMarkup keyboardMarkup = giveawayBot.parseKeyboard(postKeyboard, participants, query, false);

                switch (postMediaType) {
                    case "Photo" -> {
                        InlineQueryResultCachedPhoto result = new InlineQueryResultCachedPhoto();
                        result.setPhotoFileId(postMediaId);
                        result.setId("1");
                        result.setCaption(postText);
                        result.setParseMode(ParseMode.HTML);
                        result.setReplyMarkup(keyboardMarkup);
                        results.add(result);
                    }
                    case "Video" -> {
                        InlineQueryResultCachedVideo result = new InlineQueryResultCachedVideo();
                        result.setVideoFileId(postMediaId);
                        result.setId("1");
                        result.setCaption(postText);
                        result.setParseMode(ParseMode.HTML);
                        result.setReplyMarkup(keyboardMarkup);
                        results.add(result);
                    }
                    case "Animation" -> {
                        InlineQueryResultCachedGif result = new InlineQueryResultCachedGif();
                        result.setGifFileId(postMediaId);
                        result.setId("1");
                        result.setCaption(postText);
                        result.setParseMode(ParseMode.HTML);
                        result.setReplyMarkup(keyboardMarkup);
                        results.add(result);
                    }
                    default -> {
                        InlineQueryResultArticle result = new InlineQueryResultArticle();
                        String title;
                        if (postText == null) {
                            postText = "Пост не настроен!";
                            title = "Пост для конкурса '" + contestName + "' не настроен!";
                        } else title = "Конкурс '" + contestName + "'";
                        result.setTitle(title);
                        result.setId("1");
                        result.setReplyMarkup(keyboardMarkup);
                        InputTextMessageContent inputTextMessageContent = new InputTextMessageContent();
                        inputTextMessageContent.setMessageText(postText);
                        inputTextMessageContent.setParseMode(ParseMode.HTML);
                        result.setInputMessageContent(inputTextMessageContent);
                        results.add(result);
                    }
                }
            }

        }
        AnswerInlineQuery answerInlineQuery = new AnswerInlineQuery();
        answerInlineQuery.setInlineQueryId(inlinequery.getId());
        answerInlineQuery.setCacheTime(1);
        answerInlineQuery.setResults(results);
        giveawayBot.execute(answerInlineQuery);

    }

}
