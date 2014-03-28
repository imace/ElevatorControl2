package com.kio.ElevatorControl.config;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-19.
 * Time: 10:48.
 * 参数名称Name对应
 */
public class ApplicationConfig {

    /**
     * 数据库名称
     */
    public static final String DATABASE_NAME = "ElevatorControl.db";

    public static final String RUNNING_SPEED_NAME = "运行速度";

    public static final String SYSTEM_STATUS_NAME = "系统状态";

    public static final String ERROR_CODE_NAME = "故障信息";

    public static final String CURRENT_FLOOR_NAME = "当前楼层";

    public static final String HISTORY_ERROR_CODE_NAME = "第&次故障信息";

    public static final String LAST_HISTORY_ERROR_CODE_NAME = "最后一次故障";

    public static final String GET_FLOOR_NAME = "最高层";

    public static final String RETAIN_NAME = "保留";

    /**
     * 电梯默认最底层和最高层
     */
    public static final int[] DEFAULT_FLOORS = new int[]{1, 10};

    /**
     * 设备型号
     */
    public static final String equipmentModel = "equipment_model";

    public static final String[] MOVE_SIDE_CODE = new String[]{
            "0001", // 召唤一楼 & 一楼上
            "0002", // 召唤二楼 & 一楼下
            "0004", // 召唤三楼 & 二楼上
            "0008", // 召唤四楼 & 二楼下
            "0010", // 召唤五楼 & 三楼上
            "0020", // 召唤六楼 & 三楼下
            "0040", // 召唤七楼 & 四楼上
            "0080"  // 召唤八楼 & 四楼下
    };

    /**
     * 通讯故障信息描述码
     */
    public static final String[] ERROR_CODE_ARRAY = new String[]{
            "80010000", //　无故障
            "80010001", //　密码错误
            "80010002", //　命令码错误
            "80010003", //　CRC校验错误
            "80010004", //　无效地址
            "80010005", //　无效参数
            "80010006", //　参数更改无效
            "80010007"  //　系统被锁定
    };

    /**
     * 通讯故障信息描述信息
     */
    public static final String[] ERROR_NAME_ARRAY = new String[]{
            "无故障",
            "密码错误",
            "命令码错误",
            "CRC校验错误",
            "无效地址",
            "无效参数",
            "参数更改无效",
            "系统被锁定"
    };

    public static final String NO_RESPOND = "未写入";

    /**
     * 无描述返回     0
     * 数值计算匹配   1
     * Bit位值匹配    2
     * Bit多位值匹配  3
     */
    public static final int[] DESCRIPTION_TYPE = new int[]{0, 1, 2, 3};

}
