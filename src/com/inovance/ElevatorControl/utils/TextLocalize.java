package com.inovance.elevatorcontrol.utils;

import android.content.Context;

import com.inovance.elevatorcontrol.R;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-6-11.
 * Time: 11:16.
 */
public class TextLocalize {

    private static TextLocalize instance = new TextLocalize();

    private Context context;

    public static TextLocalize getInstance() {
        return instance;
    }

    private TextLocalize() {

    }

    public void init(Context context) {
        this.context = context;
    }

    public String getViewDetailText() {
        if (context != null) {
            return context.getResources().getString(R.string.view_detail_text);
        }
        return "";
    }

    public String getWriteFailedText() {
        if (context != null) {
            return context.getResources().getString(R.string.write_failed_text);
        }
        return "";
    }
}
