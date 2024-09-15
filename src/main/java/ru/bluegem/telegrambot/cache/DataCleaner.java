package ru.bluegem.telegrambot.cache;



import ru.bluegem.telegrambot.botapi.messages.MessageSender;

import java.util.*;

public class DataCleaner {

    private final DataManager dataManager;
    private final MessageSender messageSender;

    private final Timer timer = new Timer();
    private TimerTask timerTask1, timerTask2;

    public DataCleaner(DataManager dataManager, MessageSender messageSender) {
        this.dataManager = dataManager;
        this.messageSender = messageSender;
    }

    public void startCleaner() {

        timerTask1 = new TimerTask() {
            @Override
            public void run() {
                Map<Long, UserData> usersData = dataManager.getUsersData();
                Date actualDate = new Date();

                for (Long userId: new HashSet<>(usersData.keySet())) {
                    if (usersData.get(userId).getExpiredDate().compareTo(actualDate) >= 0) {
                        messageSender.sendMessage("Время на ввод истекло!", userId);
                        dataManager.getUsersData().remove(userId);
                    }
                }
            }
        };

        timer.scheduleAtFixedRate(timerTask1, 3600 * 1000L, 3600 * 1000L);


    }


}
