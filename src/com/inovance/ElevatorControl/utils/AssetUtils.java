package com.inovance.ElevatorControl.utils;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AssetUtils {

    /**
     * @param ctx
     * @return
     */
    public static String readDefaultFunCode(Context ctx, String filename) {
        // 璇诲彇default_data.json
        InputStream inputStream = null;
        try {
            inputStream = ctx.getAssets().open(filename);
        } catch (IOException e) {
            Log.e("IOUtils", e.getMessage());
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
            Log.e("IOUtils", e.getMessage());
        }
        return outputStream.toString();

    }
}
