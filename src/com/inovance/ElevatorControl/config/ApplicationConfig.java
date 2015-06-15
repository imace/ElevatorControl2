package com.inovance.elevatorcontrol.config;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-19.
 * Time: 10:48.
 */
public class ApplicationConfig {

    /**
     * 是否是内部用户版本
     */
    public static final boolean IsInternalVersion = true;

    /**
     * 是否生产环境
     */
    public static final boolean IsProductionMode = false;

    public static final String PREFERENCE_FILE_NAME = "ApplicationPreference";

    public static final String DefaultDeviceName = "NICE 3000+";

    /**
     * 识别厂商的SN
     *  1 标准设备-内部用户 999999
        2. 标准设备-普通用户 000000
        3. 非标设备的SN 定义  打包时再定义
     */
    public static final String SN = "000000";

    /**
     * 写密码解锁设备参数
     */
    public static final String UnlockDeviceCode = "4000";

    /**
     * 有返回: 支持该设备 (NICE 5000+, NICE 7000+)
     * 无返回或者返回8001错误码: 暂不支持该设备
     */
    public static final String CheckDeviceVersion = "017010000000";

    /**
     * 获取设备型号指令
     */
    public static final String GetDeviceTypeCode = "FA08";

    /**
     * 错误指令
     */
    public static final String ErrorCode = "8001";

    /**
     * 首页状态功能码
     * 运行速度
     * 系统状态
     * 当前故障信息
     * 当前楼层
     * 电梯运行状态（状态字功能)
     */
    public static final int[] HomeStateCode = new int[]{100, 112, 124, 107, 118};

    /**
     * 实时监控状态功能码
     * 运行速度	100
     * 额定速度	101
     * 母线电压	102
     * 输出电流	103
     * 输出频率	104
     * 输入端子状态	105
     * 输出端子状态	106
     * 当前楼层	107
     * 当前位置	108
     * 轿厢负载	109
     * 轿顶输入状态	110
     * 轿顶输出状态	111
     * 系统状态	112
     * 预转矩电流	113
     * 减速距离	114
     * 高压输入端子	115
     * 内召	116
     * 外召	117
     * 电梯运行状态（状态字功能）	118
     * 故障复位	119
     * 锁梯	120
     * 消防	121
     * 恢复出厂参数	122
     * 运行停止命令	123
     * 当前故障信息	124
     * 参数密码解锁	125
     */
    public static final int[] MonitorStateCode = new int[]{100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 115, 124, 114};

    /**
     * 输入端子
     */
    public static final int InputTerminalType = 3;

    /**
     * 输出端子
     */
    public static final int OutputTerminalType = 4;

    /**
     * 高压输入端子
     */
    public static final int HVInputTerminalType = 11;

    /**
     * 状态字
     */
    public static final int RunningStatusType = 118;

    /**
     * F5-25
     * 轿内输入类型选择
     */
    public static final int InputSelectType = 5;

    /**
     * 楼层显示
     */
    public static final int FloorShowType = 25;

    /**
     * 当前楼层
     */
    public static final int CurrentFloorType = 107;

    /**
     * 内召信息
     */
    public static final int MoveInsideInformationType = 116;

    /**
     * 外召信息
     */
    public static final int MoveOutsideInformationType = 117;

    /**
     * 标准设备型号名称
     */
    public static final String[] NormalDeviceType = new String[]{"NICE 1000", "NICE 1000+", "NICE 3000", "NICE 3000+"};

    /**
     * 当前故障信息状态码
     */
    public static final int CurrentTroubleStateCode = 124;

    /**
     * 故障复位状态码
     */
    public static final int RestoreTroubleStateCode = 119;

    /**
     * 恢复出厂参数设置
     */
    public static final int RestoreFactoryStateCode = 122;

    /**
     * NICE 3000 / NICE 1000 异步
     */
    public static final int AsyncType = 41;

    /**
     * NICE 3000 / NICE 1000 同步
     */
    public static final int SyncType = 42;

    /**
     * FA-26 到 FA-37
     */
    public static final int[] FA26ToFA37 = new int[]{26, 37};

    /**
     * 数据库名称
     */
    public static final String DATABASE_NAME = "ELEVATOR_CONTROL.db";

    public static final String SYSTEM_STATUS_NAME = "系统状态";

    public static final String ELEVATOR_BOX_STATUS_NAME = "轿厢状态";

    public static final String RETAIN_NAME = "保留";

    /**
     * 1 任意修改
     * 2 停机修改
     * 3 不可修改
     */
    public static final int[] modifyType = new int[]{1, 2, 3};

