package com.kio.ElevatorControl.config;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-19.
 * Time: 10:48.
 * 参数名称Name对应
 */
public class ApplicationConfig {

    public static final String PREFS_NAME = "application_preference";

    /**
     * 数据库名称
     */
    public static final String DATABASE_NAME = "ElevatorControl.db";

    public static final String RUNNING_SPEED_NAME = "运行速度";

    public static final String SYSTEM_STATUS_NAME = "系统状态";

    public static final String ELEVATOR_BOX_STATUS_NAME = "轿厢状态";

    public static final String ERROR_CODE_NAME = "故障信息";

    public static final String CURRENT_FLOOR_NAME = "当前楼层";

    public static final String STATUS_WORD_NAME = "状态字功能";

    public static final String HISTORY_ERROR_CODE_NAME = "第&次故障信息";

    public static final String LAST_HISTORY_ERROR_CODE_NAME = "最后一次故障";

    public static final String GET_FLOOR_NAME = "最高层";

    public static final String RETAIN_NAME = "保留";

    /**
     * 1 任意修改
     * 2 停机修改
     * 3 不可修改
     */
    public static final int[] modifyType = new int[]{1, 2, 3};

    /**
     * 实时状态参数过滤
     */
    public static final String[] stateFilters = new String[]{
            "运行速度",
            "额定速度",
            "母线电压",
            "输出电流",
            "输出频率",
            "当前楼层",
            "当前位置",
            "轿顶输入端子",
            "轿顶输出端子",
            "系统状态",
            "轿厢负载",
            "预转矩电流",
            "减速距离",
            "故障信息",
            "高压端子输入状态",
            "输出端子",
            "输出端子2",
            "输入端子低位",
            "输入端子高位",
            "端子输入状态3",
            "端子输入状态4"};

    public static final String[] normalFilters = new String[]{
            "运行速度",
            "额定速度",
            "母线电压",
            "输出电流",
            "输出频率",
            "当前楼层",
            "当前位置",
            "轿顶输入端子",
            "轿顶输出端子",
            "系统状态",
            "轿厢负载",
            "预转矩电流",
            "减速距离",
            "故障信息",
            "高压端子输入状态"
    };

    public static final String[] outputFilters = new String[]{
            "输出端子",
            "输出端子2",
    };

    public static final String[] inputFilters = new String[]{
            "输入端子低位",
            "输入端子高位",
            "端子输入状态3",
            "端子输入状态4"
    };

    public static final int specialTypeInput = 5;

    public static final int specialTypeOutput = 6;

    /**
     * 电梯默认最底层和最高层
     */
    public static final int[] DEFAULT_FLOORS = new int[]{1, 9};

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

    /**
     * Web API接口
     */
    public static final String DomainName = "http://222.92.112.36:6954/";

    // 验证是否注册用户
    // 参数：phoneNum 手机号码
    // 返回值：成功返回True失败返回False
    public static final String VerifyUser = "HuiChuanAPI.asmx/VerifyRegister?phoneNum=";

    // 注册用户
    // 参数：username 用户名
    // company 公司
    // mobilPhone 手机号码
    // contactTel 联系电话
    // email E-MAIL地址
    // 返回值：成功返回True失败返回False
    public static final String RegisterUser = "HuiChuanAPI.asmx/SendRegister";

    // 获取最近一次功能码更新的时间戳
    // 参数：DeviceType 设备型号
    // 返回值：最近一次功能码更新的时间戳
    public static final String GetFunctionCodeUpdateTimestamp = "";

    // 获取功能码
    // 参数：DeviceType 设备型号
    // 返回值：返回JSON格式的功能码
    public static final String GetFunctionCode = "HuiChuanAPI.asmx/GetFunctionCode?DeviceType=";

    // 获取最近一次故障帮助信息更新的时间戳
    // 参数无
    // 返回值：最近一次故障帮助信息更新的时间戳
    public static final String GetErrorHelpUpdateTimestamp = "";

    // 获取故障码
    // 参数：DeviceType 设备型号
    // 返回值：返回JSON格式的故障码
    public static final String GetErrorHelp = "HuiChuanAPI.asmx/GetErrHelp?DeviceType=";

    // 获取最近一次状态码更新的时间戳
    // 参数：DeviceType 设备型号
    // 返回值：最近一次状态码更新的时间戳
    public static final String GetStateCodeUpdateTimestamp = "";

    // 获取状态码
    // 参数：DeviceType 设备型号
    // 返回值：返回JSON格式的状态码
    public static final String GetStateCode = "HuiChuanAPI.asmx/GetState?DeviceType=";

    // 获取所有设备的列表
    // 参数：无
    // 返回值：返回JSON格式的所有设备信息列表
    public static final String GetDeviceList = "HuiChuanAPI.asmx/GetDeviceList";

}
