package com.kio.ElevatorControl.cache;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-7.
 * Time: 14:59.
 * Lru 缓存
 */

public class LruCacheTool {
    private static LruCacheTool ourInstance = new LruCacheTool();

    public static LruCacheTool getInstance() {
        return ourInstance;
    }

    private LruCacheTool() {
    }
}