    /**
     * 电梯默认最底层和最高层
     */
    public static final int[] DefaultFloors = new int[]{1, 1};

    public static final String[] MoveSideCallCode = new String[]{
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

    // ==================================================== 存储文件夹 ============================================= //

    /**
     * 下载的固件存储文件夹
     */
    public static final String FIRMWARE_FOLDER = "FirmwareBin";

    /**
     * API 缓存文件夹
     */
    public static final String CacheFolder = "APICache";

    /**
     * 参数保存文件夹
     */
    public static final String ProfileFolder = "Profile";

    /**
     * 发送的文件保存文件夹
     */
    public static final String SentFolder = "SentFile";

    /**
     * 接收的文件保存文件夹
     */
    public static final String ReceiveFileFolder = "ReceivedFile";

    // ==================================================== Web API接口 =========================================== //

    /**
     * 功能码类型
     */
    public static final int FunctionCodeType = 1;

    /**
     * 状态码类型
     */
    public static final int StateCodeType = 2;

    /**
     * 故障帮助类型
     */
    public static final int ErrorHelpType = 3;

    /**
     * 测试版本签名不加密
     */
    public static final String RootDomain = IsProductionMode
            ? "http://android.iotdataserver.cn:8007"
            : "http://58.60.228.147:8007";

    public static final String APIUri = RootDomain + "/HuiChuanAPI.asmx/";

    // 验证是否注册用户
    // 参数：blueAddress 设备蓝牙地址
    // 返回值：成功返回True失败返回False
    public static final String VerifyUser = "VerifyRegister?blueAddress=";

    // 验证是否内部注册用户
    // 参数：blueAddress 蓝牙设备地址
    // 返回值
    public static final String VerifyInternalUser = "VerifyRegisterInternal?blueAddress=";

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
    // isT: 是否是专用设备 0 专用设备 1 标准设备
    // 返回值：返回JSON格式的功能码
    public static final String GetFunctionCode = "GetFunctionCode?DeviceType={param0}&isT={param1}";

    // 获取故障码
    // 参数：DeviceType 设备型号
    // isT: 是否是专用设备 0 专用设备 1 标准设备
    // 返回值：返回JSON格式的故障码
    public static final String GetErrorHelp = "GetErrHelp?DeviceType={param0}&isT={param1}";

    // 获取状态码
    // 参数：DeviceType 设备型号
    // isT: 是否是专用设备 0 专用设备 1 标准设备
    // 返回值：返回JSON格式的状态码
    public static final String GetStateCode = "GetState?DeviceType={param0}&isT={param1}";

    // 获取最近一次更新的(故障码，功能码，状态码)的时间
    // deviceID: 设备型号
    // isT: 是否是专用设备 0 专用设备 1 标准设备
    // 返回值：返回最近一次更新的(故障码，功能码，状态码)的时间,没有记录返回NULL
    public static final String GetParameterListUpdateTime = "getCodeUpdateTime?deviceID={param0}&isT={param1}";

    // 获取用户所有具备连接权限的非标设备的通信码
    // 参数：blue：用户的蓝牙地址
    // 返回值：返回所有具备权限的设备的通信码列表
    public static final String GetSpecialDeviceCodeList = "GetUserDevicePermission?blue=";

    // 获取所有通用设备的列表
    // 参数：无
    // 返回值：返回JSON格式的所有通用设备信息列表
    public static final String GetNormalDeviceList = "GetDeviceList";

    //************************************//
    // <editor-fold desc="软件更新">

    // 获取软件最新版本 -- 20150514讨论接口
    // 参数：厂商SN
    // 返回值：返回软件版本信息的字符串
    public static final String GetAppLatestVersionBySN = "GetAppLatestVersionBySN";

    // 下载VersionID对应的软件安装包 -- 20150514讨论接口
    // 参数：VersionID（版本ID）
    // 返回值：更新包文件流
    public static final String DownloadSoftwareByVersion = "getAppVersionByID";


    // 取得软件最新版本
    // 参数：无
    // 返回值：返回JSON格式的软件版本信息
    public static final String GetLastSoftwareVersion = IsInternalVersion
            ? "GetSoftwareVersionInside"
            : "GetSoftwareVersion";

    // 下载最新软件安装包
    // 参数：无
    // 返回值：APK 安装包
    public static final String DownloadApplicationFile = IsInternalVersion
            ? "DownloadSoftwareFileInside"
            : "UploadSoftwareFile";
    // </editor-fold>

    //************************************//
    // <editor-fold desc="固件下载新API">


    // </editor-fold>

    // <editor-fold desc="固件下载旧接口">

    // 申请通用设备固件
    // 参数：blue 手机蓝牙地址
    // deviceID 设备ID
    // remark 备注
    // 返回值：申请成功返回 True,失败返回错误信息
    public static final String ApplyFirmwareApplication = "FirmwareApplication";

    // 申请非标设备固件
    // 参数：blue：手机蓝牙地址
    // deviceID：设备ID
    // remark：备注
    // 返回值：申请成功返回"True"，否则返回错误信息
    public static final String ApplySpecialFirmwareApplication = "SpecailFirmwareApplication";

    // 获取所有已审批但未提取的信息列表（用于查看是否有已经审批通过，但还没有提取的固件）
    // 参数：blue 手机蓝牙地址
    // 返回值：如果有返回dt，没有则返回null,其中FileUrl是文件的相对路径，UseTimes是最大使用烧录次数
    public static final String GetAllFirmwareNotDownload = IsInternalVersion
            ? "ApplicationRemind_Internal?blue="
            : "ApplicationRemind?blue=";

    // 从服务器下载固件文件
    // 参数：approveID 审批记录的ID
    // 返回值：固件文件的Byte流
    public static final String DownloadFirmware = "getFirmwareFile?approveID=";

    // 记录用户提取文件的日期，并删除服务器上的文件
    // 参数：approveID 审批记录的ID(从上面的方法中获得的ID)
    // 返回值：执行成功返回"success"，否则返回错误信息
    public static final String DeleteFile = "DeleteFile?approveID=";

    // </editor-fold>


    // 获取所有厂商的列表
    // 参数：无
    // 返回值：返回JSON格式的所有厂商信息列表
    public static final String GetVendorList = "getVendorList";

    // 根据厂商ID获得该厂商的所有设备
    // 参数：vendorID 厂商ID
    // 返回值：返回JSON格式的设备信息列表
    public static final String GetDeviceListByVendorID = "GetDeviceListFromVendor?vendorID=";

    // 返回所有非标设备
    // 参数：无
    // 返回值：返回设备表，否则返回失败信息
    public static final String GetSpecialDeviceList = "GetDeviceListFromVendor";

    // 专用设备连接权限申请
    // 参数：deviceID：设备ID
    // Blue：申请用户蓝牙地址
    // 返回值：成功返回True，否则返回False或者错误信息
    public static final String ApplySpecialDevicePermission = "ApplySpecialDevicePermission";

    // 发送远程协助文件
    // 参数：FromNum：发送者电话号码
    // ToNum：接受者电话号码
    // fs：发送的文件流
    // FileName：发送的完整文件名
    // Type: 发送的文件类型(0文本, 1参数, 2照片, 3视频, 4音频)
    // Title: 标题
    // 返回值：成功返回True，失败返回False
    public static final String SendChatMessage = "SendAssistance";

    // 获得该号码所有发送的信息列表
    // 参数：phoneNum：手机号码
    // 返回值：返回文件列表
    public static final String GetSendChatMessage = "GetSendFileList?phoneNum=";

    // 获得该号码所有收到的信息列表
    // 参数：phoneNum：手机号码
    // 返回值：返回文件列表
    public static final String GetReceiveChatMessage = "GetReceiveFileList?phoneNum=";

    // 获得该号码发送或待接收的文件列表
    // phoneNum: 手机号码
    // date：起始时间戳
    // 返回值：返回文件列表(ty=1:待接收;ty=0:发送)
    public static final String GetChatMessage = "GetAssistanceFileList?phoneNum={param0}&date={param1}";

    // 接收远程协助文件
    // 参数：id：文件列表记录的ID
    // 返回值：返回文件流
    public static final String GetChatMessageFile = "ReceiveAssistanceFile?id=";

    // 返回所有已经在系统中注册的内部用户列表（姓名、部门、手机号码）
    // 参数：无
    // 返回值：所有已注册的用户列表信息
    public static final String GetRegistUserList = "GetInternalUserList";

    // 发送内部用户注册信息
    // 参数：UserName：用户姓名
    // WorkNo：工号
    // MobilePhone：手机号
    // Are：片区 默认为空
    // Department：部门
    // Email：Email
    // Remark：备注
    // Blue：蓝牙地址
    // 返回值：成功返回数据库记录,失败返回错误信息
    public static final String RegisterInternalUser = "SendRegisterInternal";

    // Crash report collect API
    public static final String ReportsCrashesAPI = RootDomain + "/error.aspx";

    // Upload message attachments API
    public static final String UploadMessageAttachments = RootDomain + "/Assistance.aspx";
}
