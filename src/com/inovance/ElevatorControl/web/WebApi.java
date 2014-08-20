package com.inovance.ElevatorControl.web;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.cache.LruCacheTool;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.models.Device;
import com.inovance.ElevatorControl.models.User;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-4-10.
 * Time: 9:20.
 */
public class WebApi {

    private static final String TAG = WebApi.class.getSimpleName();

    private static final String STRING_TAG = "string";

    private static final String BASE64_TAG = "base64Binary";

    private static final int RequestTimeout = 4000;

    /**
     * 请求失败
     */
    public static interface OnRequestFailureListener {
        void onFailure(int statusCode, Throwable throwable);
    }

    /**
     * 请求成功
     */
    public static interface OnGetResultListener {
        void onResult(String tag, String responseString);
    }

    private static WebApi instance = new WebApi();

    private AsyncHttpClient client = new AsyncHttpClient();

    private OnRequestFailureListener onFailureListener;

    private OnGetResultListener onResultListener;

    public void setOnResultListener(OnGetResultListener onResultListener) {
        this.onResultListener = onResultListener;
    }

    public void setOnFailureListener(OnRequestFailureListener onFailureListener) {
        this.onFailureListener = onFailureListener;
    }

    public void removeListener() {
        this.onResultListener = null;
        this.onFailureListener = null;
    }

    public static WebApi getInstance() {
        instance.client.setTimeout(RequestTimeout);
        return instance;
    }

    private WebApi() {

    }

