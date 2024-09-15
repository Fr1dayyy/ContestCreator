package ru.bluegem.telegrambot.cache;


import ru.bluegem.telegrambot.enums.BotState;

import java.util.Date;

public class UserData {

    private final Date expiredDate;
    private final int lifeCycleSeconds;

    private String contestId;
    private String groupId;

    private String postText;
    private String postMediaType;
    private String postMediaId;


    private BotState botState;


    public UserData(int lifeCycleSeconds, BotState botState) {
        this.expiredDate = new Date();
        expiredDate.setTime(expiredDate.getTime() + lifeCycleSeconds*1000L);
        this.lifeCycleSeconds = lifeCycleSeconds;
        this.botState = botState;
    }


    public String getPostText() {
        return postText;
    }

    public void setPostText(String postText) {
        this.postText = postText;
    }

    public String getPostMediaType() {
        return postMediaType;
    }

    public void setPostMediaType(String postMediaType) {
        this.postMediaType = postMediaType;
    }

    public String getPostMediaId() {
        return postMediaId;
    }

    public void setPostMediaId(String postMediaId) {
        this.postMediaId = postMediaId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setContestId(String contestId) {
        this.contestId = contestId;
    }

    public String getContestId() {
        return contestId;
    }

    public void setBotState(BotState botState) {
        this.botState = botState;
    }

    public BotState getBotState() {
        return botState;
    }

    public Date getExpiredDate() {
        return expiredDate;
    }

}
