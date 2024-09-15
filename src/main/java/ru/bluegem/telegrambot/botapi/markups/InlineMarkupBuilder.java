package ru.bluegem.telegrambot.botapi.markups;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public class InlineMarkupBuilder implements ReplyKeyboardBuilder {

    private final InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
    private final List<List<InlineKeyboardButton>> keyboardView = new ArrayList<>(125);
    private byte rows;


    public InlineMarkupBuilder addButton(@NotNull String text, @NotNull String callbackData) {
        final InlineKeyboardButton keyboardButton = createButton(text, callbackData);
        keyboardView.get(rows - 1).add(keyboardButton);
        return this;
    }

    public InlineMarkupBuilder addUrlButton(@NotNull String text, String callbackData, String url) {
        InlineKeyboardButton keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText(text);
        keyboardButton.setCallbackData(callbackData);
        keyboardButton.setUrl(url);
        keyboardView.get(rows - 1).add(keyboardButton);
        return this;
    }

    public InlineMarkupBuilder addInlineButton(@NotNull String text, @NotNull String switchInlineQuery) {
        final InlineKeyboardButton keyboardButton = createButton(text, "");
        keyboardButton.setSwitchInlineQuery(switchInlineQuery);
        keyboardView.get(rows - 1).add(keyboardButton);
        return this;
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        if (rows == 0) throw new RuntimeException("cannot add button without rows");
        if (text == null) throw new NullPointerException("text is marked non-null but is null");
        if (callbackData == null) throw new NullPointerException("callbackData is marked non-null but is null");

        if (keyboardView.get(rows - 1).size() == 125) throw new RuntimeException("maximum value of buttons");

        final InlineKeyboardButton keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText(text);
        keyboardButton.setCallbackData(callbackData);
        return keyboardButton;
    }

    public InlineMarkupBuilder addRow() {
        if (rows == 125) throw new RuntimeException("maximum value of rows");

        final List<InlineKeyboardButton> keyboardStroke = new ArrayList<>(125);
        keyboardView.add(keyboardStroke);
        rows++;
        return this;
    }

    public InlineKeyboardMarkup build() {
        keyboardMarkup.setKeyboard(keyboardView);
        return keyboardMarkup;
    }

//    public List<List<InlineKeyboardButton>> getKeyboardView() {
//        return keyboardView;
//    }
//
    public short getRows() {
        return rows;
    }

//    public class RowBuilder extends InlineMarkupBuilder {
//
//        private short row;
//
//        public RowBuilder(short row) {
//            this.row = row;
//        }
//
//        public RowBuilder addButton(@NotNull String text, @NotNull String callbackData) {
//            System.out.println(keyboardView);
//            final InlineKeyboardButton keyboardButton = new InlineKeyboardButton();
//            keyboardButton.setText(text);
//            keyboardButton.setCallbackData(callbackData);
//            System.out.println(keyboardButton);
//            keyboardView.get(row - 1).add(keyboardButton);
//            System.out.println(keyboardView);
//
//            System.out.println();
//            System.out.println("1:" + keyboardView);
//            System.out.println();
//            System.out.println("2:" + getKeyboardView());
//            System.out.println();
//            return this;
//        }
//
//    }

}
