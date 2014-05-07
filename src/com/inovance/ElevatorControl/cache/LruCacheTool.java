package com.inovance.ElevatorControl.cache;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-7.
 * Time: 14:59.
 * Lru 缓存
 */

public class LruCacheTool {

    private SimpleDiskCache diskCache;

    private static final int cacheMaxSize = 10485760;

    private static final String cacheDir = "localCache";

    private static final int versionCode = 1;

    private static LruCacheTool ourInstance = new LruCacheTool();

    public static LruCacheTool getInstance() {
        return ourInstance;
    }

    private LruCacheTool() {

    }

    /**
     * Init cache manage
     *
     * @param context context
     */
    public void initCache(Context context) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            if (context.getExternalCacheDir() != null) {
                File cacheDirectory = new File(context.getExternalCacheDir().getPath() + "/" + cacheDir);
                try {
                    diskCache = SimpleDiskCache.open(cacheDirectory, versionCode, cacheMaxSize);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Cache text.
     *
     * @param key  Cache key
     * @param text Cache text
     */
    public void putCache(String key, String text) {
        try {
            diskCache.put(key, text);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get cache text by cache key.
     *
     * @param cacheKey Cache key
     * @return Cache text or null.
     */
    public String getCache(String cacheKey) {
        try {
            SimpleDiskCache.StringEntry stringEntry = diskCache.getString(cacheKey);
            if (stringEntry != null) {
                return stringEntry.getString();
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

}