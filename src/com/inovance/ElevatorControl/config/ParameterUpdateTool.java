package com.inovance.ElevatorControl.config;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.widget.Toast;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.daos.DeviceDao;
import com.inovance.ElevatorControl.daos.ParameterFactoryDao;
import com.inovance.ElevatorControl.models.Device;
import com.inovance.ElevatorControl.models.User;
import com.inovance.ElevatorControl.web.WebApi;
import com.inovance.ElevatorControl.web.WebApi.OnGetResultListener;
import com.inovance.ElevatorControl.web.WebApi.OnRequestFailureListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-5-29.
 * Time: 11:51.
 */
public class ParameterUpdateTool implements OnGetResultListener, OnRequestFailureListener {

    private static final String TAG = ParameterUpdateTool.class.getSimpleName();

    private static ParameterUpdateTool ourInstance = new ParameterUpdateTool();

    private Context context;

    private static final String DefaultDeviceName = ApplicationConfig.DefaultDeviceName;

    /**
     * 所有配置结束
     */
    public static interface OnCheckResultListener {
        // 参数更新成功并保存到本地
        void onComplete();

        // 参数更新失败
        void onFailed(Throwable throwable);
    }

    private OnCheckResultListener mListener;

    /**
     * 普通用户权限
     */
    public static final int Normal = 0;

    /**
     * 专有设备权限
     */
    public static final int Special = 1;

    private int permission = Normal;

    /**
     * 当前登录的用户
     */
    private User currentUser;

    /**
     * 当前的设备型号
     */
    private Device currentDevice;

    private boolean updateFunctionCodeComplete;

    private boolean updateStateCodeComplete;

    private boolean updateErrorHelpComplete;

    /**
     * 更新对话框
     */
    private ProgressDialog updateDialog;

