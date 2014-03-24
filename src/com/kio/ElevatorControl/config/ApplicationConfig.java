package com.kio.ElevatorControl.config;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-19.
 * Time: 10:48.
 * 参数名称Name对应
 */
public class ApplicationConfig {

    public static final String RUNNING_SPEED_NAME = "运行速度";

    public static final String SYSTEM_STATUS_NAME = "系统状态";

    public static final String ERROR_CODE_NAME = "故障信息";

    public static final String CURRENT_FLOOR_NAME = "当前楼层";

    public static final String FC_PROTECTION_FUNCTION_NAME = "FC保护功能参数";

    public static final String HISTORY_ERROR_CODE_NAME = "第&次故障信息";

    public static final String LAST_HISTORY_ERROR_CODE_NAME = "最后一次故障";

    public static final String F6_LOGIC_PARAMETER_NAME = "F6电梯逻辑参数";

    public static final String TOP_FLOOR_NAME = "最高层";

    public static final String BOTTOM_FLOOR_NAME = "最低层";

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

}
