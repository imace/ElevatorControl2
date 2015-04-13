package com.inovance.elevatorcontrol.daos;

import android.content.Context;

import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.models.ChatMessage;

import net.tsz.afinal.FinalDb;

import java.util.List;

/**
 * Created by keith on 14-8-12.
 * User keith
 * Date 14-8-12
 * Time 下午9:24
 */
public class ChatMessageDao {

    private static final boolean DEBUG = false;

    public static List<ChatMessage> findAll(Context context) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        return db.findAll(ChatMessage.class);
    }

    public static void saveAll(Context context, List<ChatMessage> messageList) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        for (ChatMessage message : messageList) {
            db.save(message);
        }
    }

    public static void save(Context context, ChatMessage message) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        String condition = "remoteID = '" + message.getRemoteID() + "'";
        List<ChatMessage> messageList = db.findAllByWhere(ChatMessage.class, condition);
        if (messageList == null || messageList.size() == 0) {
            db.save(message);
        }
    }

    public static void update(Context context, ChatMessage message) {
        FinalDb db = FinalDb.create(context, ApplicationConfig.DATABASE_NAME, DEBUG);
        db.update(message);
    }

}