    private IntentFilter intentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        return filter;
    }

    private BroadcastReceiver broadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                if (noConnectivity && mListener != null) {
                    mListener.onFailed(null);
                    // Unregister network state change broadcast receiver
                    context.unregisterReceiver(broadcastReceiver());
                }
            }
        };
    }

    public static ParameterUpdateTool getInstance() {
        return ourInstance;
    }

    private ParameterUpdateTool() {

    }

    /**
     * 设置默认的 Device ID
     *
     * @param context Context
     */
    public void init(Context context) {
        this.context = context;
        this.currentDevice = DeviceDao.findByName(context, DefaultDeviceName, Device.NormalDevice);
        // Register network state change broadcast receiver
        context.registerReceiver(broadcastReceiver(), intentFilter());
    }

    public String getDeviceName() {
        if (currentDevice != null) {
            return currentDevice.getDeviceName();
        }
        return "0xFFFFFFFF";
    }

    public String getSupplierCode() {
        if (currentDevice != null) {
            if (currentDevice.getDeviceType() == Device.NormalDevice) {
                return context.getResources().getString(R.string.normal_device_text);
            }
            return currentDevice.getDeviceSupplierCode();
        }
        return "0xFFFFFFFF";
    }

    public int getDeviceSQLID() {
        if (currentDevice != null) {
            return currentDevice.getId();
        }
        return -1;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
        this.permission = this.currentUser.getPermission();
    }

    /**
     * 根据设备名称查找数据库中是否存在相应的条目
     *
     * @param remoteID   API 设备ID
     * @param deviceName 设备名称
     * @param deviceType 设备类型 标准、专有
     * @param listener   OnCheckResultListener
     */
    public void selectDevice(Context context,
                             int remoteID, String deviceName,
                             int deviceType, OnCheckResultListener listener) {
        this.context = context;
        mListener = listener;
        Device device = DeviceDao.findByName(context, deviceName, deviceType);
        if (device != null) {
            device.setRemoteID(remoteID);
            this.currentDevice = device;
        } else {
            device = new Device();
            device.setDeviceType(deviceType);
            device.setDeviceName(deviceName);
            DeviceDao.save(context, device);
            device.setRemoteID(remoteID);
            this.currentDevice = device;
        }
        Toast.makeText(context, R.string.check_parameter_data_update_text, Toast.LENGTH_SHORT).show();
        // 检查并更新参数功能码
        updateFunctionCodeComplete = false;
        updateStateCodeComplete = false;
        updateErrorHelpComplete = false;
        WebApi.getInstance().setOnFailureListener(this);
        WebApi.getInstance().setOnResultListener(this);
        WebApi.getInstance().getDeviceCodeUpdateTime(context, remoteID, deviceType);
    }

    private void showUpdateDialog() {
        if (updateDialog == null || !updateDialog.isShowing()) {
            updateDialog = new ProgressDialog(context);
            updateDialog.setMessage(context.getString(R.string.update_parameter_data_wait_message));
            updateDialog.setIndeterminate(false);
            updateDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            updateDialog.show();
            updateDialog.setCancelable(false);
            updateDialog.setCanceledOnTouchOutside(false);
        }
    }

    public int getPermission() {
        return permission;
    }

    public void setPermission(int permission) {
        this.permission = permission;
    }

    /**
     * 检查更新是否完毕
     */
    private void checkUpdateParameterDataComplete() {
        if (updateFunctionCodeComplete && updateStateCodeComplete && updateErrorHelpComplete) {
            if (updateDialog != null && updateDialog.isShowing()) {
                updateDialog.dismiss();
            }
            if (mListener != null) {
                mListener.onComplete();
            }
        }
    }

    @Override
    public void onResult(String tag, String responseString) {
        // 检查参数是否有更新
        if (tag.equalsIgnoreCase(ApplicationConfig.GetParameterListUpdateTime)) {
            try {
                JSONArray jsonArray = new JSONArray(responseString);
                int size = jsonArray.length();
                for (int i = 0; i < size; i++) {
                    JSONObject object = jsonArray.getJSONObject(i);
                    String type = object.optString("codeType");
                    String updateTime = object.optString("updateTime");
                    if (!updateTime.equalsIgnoreCase("null")) {
                        if (type.equalsIgnoreCase("funcode")) {
                            if (currentDevice.getFunctionCodeUpdateTime() == null
                                    || !currentDevice.getFunctionCodeUpdateTime().equalsIgnoreCase(updateTime)) {
                                ParameterFactoryDao.emptyRecordByDeviceID(context,
                                        getDeviceSQLID(),
                                        ApplicationConfig.FunctionCodeType);
                                updateFunctionCodeComplete = false;
                                currentDevice.setFunctionCodeUpdateTime(updateTime);
                                showUpdateDialog();
                                WebApi.getInstance().getFunctionCode(context,
                                        currentDevice.getRemoteID(),
                                        currentDevice.getDeviceType());
                            } else {
                                updateFunctionCodeComplete = true;
                            }
                        }
                        if (type.equalsIgnoreCase("state")) {
                            if (currentDevice.getStateCodeUpdateTime() == null
                                    || !currentDevice.getStateCodeUpdateTime().equalsIgnoreCase(updateTime)) {
                                ParameterFactoryDao.emptyRecordByDeviceID(context,
                                        getDeviceSQLID(),
                                        ApplicationConfig.StateCodeType);
                                currentDevice.setStateCodeUpdateTime(updateTime);
                                updateStateCodeComplete = false;
                                showUpdateDialog();
                                WebApi.getInstance().getStateCode(context,
                                        currentDevice.getRemoteID(),
                                        currentDevice.getDeviceType());
                            } else {
                                updateStateCodeComplete = true;
                            }
                        }
                        if (type.equalsIgnoreCase("help")) {
                            if (currentDevice.getErrorHelpUpdateTime() == null
                                    || !currentDevice.getErrorHelpUpdateTime().equalsIgnoreCase(updateTime)) {
                                currentDevice.setErrorHelpUpdateTime(updateTime);
                                updateErrorHelpComplete = false;
                                showUpdateDialog();
                                WebApi.getInstance().getErrorHelpList(context,
                                        currentDevice.getRemoteID(),
                                        currentDevice.getDeviceType());
                            } else {
                                updateErrorHelpComplete = true;
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            checkUpdateParameterDataComplete();
        }
        // 功能参数
        if (tag.equalsIgnoreCase(ApplicationConfig.GetFunctionCode)) {
            final String data = responseString;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ParameterFactoryDao.emptyRecordByDeviceID(context,
                            getDeviceSQLID(),
                            ApplicationConfig.FunctionCodeType);
                    ParameterFactoryDao.saveFunctionCode(data, context, getDeviceSQLID());
                    DeviceDao.update(context, currentDevice);
                    updateFunctionCodeComplete = true;
                    checkUpdateParameterDataComplete();
                }
            }).start();
        }
        // 状态参数
        if (tag.equalsIgnoreCase(ApplicationConfig.GetStateCode)) {
            final String data = responseString;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ParameterFactoryDao.emptyRecordByDeviceID(context, getDeviceSQLID(),
                            ApplicationConfig.StateCodeType);
                    ParameterFactoryDao.saveStateCode(data, context, getDeviceSQLID());
                    DeviceDao.update(context, currentDevice);
                    updateStateCodeComplete = true;
                    checkUpdateParameterDataComplete();
                }
            }).start();
        }
        // 故障帮助
        if (tag.equalsIgnoreCase(ApplicationConfig.GetErrorHelp)) {
            final String data = responseString;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ParameterFactoryDao.emptyRecordByDeviceID(context,
                            getDeviceSQLID(),
                            ApplicationConfig.ErrorHelpType);
                    ParameterFactoryDao.saveErrorHelp(data, context, getDeviceSQLID());
                    DeviceDao.update(context, currentDevice);
                    updateErrorHelpComplete = true;
                    checkUpdateParameterDataComplete();
                }
            }).start();
        }
    }

    @Override
    public void onFailure(int statusCode, Throwable throwable) {
        // Timeout or network not reliable
        if (mListener != null) {
            mListener.onFailed(throwable);
        }
    }
}
