package com.inovance.elevatorcontrol.window;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.inovance.bluetoothtool.BluetoothTalk;
import com.inovance.bluetoothtool.BluetoothTool;
import com.inovance.bluetoothtool.SerialUtility;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.handlers.UnlockHandler;
import com.inovance.elevatorcontrol.utils.ParseSerialsUtils;

public class UnlockWindow extends Activity {

    private static final String TAG = UnlockWindow.class.getSimpleName();

    private EditText passwordEditText;

    private Button positiveButton;

    private WritePasswordHandler handler;

    private BluetoothTalk[] talk;

    /**
     * 是否正在写入密码
     */
    private boolean isSyncing;

    /**
     * 设备是否已经返回解锁结果
     */
    private boolean responsive;

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
                String password = passwordEditText.getText().toString();
                int value = UnlockWindow.covertToInteger(password);
                if (value >= 0 && value < 65536) {
                    createUnlockCommunication(value);
                    startUnlockCommunication();
                } else {
                    Toast.makeText(UnlockWindow.this, R.string.password_value_error, Toast.LENGTH_SHORT).show();
                }
            }
        });
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

    private void startUnlockCommunication() {
        isSyncing = false;
        responsive = false;
        if (BluetoothTool.getInstance().isConnected()) {
            if (handler != null && talk != null) {
                positiveButton.setEnabled(false);
                passwordEditText.setEnabled(false);
                new CountDownTimer(1800, 600) {

                    @Override
                    public void onTick(long millisUntilFinished) {
                        if (responsive) {
                            cancel();
                        }
                        if (!isSyncing && !responsive) {
                            BluetoothTool.getInstance().unlock(handler, talk);
                        }
                    }

                    @Override
                    public void onFinish() {
                        if (!responsive) {
                            positiveButton.setEnabled(true);
                            passwordEditText.setEnabled(true);
                            // 释放状态
                            BluetoothTool.getInstance().setUnlocking(false);
                            Toast.makeText(UnlockWindow.this, R.string.unlock_failed_message, Toast.LENGTH_SHORT).show();
                        }
                    }
                }.start();
            }
        }
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
                responsive = true;
                byte[] data = (byte[]) msg.obj;
                String response = SerialUtility.byte2HexStr(data);
                int errorIndex = ParseSerialsUtils.getErrorIndex(response);
                if (errorIndex == -1) {
                    // 设备已解锁
                    BluetoothTool.getInstance().setUnlocked();
                    Toast.makeText(UnlockWindow.this, R.string.unlock_device_successful, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    positiveButton.setEnabled(true);
                    passwordEditText.setEnabled(true);
                    // 释放状态
                    BluetoothTool.getInstance().setUnlocking(false);
                    String message = ApplicationConfig.ERROR_NAME_ARRAY[errorIndex];
                    Toast.makeText(UnlockWindow.this, message, Toast.LENGTH_SHORT).show();
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