    /**
     * 注册用户
     *
     * @param user User Class
     */
    public void registerUser(Context context, User user) {
        if (isNetworkAvailable(context)) {
            String postURL = ApplicationConfig.DomainName + ApplicationConfig.RegisterUser;
            String params = "username={param0}&company={param1}&mobilephone" +
                    "={param2}&contacttel={param3}&email={param4}&blue={param5}";
            try {
                params = params.replace("{param0}", URLEncoder.encode(user.getName(), "UTF-8"));
                params = params.replace("{param1}", URLEncoder.encode(user.getCompany(), "UTF-8"));
                params = params.replace("{param2}", user.getCellPhone());
                params = params.replace("{param3}", user.getTelephone());
                params = params.replace("{param4}", user.getEmail());
                params = params.replace("{param5}", user.getBluetoothAddress());
                HttpEntity entity = new StringEntity(params);
                client.post(context,
                        postURL,
                        entity,
                        "application/x-www-form-urlencoded",
                        new ResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                                if (onResultListener != null) {
                                    onResultListener.onResult(ApplicationConfig.RegisterUser,
                                            getResponseString(response, STRING_TAG));
                                }
                            }
                        });
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            alertUserNetworkNotAvailable(context);
        }
    }

    /**
     * 注册内部用户
     *
     * @param context          Context
     * @param name             用户姓名
     * @param number           工号
     * @param cellphone        手机号
     * @param department       部门
     * @param email            Email
     * @param remark           备注
     * @param bluetoothAddress 蓝牙地址
     */
    public void registerInternalUser(Context context, String name, String number,
                                     String cellphone, String department, String email,
                                     String remark, String bluetoothAddress) {
        if (isNetworkAvailable(context)) {
            String postURL = ApplicationConfig.DomainName + ApplicationConfig.RegisterInternalUser;
            String params = "UserName={param0}&WorkNo={param1}&MobilePhone" +
                    "={param2}&Area=&Department={param3}&Email={param4}&Remark={param5}&Blue={param6}";
            try {
                params = params.replace("{param0}", URLEncoder.encode(name, "UTF-8"));
                params = params.replace("{param1}", number);
                params = params.replace("{param2}", cellphone);
                params = params.replace("{param3}", URLEncoder.encode(department, "UTF-8"));
                params = params.replace("{param4}", email);
                params = params.replace("{param5}", URLEncoder.encode(remark, "UTF-8"));
                params = params.replace("{param6}", bluetoothAddress);
                Log.v("registerInternalUser", params);
                HttpEntity entity = new StringEntity(params);
                client.post(context,
                        postURL,
                        entity,
                        "application/x-www-form-urlencoded",
                        new ResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                                if (onResultListener != null) {
                                    onResultListener.onResult(ApplicationConfig.RegisterInternalUser,
                                            getResponseString(response, STRING_TAG));
                                }
                            }
                        });
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            alertUserNetworkNotAvailable(context);
        }
    }

    /**
     * 验证用户
     *
     * @param bluetoothAddress Bluetooth Address
     */
    public void verifyUser(Context context, String bluetoothAddress) {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.VerifyUser + bluetoothAddress;
        startGetRequest(context, requestURL, ApplicationConfig.VerifyUser, false);
    }

    /**
     * 取得功能参数
     *
     * @param deviceID   设备 API ID
     * @param deviceType 设备类型
     */
    public void getFunctionCode(Context context, int deviceID, int deviceType) {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetFunctionCode;
        requestURL = requestURL.replace("{param0}", String.valueOf(deviceID));
        switch (deviceType) {
            case Device.NormalDevice:
                requestURL = requestURL.replace("{param1}", "1");
                break;
            case Device.SpecialDevice:
                requestURL = requestURL.replace("{param1}", "0");
                break;
        }
        startGetRequest(context, requestURL, ApplicationConfig.GetFunctionCode, false);
    }

    /**
     * 取得错误帮助信息
     *
     * @param deviceID   设备 API ID
     * @param deviceType 设备类型
     */
    public void getErrorHelpList(Context context, int deviceID, int deviceType) {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetErrorHelp;
        requestURL = requestURL.replace("{param0}", String.valueOf(deviceID));
        switch (deviceType) {
            case Device.NormalDevice:
                requestURL = requestURL.replace("{param1}", "1");
                break;
            case Device.SpecialDevice:
                requestURL = requestURL.replace("{param1}", "0");
                break;
        }
        startGetRequest(context, requestURL, ApplicationConfig.GetErrorHelp, false);
    }

    /**
     * 取得设备状态参数
     *
     * @param deviceID   设备 API ID
     * @param deviceType 设备类型
     */
    public void getStateCode(Context context, int deviceID, int deviceType) {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetStateCode;
        requestURL = requestURL.replace("{param0}", String.valueOf(deviceID));
        switch (deviceType) {
            case Device.NormalDevice:
                requestURL = requestURL.replace("{param1}", "1");
                break;
            case Device.SpecialDevice:
                requestURL = requestURL.replace("{param1}", "0");
                break;
        }
        startGetRequest(context, requestURL, ApplicationConfig.GetStateCode, false);
    }

    /**
     * 获取最近一次更新的(故障码，功能码，状态码)的时间
     *
     * @param deviceID   设备 API ID
     * @param deviceType 设备类型
     */
    public void getDeviceCodeUpdateTime(Context context, int deviceID, int deviceType) {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetParameterListUpdateTime;
        requestURL = requestURL.replace("{param0}", String.valueOf(deviceID));
        switch (deviceType) {
            case Device.NormalDevice:
                requestURL = requestURL.replace("{param1}", "1");
                break;
            case Device.SpecialDevice:
                requestURL = requestURL.replace("{param1}", "0");
                break;
        }
        startGetRequest(context, requestURL, ApplicationConfig.GetParameterListUpdateTime, false);
    }

    /**
     * 获取用户所有具备连接权限的非标设备的通信码
     *
     * @param bluetoothAddress 用户的蓝牙地址
     */
    public void getSpecialDeviceCodeList(Context context, String bluetoothAddress) {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetSpecialDeviceCodeList + bluetoothAddress;
        Log.v(TAG, requestURL);
        startGetRequest(context, requestURL, ApplicationConfig.GetSpecialDeviceCodeList, true);
    }

    /**
     * 取得所有设备列表
     */
    public void getNormalDeviceList(Context context) {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetNormalDeviceList;
        startGetRequest(context, requestURL, ApplicationConfig.GetNormalDeviceList, true);
    }

    /**
     * 获取所有厂商的列表
     */
    public void getVendorList(Context context) {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetVendorList;
        startGetRequest(context, requestURL, ApplicationConfig.GetVendorList, true);
    }

    /**
     * 根据厂商ID获得该厂商的所有设备
     *
     * @param vendorID 厂商ID
     */
    public void getDeviceListByVendorID(Context context, String vendorID) {
        String requestURL = ApplicationConfig.DomainName
                + ApplicationConfig.GetDeviceListByVendorID
                + vendorID;
        startGetRequest(context, requestURL, ApplicationConfig.GetDeviceListByVendorID, true);
    }

    /**
     * 返回所有非标设备
     */
    public void getSpecialDeviceList(Context context) {
        String requestURL = ApplicationConfig.DomainName
                + ApplicationConfig.GetSpecialDeviceList;
        startGetRequest(context, requestURL, ApplicationConfig.GetSpecialDeviceList, true);
    }

    /**
     * 申请通用设备固件
     *
     * @param context          Context
     * @param bluetoothAddress 蓝牙地址
     * @param deviceID         设备ID
     * @param remark           备注
     */
    public void applyFirmware(Context context, String bluetoothAddress, int deviceID, String remark) {
        if (isNetworkAvailable(context)) {
            String postURL = ApplicationConfig.DomainName + ApplicationConfig.ApplyFirmwareApplication;
            String params = "blue={param0}&deviceID={param1}&remark={param2}";
            params = params.replace("{param0}", bluetoothAddress);
            params = params.replace("{param1}", String.valueOf(deviceID));
            try {
                params = params.replace("{param2}", URLEncoder.encode(remark, "UTF-8"));
                HttpEntity entity = new StringEntity(params);
                client.post(context,
                        postURL,
                        entity,
                        "application/x-www-form-urlencoded",
                        new ResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                                if (onResultListener != null) {
                                    onResultListener.onResult(ApplicationConfig.ApplyFirmwareApplication,
                                            getResponseString(response, STRING_TAG));
                                }
                            }
                        });
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            alertUserNetworkNotAvailable(context);
        }
    }

    /**
     * 专用设备连接权限申请
     *
     * @param context  Context
     * @param deviceID 设备ID
     * @param name     真实姓名
     * @param company  公司名称
     * @param position 职位
     * @param phone    电话
     * @param email    邮箱
     */
    public void applySpecialDevicePermission(Context context, int deviceID, String bluetoothAddress,
                                             String name, String company, String position, String phone,
                                             String email) {
        if (isNetworkAvailable(context)) {
            String postURL = ApplicationConfig.DomainName + ApplicationConfig.ApplySpecialDevicePermission;
            String params = "Deviceid={param0}&Blue={param1}&TrueName={param2}" +
                    "&CompanyName={param3}&Position={param4}&Tel={param5}&Email={param6}";
            params = params.replace("{param0}", String.valueOf(deviceID));
            params = params.replace("{param1}", bluetoothAddress);
            try {
                params = params.replace("{param2}", URLEncoder.encode(name, "UTF-8"));
                params = params.replace("{param3}", URLEncoder.encode(company, "UTF-8"));
                params = params.replace("{param4}", URLEncoder.encode(position, "UTF-8"));
                params = params.replace("{param5}", phone);
                params = params.replace("{param6}", email);
                HttpEntity entity = new StringEntity(params);
                client.post(context,
                        postURL,
                        entity,
                        "application/x-www-form-urlencoded",
                        new ResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                                if (onResultListener != null) {
                                    onResultListener.onResult(ApplicationConfig.ApplySpecialDevicePermission,
                                            getResponseString(response, STRING_TAG));
                                }
                            }
                        });
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            alertUserNetworkNotAvailable(context);
        }
    }

    /**
     * 申请非标设备固件
     *
     * @param context          Context
     * @param bluetoothAddress 蓝牙地址
     * @param deviceID         设备ID
     * @param remark           备注
     */
    public void applySpecialFirmware(Context context, String bluetoothAddress, int deviceID, String remark) {
        if (isNetworkAvailable(context)) {
            String postURL = ApplicationConfig.DomainName + ApplicationConfig.ApplySpecialFirmwareApplication;
            String params = "blue={param0}&deviceID={param1}&remark={param2}";
            params = params.replace("{param0}", bluetoothAddress);
            params = params.replace("{param1}", String.valueOf(deviceID));
            try {
                params = params.replace("{param2}", URLEncoder.encode(remark, "UTF-8"));
                HttpEntity entity = new StringEntity(params);
                client.post(context,
                        postURL,
                        entity,
                        "application/x-www-form-urlencoded",
                        new ResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                                if (onResultListener != null) {
                                    onResultListener.onResult(ApplicationConfig.ApplySpecialFirmwareApplication,
                                            getResponseString(response, STRING_TAG));
                                }
                            }
                        });
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            alertUserNetworkNotAvailable(context);
        }
    }

    /**
     * 获取所有已审批但未提取的信息列表（用于查看是否有已经审批通过，但还没有提取的固件）
     *
     * @param bluetoothAddress 手机蓝牙地址
     */
    public void getAllFirmwareNotDownload(Context context, String bluetoothAddress) {
        String requestURL = ApplicationConfig.DomainName
                + ApplicationConfig.GetAllFirmwareNotDownload
                + bluetoothAddress;
        startGetRequest(context, requestURL, ApplicationConfig.GetAllFirmwareNotDownload, false);
    }

    /**
     * 记录用户提取文件的日期，并删除服务器上的文件
     *
     * @param approveID 审批记录的ID
     */
    public void deleteFileFromServer(Context context, int approveID) {
        String requestURL = ApplicationConfig.DomainName
                + ApplicationConfig.DeleteFile
                + approveID;
        startGetRequest(context, requestURL, ApplicationConfig.DeleteFile, false);
    }

    /**
     * 取得最新软件版本信息
     *
     * @param context Context
     */
    public void getLastSoftwareVersion(Context context) {
        String requestURL = ApplicationConfig.DomainName
                + ApplicationConfig.GetLastSoftwareVersion;
        startGetRequest(context, requestURL, ApplicationConfig.GetLastSoftwareVersion, false);
    }

    /**
     * 提取固件，返回Base64编码的字符串。
     *
     * @param approveID 审批记录的ID
     */
    public void downloadFirmwareFromServer(Context context, int approveID) {
        if (isNetworkAvailable(context)) {
            String requestURL = ApplicationConfig.DomainName
                    + ApplicationConfig.DownloadFirmware
                    + approveID;
            client.get(requestURL, new ResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                    if (onResultListener != null) {
                        onResultListener.onResult(ApplicationConfig.DownloadFirmware,
                                getResponseString(response, BASE64_TAG));
                    }
                }
            });
        } else {
            alertUserNetworkNotAvailable(context);
        }
    }

    /**
     * 发送远程协助文件
     * 返回值：成功返回True，失败返回False
     *
     * @param context    Context
     * @param from       发送者电话号码
     * @param to         接受者电话号码
     * @param fileStream 发送的文件流
     * @param fileName   发送的完整文件名
     * @param type       发送的文件类型(0文本,1参数,2照片,3视频,4音频)
     * @param title      标题
     */
    public void sendChatMessage(Context context, String from, String to, String fileStream, String fileName, int type, String title) {
        if (isNetworkAvailable(context)) {
            String postURL = ApplicationConfig.DomainName + ApplicationConfig.SendChatMessage;
            String params = "FromNum={param0}&ToNum={param1}&fs={param2}&FileName={param3}&Type={param4}&title={param5}";
            params = params.replace("{param0}", from);
            params = params.replace("{param1}", to);
            params = params.replace("{param2}", fileStream);
            try {
                params = params.replace("{param3}", URLEncoder.encode(fileName, "UTF-8"));
                params = params.replace("{param4}", String.valueOf(type));
                params = params.replace("{param5}", URLEncoder.encode(title, "UTF-8"));
                HttpEntity entity = new StringEntity(params);
                client.post(context,
                        postURL,
                        entity,
                        "application/x-www-form-urlencoded",
                        new ResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                                if (onResultListener != null) {
                                    onResultListener.onResult(ApplicationConfig.SendChatMessage,
                                            getResponseString(response, STRING_TAG));
                                }
                            }
                        });
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            alertUserNetworkNotAvailable(context);
        }
    }

    /**
     * 获得该号码所有发送的信息列表
     * 返回值：返回文件列表
     *
     * @param context     Context
     * @param phoneNumber 手机号码
     */
    public void getSendChatMessage(Context context, String phoneNumber) {
        String requestURL = ApplicationConfig.DomainName
                + ApplicationConfig.GetSendChatMessage
                + phoneNumber;
        startGetRequest(context, requestURL, ApplicationConfig.GetSendChatMessage, false);
    }

    /**
     * 获得该号码所有收到的信息列表
     * 返回值：返回文件列表
     *
     * @param context     Context
     * @param phoneNumber 手机号码
     */
    public void getReceiveChatMessage(Context context, String phoneNumber) {
        String requestURL = ApplicationConfig.DomainName
                + ApplicationConfig.GetReceiveChatMessage
                + phoneNumber;
        startGetRequest(context, requestURL, ApplicationConfig.GetReceiveChatMessage, false);
    }

    /**
     * 获得该号码发送或待接收的文件列表
     * 返回值：返回文件列表(ty=1:待接收;ty=0:发送)
     *
     * @param context     Context
     * @param phoneNumber 手机号码
     * @param timestamp   起始时间戳
     */
    public void getChatMessage(Context context, String phoneNumber, long timestamp) {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetChatMessage;
        requestURL = requestURL.replace("{param0}", phoneNumber);
        requestURL = requestURL.replace("{param1}", String.valueOf(timestamp));
        startGetRequest(context, requestURL, ApplicationConfig.GetChatMessage, false);
    }

    public void getRegistUserList(Context context) {
        String requestURL = ApplicationConfig.DomainName
                + ApplicationConfig.GetRegistUserList;
        startGetRequest(context, requestURL, ApplicationConfig.GetRegistUserList, true);
    }

    /**
     * Start Get Request
     *
     * @param url    URL
     * @param tag    Tag
     * @param cached Cached
     */
    private void startGetRequest(Context context, final String url, final String tag, final boolean cached) {
        Log.v(TAG, url);
        boolean needFetchRemote;
        if (cached) {
            String cacheString = LruCacheTool.getInstance().getCache(url);
            if (cacheString != null) {
                needFetchRemote = false;
                if (onResultListener != null) {
                    onResultListener.onResult(tag, cacheString);
                }
            } else {
                needFetchRemote = true;
            }
        } else {
            needFetchRemote = true;
        }
        if (needFetchRemote) {
            if (isNetworkAvailable(context)) {
                client.get(url, new ResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                        String responseString = getResponseString(response, STRING_TAG);
                        if (cached) {
                            LruCacheTool.getInstance().putCache(url, responseString);
                        }
                        if (onResultListener != null) {
                            onResultListener.onResult(tag, responseString);
                        }
                    }
                });
            } else {
                alertUserNetworkNotAvailable(context);
            }
        }
    }

    /**
     * Get Response String
     *
     * @param bytes byte Array
     * @return String
     */
    private String getResponseString(byte[] bytes, String tag) {
        InputStream inputStream = new ByteArrayInputStream(bytes);
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(inputStream, "utf-8");
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        if (parser.getName().equalsIgnoreCase(tag)) {
                            return parser.nextText();
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Check Network Available
     *
     * @param context Context
     * @return Available Status
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (NetworkInfo anInfo : info) {
                    if (anInfo.getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Alert User Current Network Not Available
     *
     * @param context Context
     */
    private void alertUserNetworkNotAvailable(Context context) {
        Toast.makeText(context, R.string.current_network_not_available, Toast.LENGTH_SHORT).show();
    }

    /**
     * AsyncHttpResponseHandler
     */
    private class ResponseHandler extends AsyncHttpResponseHandler {

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable error) {
            if (onFailureListener != null) {
                onFailureListener.onFailure(statusCode, error);
            }
        }

    }
}
