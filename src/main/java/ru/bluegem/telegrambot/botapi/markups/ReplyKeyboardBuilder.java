package ru.bluegem.telegrambot.botapi.markups;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

public interface ReplyKeyboardBuilder {

    ReplyKeyboardBuilder addRow();
    ReplyKeyboard build();

}
