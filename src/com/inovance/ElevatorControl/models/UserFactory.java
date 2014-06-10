package com.inovance.ElevatorControl.models;

import android.content.Context;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.daos.DeviceDao;
import com.inovance.ElevatorControl.daos.ParamterFactoryDao;
import com.inovance.ElevatorControl.web.WebApi;
import com.inovance.ElevatorControl.web.WebApi.OnGetResultListener;
import com.inovance.ElevatorControl.web.WebApi.OnRequestFailureListener;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-5-29.
 * Time: 11:51.
 */
public class UserFactory implements OnGetResultListener, OnRequestFailureListener {

    private static UserFactory ourInstance = new UserFactory();

    private Context context;

    private static final String DefaultDeviceName = ApplicationConfig.DefaultDevice;

    /**
     * 所有配置结束
     */
    private static interface OnDeployFinishListener {
        void onComplete();
    }

    private OnDeployFinishListener mListener;

    /**
     * 设备型号编码
     */
    private int deviceType = -1;

    /**
     * 设备型号名称
     */
    private String deviceName = "0xFFFFFFFF";

    /**
     * 厂家编号
     */
    private String supplierCode = "0xFFFFFFFF";

    /**
     * 普通用户权限
     */
    public static final int Normal = 1;

    /**
     * 专有设备权限
     */
    public static final int Special = 2;

    private int Permission = Special;

    /**
     * 当前的设备型号
     */
    private Device device;

    /**
     * 故障复位指令
     */
    private String restoreErrorCode = "6000";

    public static UserFactory getInstance() {
        return ourInstance;
    }

    private UserFactory() {

    }

    /**
     * 设置默认的 Device ID
     *
     * @param context Context
     */
    public void init(Context context) {
        if (context == null) {
            this.context = context;
        }
        Device device = DeviceDao.findByName(context, DefaultDeviceName);
        if (device != null) {
            deviceType = device.getId();
        }
    }

    public int getDeviceType() {
        return deviceType;
    }

    public String getDeviceName() {
        return deviceName;
    }

    /**
     * 根据设备名称查找数据库中是否存在相应的条目
     *
     * @param deviceName 设备型号名称
     */
    public void setDeviceName(String deviceName, OnDeployFinishListener listener) {
        mListener = listener;
        this.deviceName = deviceName;
        Device device = DeviceDao.findByName(context, deviceName);
        if (device != null) {
            this.device = device;
            this.deviceType = device.getId();
            checkDeviceCodeUpdate(deviceName);
        } else {
            this.deviceType = -1;
            device = new Device();
            device.setDeviceType(deviceName);
            this.device = device;
            downloadDeviceCode(deviceName);
        }
    }

    public String getRestoreErrorCode() {
        return restoreErrorCode;
    }

    public void setRestoreErrorCode(String restoreErrorCode) {
        this.restoreErrorCode = restoreErrorCode;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public int getPermission() {
        return Permission;
    }

    public void setPermission(int permission) {
        Permission = permission;
    }

    /**
     * 检查当前选定的设备对应的数据是否有更新
     *
     * @param deviceName 设备型号名称
     */
    private void checkDeviceCodeUpdate(String deviceName) {
        WebApi.getInstance().setOnFailureListener(this);
        WebApi.getInstance().setOnResultListener(this);
        WebApi.getInstance().getDeviceCodeUpdateTime(context, deviceName);
    }

    /**
     * 下载对应的设备数据
     *
     * @param deviceName 设备型号名称
     */
    private void downloadDeviceCode(String deviceName) {
        WebApi.getInstance().setOnFailureListener(this);
        WebApi.getInstance().setOnResultListener(this);
        WebApi.getInstance().getFunctionCode(context, deviceName);
        WebApi.getInstance().getStateCode(context, deviceName);
        WebApi.getInstance().getErrorHelpList(context, deviceName);
    }

    @Override
    public void onResult(String tag, String responseString) {
        // 检查参数是否有更新
        if (tag.equalsIgnoreCase(ApplicationConfig.GetParameterListUpdateTime)) {

        }
        if (deviceType == -1) {
            DeviceDao.save(context, device);
            deviceType = device.getId();
            // 功能参数
            if (tag.equalsIgnoreCase(ApplicationConfig.GetFunctionCode)) {
                ParamterFactoryDao.saveFunctionCode(responseString, context, device);
            }
            // 状态参数
            if (tag.equalsIgnoreCase(ApplicationConfig.GetStateCode)) {
                ParamterFactoryDao.saveStateCode(responseString, context, device);
            }
            // 故障帮助
            if (tag.equalsIgnoreCase(ApplicationConfig.GetErrorHelp)) {
                ParamterFactoryDao.saveErrorHelp(responseString, context, device);
            }
        } else {
            ParamterFactoryDao.emptyRecordByDeviceID(context, deviceType);
            // 功能参数
            if (tag.equalsIgnoreCase(ApplicationConfig.GetFunctionCode)) {
                ParamterFactoryDao.saveFunctionCode(responseString, context, device);
            }
            // 状态参数
            if (tag.equalsIgnoreCase(ApplicationConfig.GetStateCode)) {
                ParamterFactoryDao.saveFunctionCode(responseString, context, device);
            }
            // 故障帮助
            if (tag.equalsIgnoreCase(ApplicationConfig.GetErrorHelp)) {
                ParamterFactoryDao.saveFunctionCode(responseString, context, device);
            }
        }
        if (mListener != null) {
            mListener.onComplete();
        }
    }

    @Override
    public void onFailure(int statusCode, Throwable throwable) {

    }
}
