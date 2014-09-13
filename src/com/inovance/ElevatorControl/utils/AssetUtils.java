package com.inovance.elevatorcontrol.utils;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AssetUtils {

    /**
     * @param context context
     * @return String
     */
    public static String readDefaultFunCode(Context context, String filename) {
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte buf[] = new byte[80240];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toString();

    }
}
