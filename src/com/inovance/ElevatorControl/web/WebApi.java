package com.inovance.ElevatorControl.web;

import android.content.Context;
import android.util.Xml;
import com.inovance.ElevatorControl.config.ApplicationConfig;
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

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-4-10.
 * Time: 9:20.
 */
public class WebApi {

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
        String postURL = ApplicationConfig.DomainName + ApplicationConfig.RegisterUser;
        String params = "username={param0}&company={param1}&mobilephone" +
                "={param2}&contacttel={param3}&email={param4}&blue={param5}";
        params = params.replace("{param0}", user.getName());
        params = params.replace("{param1}", user.getCompany());
        params = params.replace("{param2}", user.getCellPhone());
        params = params.replace("{param3}", user.getTelephone());
        params = params.replace("{param4}", user.getEmail());
        params = params.replace("{param5}", user.getBluetoothAddress());
        try {
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
    }

    /**
     * 验证用户
     *
     * @param bluetoothAddress Bluetooth Address
     */
    public void verifyUser(String bluetoothAddress) {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.VerifyUser + bluetoothAddress;
        startGetRequest(requestURL, ApplicationConfig.VerifyUser);
    }

    /**
     * 取得功能参数
     *
     * @param deviceType 设备型号
     */
    public void getFunctionCode(String deviceType) {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetFunctionCode + deviceType;
        startGetRequest(requestURL, ApplicationConfig.GetFunctionCode);
    }

    /**
     * 取得错误帮助信息
     */
    public void getErrorHelpList() {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetErrorHelp;
        startGetRequest(requestURL, ApplicationConfig.GetErrorHelp);
    }

    /**
     * 获取最近一次更新的(故障码，功能码，状态码)的时间
     *
     * @param deviceType 设备型号
     */
    public void getParameterListUpdateTime(String deviceType) {
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
    }

    /**
     * 取得设备状态参数
     *
     * @param deviceType 设备型号
     */
    public void getStateCode(String deviceType) {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetStateCode + deviceType;
        startGetRequest(requestURL, ApplicationConfig.GetStateCode);
    }

    /**
     * 取得所有设备列表
     */
    public void getDeviceList() {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetDeviceList;
        startGetRequest(requestURL, ApplicationConfig.GetDeviceList);
    }

    /**
     * 获取所有厂商的列表
     */
    public void getVendorList() {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetVendorList;
        startGetRequest(requestURL, ApplicationConfig.GetVendorList);
    }

    /**
     * 根据厂商ID获得该厂商的所有设备
     *
     * @param vendorID 厂商ID
     */
    public void getDeviceListByVendorIDS(String vendorID) {
        String requestURL = ApplicationConfig.DomainName
                + ApplicationConfig.GetDeviceListByVendorID
                + vendorID;
        startGetRequest(requestURL, ApplicationConfig.GetDeviceListByVendorID);
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
        String postURL = ApplicationConfig.DomainName + ApplicationConfig.ApplyFirmwareApplication;
        String params = "blue={param0}&deviceID={param1}&remark={param2}";
        params = params.replace("{param0}", bluetoothAddress);
        params = params.replace("{param1}", deviceID);
        params = params.replace("{param2}", remark);
        try {
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
    }

    /**
     * 获取所有已审批但未提取的信息列表（用于查看是否有已经审批通过，但还没有提取的固件）
     *
     * @param bluetoothAddress 手机蓝牙地址
     */
    public void getAllFirmwareNotDownload(String bluetoothAddress) {
        String requestURL = ApplicationConfig.DomainName
                + ApplicationConfig.GetAllFirmwareNotDownload
                + bluetoothAddress;
        startGetRequest(requestURL, ApplicationConfig.GetAllFirmwareNotDownload);
    }

    /**
     * 记录用户提取文件的日期，并删除服务器上的文件
     *
     * @param approveID 审批记录的ID
     */
    public void deleteFileFromServer(int approveID) {
        String requestURL = ApplicationConfig.DomainName
                + ApplicationConfig.DeleteFile
                + approveID;
        client.get(requestURL, new ResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                if (onResultListener != null) {
                    onResultListener.onResult(ApplicationConfig.DeleteFile,
                            getResponseString(response, STRING_TAG));
                }
            }
        });
    }

    /**
     * 提取固件，返回Base64编码的字符串。
     *
     * @param approveID 审批记录的ID
     */
    public void downloadFirmwareFromServer(int approveID) {
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
    }

    /**
     * Start Get Request
     *
     * @param url URL
     * @param tag Tag
     */
    private void startGetRequest(final String url, final String tag) {
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
        client.get(url, new ResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                String responseString = getResponseString(response, STRING_TAG);
                if (onResultListener != null) {
                    onResultListener.onResult(tag, responseString);
                }
            }
        });
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
