package com.inovance.ElevatorControl.config;

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

    public static final String DeviceTypeValue = "deviceType";

    public static final String DeviceNumberValue = "deviceNumber";

    /**
     * 最大重试次数
     */
    public static final int MAX_RETRY_TIME = 10;

    /**
     * 设备型号
     * FA-08 -> 设备型号
     * FP-09 -> D2-09 厂家编号
     */
    public static final String[] deviceType = new String[]{
            "NICE 1000",
            "NICE 1000+",
            "NICE 3000",
            "NICE 3000+",
            "NICE 5000",
            "NICE 7000"
    };

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
            "轿顶板输入端子",
            "轿顶板输出端子",
            "系统状态",
            "轿厢负载",
            "预转矩电流",
            "减速距离",
            "故障信息",
            "输出端子",
            "输入端子低位",
            "输入端子高位",
            "端子输入状态3",
            "端子输入状态4",
            "高压端子输入状态"
    };

    public static final String[] normalFilters = new String[]{
            "运行速度",
            "额定速度",
            "母线电压",
            "输出电流",
            "输出频率",
            "当前楼层",
            "当前位置",
            "轿顶板输入端子",
            "轿顶板输出端子",
            "系统状态",
            "轿厢负载",
            "预转矩电流",
            "减速距离",
            "故障信息"
    };

    public static final String[] outputFilters = new String[]{
            "输出端子",
    };

    public static final String[] inputFilters = new String[]{
            "输入端子低位",
            "输入端子高位",
            "端子输入状态3",
            "端子输入状态4"
    };

    public static final String HVInputTerminalStatusName = "高压端子输入状态";

    /**
     * 内召信息
     */
    public static final String[] moveInsideInfoName = new String[]{
            "当前楼层",
            "1-8层信息",
            "9-16层信息",
            "17-24层信息",
            "25-32层信息",
            "33-40层信息",
            "41-48层信息"
    };

    /**
     * 外召信息
     */
    public static final String[] moveOutsideInfoName = new String[]{
            "当前楼层",

            "1~4层召唤信息",
            "5~8层召唤信息",
            "9~12层召唤信息",
            "13~16层召唤信息",
            "17~20层召唤信息",
            "21~24层召唤信息",
            "25~28层召唤信息",
            "29~32层召唤信息",
            "33~36层召唤信息",
            "37~40层召唤信息",

            "41~44层召唤信息",
            "45~48层召唤信息"
    };

    public static final int specialTypeInput = 5;

    public static final int specialTypeOutput = 6;

    /**
     * 电梯默认最底层和最高层
     */
    public static final int[] DEFAULT_FLOORS = new int[]{1, 1};

    /**
     * 设备型号
     */
    public static final String equipmentModel = "equipment_model";

    public static final String[] moveInsideName = new String[]{
            "1-8层信息",
            "9-16层信息",
            "17-24层信息",
            "25-32层信息",
            "33-40层信息",
            "41-48层信息"
    };

    public static final String[] moveOutsideName = new String[]{
            "1~4层召唤信息",
            "5~8层召唤信息",
            "9~12层召唤信息",
            "13~16层召唤信息",
            "17~20层召唤信息",
            "21~24层召唤信息",
            "25~28层召唤信息",
            "29~32层召唤信息",
            "33~36层召唤信息",
            "37~40层召唤信息",
            "41~44层召唤信息",
            "45~48层召唤信息"
    };

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

    public static final String NO_RESPOND = "写入失败";

    /**
     * 无描述返回     0
     * 数值计算匹配   1
     * Bit位值匹配    2
     * Bit多位值匹配  3
     */
    public static final int[] DESCRIPTION_TYPE = new int[]{0, 1, 2, 3};

    // ====================================== 系统日志类型 ============================================

    /**
     * 单个参数修改
     */
    public static final int LogWriteParameter = 1;

    /**
     * 故障复位
     */
    public static final int LogRestoreErrorStatus = 2;

    /**
     * 烧录程序
     */
    public static final int LogBurn = 3;

    /**
     * 电梯内召
     */
    public static final int LogMoveInside = 4;

    /**
     * 电梯外召
     */
    public static final int LogMoveOutside = 5;

    /**
     * 参数批量读取
     */
    public static final int LogDownloadProfile = 6;

    /**
     * 参数批量写入
     */
    public static final int LogUploadProfile = 7;

    /**
     * 恢复出厂设置
     */
    public static final int LogRestoreFactory = 8;

    /**
     * 下载的固件存储文件件
     */
    public static final String FIRMWARE_FOLDER = "FirmwareBin";

    // ==================================== Web API接口 =======================================

    public static final String DomainName = "http://222.92.112.36:6954/HuiChuanAPI.asmx/";

    // 验证是否注册用户
    // 参数：blueAddress 设备蓝牙地址
    // 返回值：成功返回True失败返回False
    public static final String VerifyUser = "VerifyRegister?blueAddress=";

    // 注册用户
    // 参数：username 用户名
    // company 公司
    // mobilPhone 手机号码
    // contactTel 联系电话
    // email E-MAIL地址
    // blue 手机蓝牙地址
    // 返回值：成功返回True失败返回False
    public static final String RegisterUser = "SendRegister";

    // 获取功能码
    // 参数：DeviceType 设备型号
    // 返回值：返回JSON格式的功能码
    public static final String GetFunctionCode = "GetFunctionCode?DeviceType=";

    // 获取故障码
    // 参数：DeviceType 设备型号
    // 返回值：返回JSON格式的故障码
    public static final String GetErrorHelp = "GetErrHelp?DeviceType=";

    // 获取状态码
    // 参数：DeviceType 设备型号
    // 返回值：返回JSON格式的状态码
    public static final String GetStateCode = "GetState?DeviceType=";

    // 获取最近一次更新的(故障码，功能码，状态码)的时间
    // deviceType:设备型号：
    // 返回值：返回最近一次更新的(故障码，功能码，状态码)的时间,没有记录返回NULL
    public static final String GetParameterListUpdateTime = "getCodeUpdateTime?deviceType=";

    // 获取所有通用设备的列表
    // 参数：无
    // 返回值：返回JSON格式的所有通用设备信息列表
    public static final String GetDeviceList = "GetDeviceList";

    // 申请通用设备固件
    // 参数：blue 手机蓝牙地址
    // deviceID 设备ID
    // remark 备注
    // 返回值：申请成功返回 True,失败返回错误信息
    public static final String ApplyFirmwareApplication = "FirmwareApplication";

    // 获取所有已审批但未提取的信息列表（用于查看是否有已经审批通过，但还没有提取的固件）
    // 参数：blue 手机蓝牙地址
    // 返回值：如果有返回dt，没有则返回null,其中FileUrl是文件的相对路径，UseTimes是最大使用烧录次数
    public static final String GetAllFirmwareNotDownload = "ApplicationRemind?blue=";

    // 记录用户提取文件的日期，并删除服务器上的文件
    // 参数：approveID 审批记录的ID(从上面的方法中获得的ID)
    // 返回值：执行成功返回"success",否则返回错误信息
    public static final String DeleteFile = "DeleteFile?approveID=";

    // 从服务器下载固件文件
    // 参数：approveID 审批记录的ID
    // 返回值：固件文件的Byte流
    public static final String DownloadFirmware = "getFirmwareFile?approveID=";

    // 获取所有厂商的列表
    // 参数：无
    // 返回值：返回JSON格式的所有厂商信息列表
    public static final String GetVendorList = "getVendorList";

    // 根据厂商ID获得该厂商的所有设备
    // 参数：vendorID 厂商ID
    // 返回值：返回JSON格式的设备信息列表
    public static final String GetDeviceListByVendorID = "GetDeviceListFromVendor?vendorID=";

    // 取得软件最新版本
    // 参数：无
    // 返回值：返回JSON格式的软件版本信息
    public static final String GetLastSoftwareVersion = "";

}
