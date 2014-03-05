package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import butterknife.InjectView;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HHandler;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.adapters.TroubleAnalyzeAdapter;
import com.kio.ElevatorControl.daos.MenuValues;
import com.kio.ElevatorControl.handlers.FailureCurrentHandler;
import com.kio.ElevatorControl.handlers.FailureLogHandler;
import com.kio.ElevatorControl.models.ErrorHelp;
import com.kio.ElevatorControl.models.ErrorHelpLog;
import com.kio.ElevatorControl.utils.ParseSerialsUtils;
import com.viewpagerindicator.TabPageIndicator;

import java.lang.reflect.InvocationTargetException;

/**
 * 故障分析
 *
 * @author jch
 */
public class TroubleAnalyzeActivity extends FragmentActivity {

    private static final String TAG = TroubleAnalyzeActivity.class.getSimpleName();
    /**
     * 注入页面元素
     */
    @InjectView(R.id.pager)
    public ViewPager pager;

    @InjectView(R.id.indicator)
    public TabPageIndicator indicator;

    private HHandler bluetoothHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trouble_analyze);

        // 注入以后才能使用@InjectView定义的对象
        Views.inject(this);

        // TroubleAnalyzeActivity --> TroubleAnalyzeAdapter --> TroubleAnalyzeFragment
        pager.setAdapter(new TroubleAnalyzeAdapter(this));
        indicator.setViewPager(pager);
        indicator.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageSelected(int index) {
                try {
                    // 反射执行
                    String mName = MenuValues.getTroubleAnalyzeTabsLoadMethodName(index, TroubleAnalyzeActivity.this);
                    Log.v(TAG, String.valueOf(index) + " : " + mName);
                    TroubleAnalyzeActivity.this.getClass().getMethod(mName).invoke(TroubleAnalyzeActivity.this);
                } catch (NoSuchMethodException e) {
                    Log.e(TAG, e.getMessage());
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, e.getMessage());
                } catch (IllegalAccessException e) {
                    Log.e(TAG, e.getMessage());
                } catch (InvocationTargetException e) {
                    Log.e(TAG, "InvocationTargetException");
                } finally {
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCurrentTroubleView();
    }

    /**
     * 当前故障
     */
    public void loadCurrentTroubleView() {
        bluetoothHandler = new FailureCurrentHandler(this);
        HCommunication[] hCommunications = new HCommunication[]{
                new HCommunication() {

                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("010380000001")));
                    }

                    @Override
                    public void afterSend() {
                    }

                    @Override
                    public void beforeReceive() {
                    }

                    @Override
                    public void afterReceive() {
                    }

                    @Override
                    public Object onParse() {
                        if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                            // 通过验证
                            byte[] received = HSerial.trimEnd(getReceivedBuffer());
                            Log.v(TAG, HSerial.byte2HexStr(received));
                            ErrorHelp ep = new ErrorHelp();
                            try {
                                ep.setReceived(TroubleAnalyzeActivity.this, received);
                                // 将ep存入数据库
                                // ErrorHelpLogDao.Insert(TroubleAnalyzeActivity.this,
                                // ErrorHelpLog.Instance(ep));
                                return ep;
                            } catch (Exception e) {
                                // 失败
                                Log.e(TAG, e.getLocalizedMessage());
                            }
                        }
                        return null;
                    }

                }
        };

        if (bluetoothHandler == null)
            bluetoothHandler = new FailureCurrentHandler(this);
        if (HBluetooth.getInstance(this).isPrepared())
            HBluetooth.getInstance(this).setHandler(bluetoothHandler).setCommunications(hCommunications).HStart();
    }


    /**
     * 历史故障
     */
    public void loadHistoryTroubleView() {
        HCommunication[] hCommunications = new HCommunication[]{
                new HCommunication() {
                    @Override
                    public void beforeSend() {
                        this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103FC140004")));
                    }

                    @Override
                    public void afterSend() {
                    }

                    @Override
                    public void beforeReceive() {
                    }

                    @Override
                    public void afterReceive() {
                    }

                    @Override
                    public Object onParse() {
                        if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                            byte[] received = HSerial.trimEnd(getReceivedBuffer());
                            // 通过验证
                            ErrorHelpLog errorHelpLog = new ErrorHelpLog();
                            errorHelpLog.setReceived(received);
                            errorHelpLog.setCtx(TroubleAnalyzeActivity.this);
                            errorHelpLog = ParseSerialsUtils.getErrorHelpLog(TroubleAnalyzeActivity.this, errorHelpLog);
                            return errorHelpLog;
                        }
                        return null;
                    }
                }, new HCommunication() {
            @Override
            public void beforeSend() {
                this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103FC180004")));
            }

            @Override
            public void afterSend() {
            }

            @Override
            public void beforeReceive() {
            }

            @Override
            public void afterReceive() {
            }

            @Override
            public Object onParse() {
                if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                    byte[] received = HSerial.trimEnd(getReceivedBuffer());
                    // 通过验证
                    ErrorHelpLog errorHelpLog = new ErrorHelpLog();
                    errorHelpLog.setReceived(received);
                    errorHelpLog.setCtx(TroubleAnalyzeActivity.this);
                    errorHelpLog = ParseSerialsUtils.getErrorHelpLog(TroubleAnalyzeActivity.this, errorHelpLog);
                    return errorHelpLog;
                }
                return null;
            }
        }, new HCommunication() {
            @Override
            public void beforeSend() {
                this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103FC1C0004")));
            }

            @Override
            public void afterSend() {
            }

            @Override
            public void beforeReceive() {
            }

            @Override
            public void afterReceive() {
            }

            @Override
            public Object onParse() {
                if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                    byte[] received = HSerial.trimEnd(getReceivedBuffer());
                    // 通过验证
                    ErrorHelpLog errorHelpLog = new ErrorHelpLog();
                    errorHelpLog.setReceived(received);
                    errorHelpLog.setCtx(TroubleAnalyzeActivity.this);
                    errorHelpLog = ParseSerialsUtils.getErrorHelpLog(TroubleAnalyzeActivity.this, errorHelpLog);
                    return errorHelpLog;
                }
                return null;
            }
        }, new HCommunication() {
            @Override
            public void beforeSend() {
                this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103FC200004")));
            }

            @Override
            public void afterSend() {
            }

            @Override
            public void beforeReceive() {
            }

            @Override
            public void afterReceive() {
            }

            @Override
            public Object onParse() {
                if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                    byte[] received = HSerial.trimEnd(getReceivedBuffer());
                    // 通过验证
                    ErrorHelpLog errorHelpLog = new ErrorHelpLog();
                    errorHelpLog.setReceived(received);
                    errorHelpLog.setCtx(TroubleAnalyzeActivity.this);
                    errorHelpLog = ParseSerialsUtils.getErrorHelpLog(TroubleAnalyzeActivity.this, errorHelpLog);
                    return errorHelpLog;
                }
                return null;
            }
        }, new HCommunication() {
            @Override
            public void beforeSend() {
                this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103FC240004")));
            }

            @Override
            public void afterSend() {
            }

            @Override
            public void beforeReceive() {
            }

            @Override
            public void afterReceive() {
            }

            @Override
            public Object onParse() {
                if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                    byte[] received = HSerial.trimEnd(getReceivedBuffer());
                    // 通过验证
                    ErrorHelpLog errorHelpLog = new ErrorHelpLog();
                    errorHelpLog.setReceived(received);
                    errorHelpLog.setCtx(TroubleAnalyzeActivity.this);
                    errorHelpLog = ParseSerialsUtils.getErrorHelpLog(TroubleAnalyzeActivity.this, errorHelpLog);
                    return errorHelpLog;
                }
                return null;
            }
        }, new HCommunication() {
            @Override
            public void beforeSend() {
                this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103FC280004")));
            }

            @Override
            public void afterSend() {
            }

            @Override
            public void beforeReceive() {
            }

            @Override
            public void afterReceive() {
            }

            @Override
            public Object onParse() {
                if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                    byte[] received = HSerial.trimEnd(getReceivedBuffer());
                    // 通过验证
                    ErrorHelpLog errorHelpLog = new ErrorHelpLog();
                    errorHelpLog.setReceived(received);
                    errorHelpLog.setCtx(TroubleAnalyzeActivity.this);
                    errorHelpLog = ParseSerialsUtils.getErrorHelpLog(TroubleAnalyzeActivity.this, errorHelpLog);
                    return errorHelpLog;
                }
                return null;
            }
        }, new HCommunication() {
            @Override
            public void beforeSend() {
                this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103FC2C0004")));
            }

            @Override
            public void afterSend() {
            }

            @Override
            public void beforeReceive() {
            }

            @Override
            public void afterReceive() {
            }

            @Override
            public Object onParse() {
                if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                    byte[] received = HSerial.trimEnd(getReceivedBuffer());
                    // 通过验证
                    ErrorHelpLog errorHelpLog = new ErrorHelpLog();
                    errorHelpLog.setReceived(received);
                    errorHelpLog.setCtx(TroubleAnalyzeActivity.this);
                    errorHelpLog = ParseSerialsUtils.getErrorHelpLog(TroubleAnalyzeActivity.this, errorHelpLog);
                    return errorHelpLog;
                }
                return null;
            }
        }, new HCommunication() {
            @Override
            public void beforeSend() {
                this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103FC300004")));
            }

            @Override
            public void afterSend() {
            }

            @Override
            public void beforeReceive() {
            }

            @Override
            public void afterReceive() {
            }

            @Override
            public Object onParse() {
                if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                    byte[] received = HSerial.trimEnd(getReceivedBuffer());
                    // 通过验证
                    ErrorHelpLog errorHelpLog = new ErrorHelpLog();
                    errorHelpLog.setReceived(received);
                    errorHelpLog.setCtx(TroubleAnalyzeActivity.this);
                    errorHelpLog = ParseSerialsUtils.getErrorHelpLog(TroubleAnalyzeActivity.this, errorHelpLog);
                    return errorHelpLog;
                }
                return null;
            }
        }, new HCommunication() {
            @Override
            public void beforeSend() {
                this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103FC340004")));
            }

            @Override
            public void afterSend() {
            }

            @Override
            public void beforeReceive() {
            }

            @Override
            public void afterReceive() {
            }

            @Override
            public Object onParse() {
                if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                    byte[] received = HSerial.trimEnd(getReceivedBuffer());
                    // 通过验证
                    ErrorHelpLog errorHelpLog = new ErrorHelpLog();
                    errorHelpLog.setReceived(received);
                    errorHelpLog.setCtx(TroubleAnalyzeActivity.this);
                    errorHelpLog = ParseSerialsUtils.getErrorHelpLog(TroubleAnalyzeActivity.this, errorHelpLog);
                    return errorHelpLog;
                }
                return null;
            }
        }, new HCommunication() {
            @Override
            public void beforeSend() {
                this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103FC380004")));
            }

            @Override
            public void afterSend() {
            }

            @Override
            public void beforeReceive() {
            }

            @Override
            public void afterReceive() {
            }

            @Override
            public Object onParse() {
                if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                    byte[] received = HSerial.trimEnd(getReceivedBuffer());
                    // 通过验证
                    ErrorHelpLog errorHelpLog = new ErrorHelpLog();
                    errorHelpLog.setReceived(received);
                    errorHelpLog.setCtx(TroubleAnalyzeActivity.this);
                    errorHelpLog = ParseSerialsUtils.getErrorHelpLog(TroubleAnalyzeActivity.this, errorHelpLog);
                    return errorHelpLog;
                }
                return null;
            }
        }, new HCommunication() {
            @Override
            public void beforeSend() {
                this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0103FC3C0004")));
            }

            @Override
            public void afterSend() {
            }

            @Override
            public void beforeReceive() {
            }

            @Override
            public void afterReceive() {
            }

            @Override
            public Object onParse() {
                if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                    byte[] received = HSerial.trimEnd(getReceivedBuffer());
                    // 通过验证
                    ErrorHelpLog errorHelpLog = new ErrorHelpLog();
                    errorHelpLog.setReceived(received);
                    errorHelpLog.setCtx(TroubleAnalyzeActivity.this);
                    errorHelpLog = ParseSerialsUtils.getErrorHelpLog(TroubleAnalyzeActivity.this, errorHelpLog);
                    return errorHelpLog;
                }
                return null;
            }
        }
        };
        bluetoothHandler = new FailureLogHandler(this);
        if (HBluetooth.getInstance(this).isPrepared())
            HBluetooth.getInstance(this).setHandler(bluetoothHandler).setCommunications(hCommunications).HStart();
    }

    public void loadDictionary() {
        // 停止串口通信
        HBluetooth.getInstance(this).setHandler(bluetoothHandler);
    }
}
