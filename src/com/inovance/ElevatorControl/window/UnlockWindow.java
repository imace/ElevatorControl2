package com.inovance.elevatorcontrol.window;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.inovance.bluetoothtool.BluetoothTalk;
import com.inovance.bluetoothtool.BluetoothTool;
import com.inovance.bluetoothtool.SerialUtility;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.cache.ValueCache;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.handlers.UnlockHandler;
import com.inovance.elevatorcontrol.models.CommunicationCode;
import com.inovance.elevatorcontrol.utils.ParseSerialsUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UnlockWindow extends Activity implements Runnable {

    private static final String TAG = UnlockWindow.class.getSimpleName();

    private EditText passwordEditText;

    private Button positiveButton;

    private WritePasswordHandler handler;

    private BluetoothTalk[] talk;

    private static Handler syncHandler = new Handler();

    /**
     * 是否暂停 Task
     */
    private boolean running = false;

    /**
     * 是否正在写入密码
     */
    private boolean isSyncing;

    /**
     * 设备是否已经返回解锁结果
     */
    private boolean resulted;

    private Runnable syncTask;

    /**
     * 同步间隔
     */
    private static final int SYNC_TIME = 500;

    /**
     * 通信码索引位置
     */
    private int codeIndex;

    private ExecutorService pool = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.unlock_device_dialog_title);
        setContentView(R.layout.unlock_window_layout);

        handler = new WritePasswordHandler(this);

        passwordEditText = (EditText) findViewById(R.id.password);
        Button negativeButton = (Button) findViewById(R.id.negative_button);
        positiveButton = (Button) findViewById(R.id.positive_button);

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (positiveButton != null) {
                    if (charSequence.length() > 0) {
                        positiveButton.setEnabled(true);
                    } else {
                        positiveButton.setEnabled(false);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    startTask();
                    return true;
                }
                return false;
            }
        });

        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_OK);
                finish();
            }
        });

        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTask();
            }
        });

        syncTask = new Runnable() {
            @Override
            public void run() {
                if (running && !isSyncing && !resulted) {
                    pool.execute(UnlockWindow.this);
                    syncHandler.postDelayed(this, SYNC_TIME);
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        codeIndex = 0;
        running = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        running = false;
    }

    private void startTask() {
        BluetoothTool.getInstance().setCRCValue(BluetoothTool.CRCValueNone);
        String password = passwordEditText.getText().toString();
        int value = UnlockWindow.covertToInteger(password);
        if (value >= 0 && value < 65536) {
            createUnlockCommunication(value);
            // 开始解锁
            positiveButton.setEnabled(false);
            passwordEditText.setEnabled(false);

            isSyncing = false;
            resulted = false;

            syncHandler.postDelayed(syncTask, SYNC_TIME);
        } else {
            Toast.makeText(UnlockWindow.this, R.string.password_value_error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 设备解锁成功
     */
    private void onUnlocked() {
        resulted = true;
        BluetoothTool.getInstance().setUnlocked();
        Toast.makeText(UnlockWindow.this, R.string.unlock_device_successful, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    /**
     * 解锁失败
     *
     * @param message 失败信息
     */
    private void onFailedUnlock(String message) {
        resulted = true;

        positiveButton.setEnabled(true);
        passwordEditText.setEnabled(true);
        // 释放状态
        BluetoothTool.getInstance().setUnlocking(false);

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void createUnlockCommunication(final int value) {
        talk = new BluetoothTalk[1];
        talk[0] = new BluetoothTalk() {
            @Override
            public void beforeSend() {
                this.setSendBuffer(SerialUtility.crc16("0106"
                        + ApplicationConfig.UnlockDeviceCode
                        + String.format("%04x", value)));
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
                if (SerialUtility.isCRC16Valid(getReceivedBuffer())) {
                    return SerialUtility.trimEnd(getReceivedBuffer());
                }
                return null;
            }
        };
    }

    public static int covertToInteger(String string) {
        int intValue;
        try {
            intValue = Integer.parseInt(string);
        } catch (NumberFormatException e) {
            intValue = -1;
        } catch (NullPointerException e) {
            intValue = -1;
        }
        return intValue;
    }

    @Override
    public void run() {
        if (BluetoothTool.getInstance().isConnected()) {
            if (handler != null && talk != null) {
                BluetoothTool.getInstance().unlock(handler, talk);
            }
        }
    }

    private class WritePasswordHandler extends UnlockHandler {

        public WritePasswordHandler(Activity activity) {
            super(activity);
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            isSyncing = true;
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof byte[]) {
                byte[] data = (byte[]) msg.obj;
                String response = SerialUtility.byte2HexStr(data);
                int errorIndex = ParseSerialsUtils.getErrorIndex(response);
                if (errorIndex == -1) {
                    // 设备已解锁
                    onUnlocked();
                } else if (errorIndex == 3) {
                    // CRC 校验错误
                    List<CommunicationCode> codeList = ValueCache.getInstance().getCodeList();
                    if (codeIndex < codeList.size()) {
                        CommunicationCode code = codeList.get(codeIndex);
                        BluetoothTool.getInstance().setCRCValue(code.getCrcValue());
                        codeIndex++;
                    } else {
                        onFailedUnlock(getString(R.string.unlock_device_failed));
                    }
                } else {
                    // 其他错误
                    String message = ApplicationConfig.ERROR_NAME_ARRAY[errorIndex];
                    onFailedUnlock(message);
                }
            }
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            isSyncing = false;
        }
    }
}