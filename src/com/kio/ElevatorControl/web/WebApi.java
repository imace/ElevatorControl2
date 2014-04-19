package com.kio.ElevatorControl.web;

import android.content.Context;
import android.util.Xml;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.models.User;
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
        void onResult(String responseString);
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
                "={param2}&contacttel={param3}&email={param4}";
        params.replace("{param0}", user.getName());
        params.replace("{param1}", user.getCompany());
        params.replace("{param2}", user.getCellPhone());
        params.replace("{param3}", user.getTelephone());
        params.replace("{param4}", user.getEmail());
        try {
            HttpEntity entity = new StringEntity(params);
            client.post(context,
                    postURL,
                    entity,
                    "application/x-www-form-urlencoded",
                    new ResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                            onResultListener.onResult(getResponseString(response));
                        }
                    });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * 验证用户
     *
     * @param cellPhone Cell Phone Number
     */
    public void verifyUser(String cellPhone) {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.VerifyUser + cellPhone;
        client.get(requestURL, new ResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                onResultListener.onResult(getResponseString(response));
            }
        });
    }

    /**
     * 取得功能参数
     *
     * @param deviceType 设备型号
     */
    public void getFunctionCode(String deviceType) {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetFunctionCode + deviceType;
        client.get(requestURL, new ResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                onResultListener.onResult(getResponseString(response));
            }
        });
    }

    /**
     * 取得最近一次更新的时间戳
     */
    public void getErrorHelpListLastUpdateTime() {

    }

    /**
     * 取得错误帮助信息
     */
    public void getErrorHelpList() {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetErrorHelp;
        client.get(requestURL, new ResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                onResultListener.onResult(getResponseString(response));
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
        client.get(requestURL, new ResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                onResultListener.onResult(getResponseString(response));
            }
        });
    }

    /**
     * 取得设备列表
     */
    public void getDeviceList() {
        String requestURL = ApplicationConfig.DomainName + ApplicationConfig.GetDeviceList;
        client.get(requestURL, new ResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                onResultListener.onResult(getResponseString(response));
            }
        });
    }

    /**
     * Get Response String
     *
     * @param bytes byte Array
     * @return String
     */
    private String getResponseString(byte[] bytes) {
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
                        if (parser.getName().equalsIgnoreCase("string")) {
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
