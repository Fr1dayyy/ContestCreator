package ru.bluegem.telegrambot.botapi.markups;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public class ReplyMarkupBuilder implements ReplyKeyboardBuilder {

    private final ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
    private final List<KeyboardRow> keyboardRows = new ArrayList<>(125);
    private short rows;

    public ReplyMarkupBuilder addButton(@NotNull String text) {
        if (rows == 0) throw new RuntimeException("cannot add button without rows");
        if (text == null) throw new NullPointerException("text is marked non-null but is null");
        if (keyboardRows.get(rows - 1).size() == 125) throw new RuntimeException("maximum value of buttons");

        KeyboardRow keyboardRow = keyboardRows.get(rows-1);

        KeyboardButton keyboardButton = new KeyboardButton();
        keyboardButton.setText(text);

        keyboardRow.add(keyboardButton);
        return this;
    }

    public ReplyMarkupBuilder addRow() {
        if (rows == 125) throw new RuntimeException("maximum value of rows");

        keyboardRows.add(new KeyboardRow(125));
        rows++;
        return this;
    }

    public ReplyKeyboardMarkup build() {
        replyKeyboardMarkup.setResizeKeyboard(true);

        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;
    }

}
