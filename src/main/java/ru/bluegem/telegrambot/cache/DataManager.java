package ru.bluegem.telegrambot.cache;



import ru.bluegem.telegrambot.botapi.messages.MessageSender;

import java.util.HashMap;
import java.util.Map;

public class DataManager {

    private final DataCleaner dataCleaner;

    private final Map<Long, UserData> usersData = new HashMap<>();


    public DataManager(MessageSender messageSender) {
        dataCleaner = new DataCleaner(this, messageSender);
    }

    public DataCleaner getDataCleaner() {
        return dataCleaner;
    }

    public Map<Long, UserData> getUsersData() {
        return usersData;
    }


}
