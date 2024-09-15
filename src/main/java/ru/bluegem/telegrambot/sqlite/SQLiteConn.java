package ru.bluegem.telegrambot.sqlite;

import org.telegram.telegrambots.meta.api.objects.User;

import java.sql.*;
import java.util.Date;
import java.util.Random;

public class SQLiteConn {

    private Connection connection;
    private Statement statement;

    public void connect(String dbName) throws ClassNotFoundException, SQLException {
        connection = null;
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbName);
        statement = connection.createStatement();

        System.out.println("База Подключена!");
    }

    public void CloseDB() throws ClassNotFoundException, SQLException {
        connection.close();
        statement.close();

        System.out.println("Соединения закрыты");
    }

    public String addContest(String contestName, long creatorId) {
        try {
            String contestId = getContestId();
            statement.execute("INSERT INTO 'contests' (contestName, contestId, creatorId) VALUES ('" + contestName
                    + "', '" + contestId + "', "  + creatorId + ");");
            return contestId;
        } catch (SQLException e) {
            System.out.println("Ошибка при добавлении конкурса!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public ResultSet getContest(String contestId) {
        ResultSet resultSet;
        try {
            resultSet = connection.createStatement().executeQuery("SELECT * FROM 'contests' WHERE contestId = '" + contestId + "'");
            if (!resultSet.next()) return null;
            return resultSet;
        } catch (SQLException e) {
            System.out.println("Ошибка при получении конкурса!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void deleteContest(String contestId) {
        try {
            statement.execute("DELETE FROM 'contests' WHERE contestId = '" + contestId + "'");
            statement.execute("DELETE FROM 'subChecks' WHERE contestId = '" + contestId + "'");
            statement.execute("DELETE FROM 'participants' WHERE contestId = '" + contestId + "'");
            statement.execute("DELETE FROM 'participationPosts' WHERE contestId = '" + contestId + "'");
        } catch (SQLException e) {
            System.out.println("Ошибка при удалении конкурса!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public int addContestParticipant(User user, String contestId) {
        try {
            statement.execute("INSERT INTO 'participants' (userId, contestId, username, firstname, registerDate) " +
                    "VALUES (" + user.getId() + ", '" + contestId + "', '" + user.getUserName() + "', '" + user.getFirstName()
                    + "', '" + new Date() + "');");
            int participants = statement.executeQuery("SELECT * FROM 'contests' WHERE contestId = '" + contestId + "'")
                    .getInt("participants") + 1;
            statement.execute("UPDATE 'contests' SET participants = " + participants + " WHERE contestId = '"
                    + contestId + "'");
            return participants;
        } catch (SQLException e) {
            System.out.println("Ошибка при добавлении участника!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public ResultSet getContestParticipant(long userId, String contestId) {
        ResultSet resultSet;
        try {
            resultSet = connection.createStatement().executeQuery("SELECT * FROM 'participants' WHERE userId = " + userId +
                    " AND contestId = '" + contestId + "'");
            if (!resultSet.next()) return null;
            return resultSet;
        } catch (SQLException e) {
            System.out.println("Ошибка при получении участника!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public ResultSet getParticipants(String contestId) {
        ResultSet resultSet;
        try {
            resultSet = statement.executeQuery("SELECT * FROM 'participants' WHERE contestId = '" + contestId + "'");
//            if (!resultSet.next()) return null;
            return resultSet;
        } catch (SQLException e) {
            System.out.println("Ошибка при получении участников!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public ResultSet getContests(long creatorId) {
        ResultSet resultSet;
        try {
            resultSet = statement.executeQuery("SELECT * FROM 'contests' WHERE creatorId = " + creatorId);
            if (!resultSet.next()) return null;
            return resultSet;
        } catch (SQLException e) {
            System.out.println("Ошибка при получении конкурсов пользователя!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void setContestWinner(String contestId, long userId) {
        try {
            connection.createStatement().execute("UPDATE 'participants' SET status = 1 WHERE contestId = '" + contestId + "' AND userId = "
                    + userId);
        } catch (SQLException e) {
            System.out.println("Ошибка при присовении победителя пользователю!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void resetContestWinners(String contestId) {
        try {
            connection.createStatement().execute("UPDATE 'participants' SET status = 0 WHERE contestId = '" + contestId + "'");
        } catch (SQLException e) {
            System.out.println("Ошибка при ресете победителей конкурса!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public ResultSet getContestWinners(String contestId) {
        ResultSet resultSet;
        try {
            resultSet = connection.createStatement().executeQuery("SELECT * FROM 'participants' WHERE contestId = '" + contestId
                    + "' AND status = 1");
            return resultSet;
        } catch (SQLException e) {
            System.out.println("Ошибка при получении победителей конкурса!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public int countContests(long creatorId) {
        ResultSet resultSet;
        try {
            resultSet = statement.executeQuery("SELECT COUNT(*) FROM 'contests' WHERE creatorId = " + creatorId);
            return resultSet.getInt(1);
        } catch (SQLException e) {
            System.out.println("Ошибка при получении количества конкурсов пользователя!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void setContestFinished(String contestId) {
        try {
            connection.createStatement().execute("UPDATE 'contests' SET status = 'finished' WHERE contestId = '" + contestId + "'");
        } catch (SQLException e) {
            System.out.println("Ошибка при обновлении статуса конкурса!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    public void addUser(long userId) {
        try {
            statement.execute("INSERT INTO 'users' VALUES (" + userId + ", '" + new Date() + "');");
        } catch (SQLException e) {
            System.out.println("Ошибка при добавлении юзера!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public ResultSet getUsers() {
        ResultSet resultSet;
        try {
            resultSet = statement.executeQuery("SELECT * FROM 'users'");
            return resultSet;
        } catch (SQLException e) {
            System.out.println("Ошибка при получении списка юзеров бота!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void setPostMessage(String postText, String postMediaType, String postMediaId, String postKeyboard, String contestId) {
        try {
            statement.execute("UPDATE 'contests' SET postText = '" + postText + "', postMediaType = '" + postMediaType
                    + "', postMediaId = '" + postMediaId + "', postKeyboard = '" + postKeyboard + "' WHERE contestId = '" + contestId + "'");
        } catch (SQLException e) {
            System.out.println("Ошибка при установке сообщения для поста!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void addGroup(long ownerId, long groupId, String groupName) {
        try {
            statement.execute("INSERT INTO 'groups' VALUES (" + ownerId + ", " + groupId + ", '" + groupName + "');");
        } catch (SQLException e) {
            System.out.println("Ошибка при добавлении группы!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public ResultSet getGroups(long ownerId) {
        ResultSet resultSet;
        try {
            resultSet = connection.createStatement().executeQuery("SELECT * FROM 'groups' WHERE ownerId = " + ownerId);
            if (!resultSet.next()) return null;
            return resultSet;
        } catch (SQLException e) {
            System.out.println("Ошибка при получении групп!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public ResultSet getGroup(long groupId) {
        ResultSet resultSet;
        try {
            resultSet = connection.createStatement().executeQuery("SELECT * FROM 'groups' WHERE groupId = " + groupId);
            if (!resultSet.next()) return null;
            return resultSet;
        } catch (SQLException e) {
            System.out.println("Ошибка при получении группы!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public int countGroups(long ownerId) {
        ResultSet resultSet;
        try {
            resultSet = statement.executeQuery("SELECT COUNT(*) FROM 'groups' WHERE ownerId = " + ownerId);
            return resultSet.getInt(1);
        } catch (SQLException e) {
            System.out.println("Ошибка при подсчете групп!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void deleteGroup(String groupId) {
        try {
            statement.execute("DELETE FROM 'groups' WHERE groupId = " + groupId);
        } catch (SQLException e) {
            System.out.println("Ошибка при удалении группы!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void setPrizes(String contestId, short prizes) {
        try {
            statement.execute("UPDATE 'contests' SET prizes = " + prizes + " WHERE contestId = '" + contestId + "'");
        } catch (SQLException e) {
            System.out.println("Ошибка при обновлении призовых мест!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void setMaxParticipants(String contestId, int maxParticipants) {
        try {
            statement.execute("UPDATE 'contests' SET maxParticipants = " + maxParticipants + " WHERE contestId = '" + contestId + "'");
        } catch (SQLException e) {
            System.out.println("Ошибка при обновлении макс. числа участников!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void addSubCheck(String contestId, long groupId) {
        try {
            statement.execute("INSERT INTO 'subChecks' VALUES ('" + contestId + "', " + groupId + ")");
        } catch (SQLException e) {
            System.out.println("Ошибка при добавлении проверки на подписку!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public ResultSet getSubCheck(String contestId) {
        ResultSet resultSet;
        try {
            resultSet = connection.createStatement().executeQuery("SELECT * FROM 'subChecks' WHERE contestId = '" + contestId + "'");
            if (!resultSet.next()) return null;
            return resultSet;
        } catch (SQLException e) {
            System.out.println("Ошибка при получении проверок на подписку!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void deleteSubCheck(String contestId, long groupId) {
        try {
            statement.execute("DELETE FROM 'subChecks' WHERE contestId = '" + contestId + "' AND groupId = " + groupId);
        } catch (SQLException e) {
            System.out.println("Ошибка при удалении проверки на подписку!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public boolean switchCaptcha(String contestId) {
        boolean isCaptchaEnabled;
        try {
             isCaptchaEnabled = !getContest(contestId).getBoolean("captcha");
            statement.execute("UPDATE 'contests' SET captcha = " + isCaptchaEnabled + " WHERE contestId = '" + contestId + "'");
        } catch (SQLException e) {
            System.out.println("Ошибка при изменении проверки капчи!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return isCaptchaEnabled;
    }

    public void addParticipationPost(String contestId, String inlineMessageId) {
        try {
            statement.execute("INSERT INTO 'participationPosts' VALUES ('" + contestId + "', '" + inlineMessageId + "')");
        } catch (SQLException e) {
            System.out.println("Ошибка при добавлении поста с участием!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public ResultSet getParticipationPosts(String contestId) {
        ResultSet resultSet;
        try {
            resultSet = connection.createStatement().executeQuery("SELECT * FROM 'participationPosts' WHERE contestId = '"
                    + contestId + "'");
            return resultSet;
        } catch (SQLException e) {
            System.out.println("Ошибка при получении поста с участием!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void deleteParticipationPost(String contestId, String inlineMessageId) {
        try {
            statement.execute("DELETE FROM 'participationPosts' WHERE contestId = '" + contestId + "' AND inlineMessageId = '"
                    + inlineMessageId + "'");
        } catch (SQLException e) {
            System.out.println("Ошибка при удалении поста с участием!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    private String getContestId() {
        String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        while (sb.length() < 6) { // length of the random string.
            int index = (int) (random.nextFloat() * CHARS.length());
            sb.append(CHARS.charAt(index));
        }

        return sb.toString();
    }

    public Connection getConnection() {
        return connection;
    }

    public Statement getStatement() {
        return statement;
    }

}
