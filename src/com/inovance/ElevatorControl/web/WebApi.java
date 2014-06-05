package com.inovance.ElevatorControl.web;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Xml;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.models.User;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.holoeverywhere.widget.Toast;
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

    /**
     * 请求失败
     */
    public static interface onRequestFailureListener {
        void onFailure(int statusCode, Throwable throwable);
    }

    /**
     * 请求成功
     */
    public static interface onGetResultListener {
        void onResult(String tag, String responseString);
    }

    private static WebApi ourInstance = new WebApi();

    private static AsyncHttpClient client = new AsyncHttpClient();

    private onRequestFailureListener onFailureListener;

    private onGetResultListener onResultListener;

    public void setOnResultListener(onGetResultListener onResultListener) {
        this.onResultListener = onResultListener;
    }

    public void setOnFailureListener(onRequestFailureListener onFailureListener) {
        this.onFailureListener = onFailureListener;
    }

    public void removeListener() {
        this.onResultListener = null;
        this.onFailureListener = null;
    }

    public static WebApi getInstance() {
        return ourInstance;
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
     * 验证用户
     *
     * @param bluetoothAddress Bluetooth Address
     */
    public void verifyUser(Context context, String bluetoothAddress) {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.VerifyUser + bluetoothAddress;
        startGetRequest(context, requestURL, ApplicationConfig.VerifyUser);
    }

    /**
     * 取得功能参数
     *
     * @param deviceType 设备型号
     */
    public void getFunctionCode(Context context, String deviceType) {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetFunctionCode + deviceType;
        startGetRequest(context, requestURL, ApplicationConfig.GetFunctionCode);
    }

    /**
     * 取得错误帮助信息
     */
    public void getErrorHelpList(Context context) {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetErrorHelp;
        startGetRequest(context, requestURL, ApplicationConfig.GetErrorHelp);
    }

    /**
     * 获取最近一次更新的(故障码，功能码，状态码)的时间
     *
     * @param deviceType 设备型号
     */
    public void getParameterListUpdateTime(Context context, String deviceType) {
        if (isNetworkAvailable(context)) {
            String requestURL = ApplicationConfig.DomainName
                    + ApplicationConfig.GetParameterListUpdateTime
                    + deviceType;
            client.get(requestURL, new ResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                    String responseString = getResponseString(response, STRING_TAG);
                    if (onResultListener != null) {
                        onResultListener.onResult(ApplicationConfig.GetParameterListUpdateTime,
                                responseString);
                    }
                }
            });
        } else {
            alertUserNetworkNotAvailable(context);
        }
    }

    /**
     * 取得设备状态参数
     *
     * @param deviceType 设备型号
     */
    public void getStateCode(Context context, String deviceType) {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetStateCode + deviceType;
        startGetRequest(context, requestURL, ApplicationConfig.GetStateCode);
    }

    /**
     * 取得所有设备列表
     */
    public void getDeviceList(Context context) {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetDeviceList;
        startGetRequest(context, requestURL, ApplicationConfig.GetDeviceList);
    }

    /**
     * 获取所有厂商的列表
     */
    public void getVendorList(Context context) {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetVendorList;
        startGetRequest(context, requestURL, ApplicationConfig.GetVendorList);
    }

    /**
     * 根据厂商ID获得该厂商的所有设备
     *
     * @param vendorID 厂商ID
     */
    public void getDeviceListByVendorIDS(Context context, String vendorID) {
        String requestURL = ApplicationConfig.DomainName
                + ApplicationConfig.GetDeviceListByVendorID
                + vendorID;
        startGetRequest(context, requestURL, ApplicationConfig.GetDeviceListByVendorID);
    }

    /**
     * 申请通用设备固件
     *
     * @param context          Context
     * @param bluetoothAddress 蓝牙地址
     * @param deviceID         设备ID
     * @param remark           备注
     */
    public void applyFirmware(Context context, String bluetoothAddress, String deviceID, String remark) {
        if (isNetworkAvailable(context)) {
            String postURL = ApplicationConfig.DomainName + ApplicationConfig.ApplyFirmwareApplication;
            String params = "blue={param0}&deviceID={param1}&remark={param2}";
            params = params.replace("{param0}", bluetoothAddress);
            params = params.replace("{param1}", deviceID);
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
     * 获取所有已审批但未提取的信息列表（用于查看是否有已经审批通过，但还没有提取的固件）
     *
     * @param bluetoothAddress 手机蓝牙地址
     */
    public void getAllFirmwareNotDownload(Context context, String bluetoothAddress) {
        String requestURL = ApplicationConfig.DomainName
                + ApplicationConfig.GetAllFirmwareNotDownload
                + bluetoothAddress;
        startGetRequest(context, requestURL, ApplicationConfig.GetAllFirmwareNotDownload);
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
        startGetRequest(context, requestURL, ApplicationConfig.DeleteFile);
    }

    /**
     * 取得最新软件版本信息
     *
     * @param context Context
     */
    public void getLastSoftwareVersion(Context context) {
        String requestURL = ApplicationConfig.DomainName
                + ApplicationConfig.GetLastSoftwareVersion;
        startGetRequest(context, requestURL, ApplicationConfig.GetLastSoftwareVersion);
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
     * Start Get Request
     *
     * @param url URL
     * @param tag Tag
     */
    private void startGetRequest(Context context, final String url, final String tag) {
        /*
        String cacheString = LruCacheTool.getInstance().getCache(url);
        if (cacheString != null) {
            if (onResultListener != null) {
                onResultListener.onResult(tag, cacheString);
            }
        } else {
            client.get(url, new ResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                    String responseString = getResponseString(response, STRING_TAG);
                    LruCacheTool.getInstance().putCache(url, responseString);
                    if (onResultListener != null) {
                        onResultListener.onResult(tag, responseString);
                    }
                }
            });
        }
        */
        if (isNetworkAvailable(context)) {
            client.get(url, new ResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                    String responseString = getResponseString(response, STRING_TAG);
                    if (onResultListener != null) {
                        onResultListener.onResult(tag, responseString);
                    }
                }
            });
        } else {
            alertUserNetworkNotAvailable(context);
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
