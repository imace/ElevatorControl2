package com.inovance.elevatorcontrol.cache;

import android.content.Context;

import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.utils.UserSession;

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

    private static final String TAG = LruCacheTool.class.getSimpleName();

    private SimpleDiskCache diskCache;

    private static final int cacheMaxSize = 10485760;

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
        if (context.getFilesDir() != null) {
            File path = new File(context.getFilesDir().getPath() + "/" + ApplicationConfig.CacheFolder);
            if (!UserSession.getInstance().isSessionAvailable()) {
                if (path.exists()) {
                    if (path.isDirectory()) {
                        String[] children = path.list();
                        for (String aChildren : children) {
                            new File(path, aChildren).delete();
                        }
                    }
                }
            }
            if (!path.exists()) {
                path.mkdir();
            }
            try {
                diskCache = SimpleDiskCache.open(path, versionCode, cacheMaxSize);
            } catch (IOException e) {
                e.printStackTrace();
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
        if (cacheKey != null && cacheKey.length() > 0) {
            try {
                SimpleDiskCache.StringEntry stringEntry = diskCache.getString(cacheKey);
                if (stringEntry != null) {
                    return stringEntry.getString();
                } else {
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
