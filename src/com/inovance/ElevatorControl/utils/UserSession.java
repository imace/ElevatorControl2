package com.inovance.elevatorcontrol.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.inovance.elevatorcontrol.config.ApplicationConfig;

public class UserSession {
    private static UserSession instance = new UserSession();

    private Context context;

    public static final String LAST_LOGIN_STAMP = "lastLoginStamp";

    private SharedPreferences preferences;

    public static UserSession getInstance() {
        return instance;
    }

    private UserSession() {

    }

    public void init(Context context) {
        preferences = context.getSharedPreferences(ApplicationConfig.PREFERENCE_FILE_NAME, 0);
        this.context = context;
    }

    /**
     * Check user session avaiable
     *
     * @return true or false
     */
    public boolean isSessionAvailable() {
        long last = preferences.getLong(LAST_LOGIN_STAMP, 0);
        if (last == 0) {
            return false;
        }
        long current = System.currentTimeMillis();
        int days = (int) ((current - last) / (1000 * 60 * 60 * 24));
        return days <= 30;
    }

    /**
     * Record user login session
     */
    public void recordSession() {
        long last = preferences.getLong(LAST_LOGIN_STAMP, 0);
        if (last == 0) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putLong(LAST_LOGIN_STAMP, System.currentTimeMillis());
            editor.apply();
        }
    }

    public void destroySession() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(LAST_LOGIN_STAMP, 0);
        editor.apply();
    }
}
