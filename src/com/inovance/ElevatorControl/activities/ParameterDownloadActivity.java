package com.inovance.ElevatorControl.activities;

import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import butterknife.OnClick;
import butterknife.Views;
import com.bluetoothtool.BluetoothHandler;
import com.bluetoothtool.BluetoothTalk;
import com.bluetoothtool.BluetoothTool;
import com.bluetoothtool.SerialUtility;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.daos.ParameterGroupSettingsDao;
import com.inovance.ElevatorControl.models.ObjectListHolder;
import com.inovance.ElevatorControl.models.ParameterGroupSettings;
import com.inovance.ElevatorControl.models.ParameterSettings;
import com.inovance.ElevatorControl.utils.GenerateJSON;
import com.inovance.ElevatorControl.utils.ParseSerialsUtils;
import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.ProgressBar;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-10.
 * Time: 11:35.
 */
public class ParameterDownloadActivity extends Activity {

    private final static String TAG = ParameterDownloadActivity.class.getSimpleName();

    private final static String DIRECTORY_NAME = "Profile";

    private DownloadParameterHandler downloadParameterHandler;

    private View progressView;

    private ProgressBar downloadProgressBar;

    private TextView totalTextView;

    private TextView currentTextView;

    private EditText fileNameEditText;

    private List<BluetoothTalk[]> communicationsList;

    private AlertDialog downloadDialog;

    private Button confirmButton;

    private Button cancelButton;

    private List<ParameterGroupSettings> parameterGroupLists;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setTitle(R.string.parameter_download_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_parameter_download);
        Views.inject(this);
        downloadParameterHandler = new DownloadParameterHandler(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 下载当前设备参数
     */
    @OnClick(R.id.download_button)
    void onDownloadButtonClick() {
        //弹出下载框
        if (BluetoothTool.getInstance(this).isConnected()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ParameterDownloadActivity.this);
            LayoutInflater inflater = ParameterDownloadActivity.this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.parameters_duplicate_dialog, null);
            progressView = dialogView.findViewById(R.id.progress_view);
            downloadProgressBar = (ProgressBar) dialogView.findViewById(R.id.progress_bar);
            totalTextView = (TextView) dialogView.findViewById(R.id.total_items);
            currentTextView = (TextView) dialogView.findViewById(R.id.current_item);
            fileNameEditText = (EditText) dialogView.findViewById(R.id.file_name);
            // 检测输入框内容是否为空
            fileNameEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                    if (confirmButton != null) {
                        if (charSequence.length() > 0) {
                            confirmButton.setEnabled(true);
                        } else {
                            confirmButton.setEnabled(false);
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });
            builder.setTitle(R.string.save_file_dialog_title);
            builder.setView(dialogView);
            builder.setPositiveButton(R.string.dialog_btn_ok, null);
            builder.setNegativeButton(R.string.dialog_btn_cancel, null);
            downloadDialog = builder.create();
            downloadDialog.setCancelable(false);
            downloadDialog.setCanceledOnTouchOutside(false);
            downloadDialog.show();
            cancelButton = downloadDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            confirmButton = downloadDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            confirmButton.setEnabled(false);
            downloadDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ParameterDownloadActivity.this.saveProfileToLocal(fileNameEditText.getText().toString());
                }
            });
            downloadDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    downloadDialog.dismiss();
                }
            });
        } else {
            Toast.makeText(this,
                    R.string.not_connect_device_error,
                    android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * 保存配置文件
     *
     * @param fileName 保存的文件名
     */
    private void saveProfileToLocal(String fileName) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File directory = new File(getApplicationContext().getExternalCacheDir().getPath()
                    + "/"
                    + DIRECTORY_NAME);
            if (!directory.exists()) {
                directory.mkdir();
            }
            int maxProgress = 0;
            fileNameEditText.setVisibility(View.GONE);
            parameterGroupLists = ParameterGroupSettingsDao.findAll(ParameterDownloadActivity.this);
            communicationsList = new ArrayList<BluetoothTalk[]>();
            for (ParameterGroupSettings groupItem : parameterGroupLists) {
                BluetoothTalk[] communications = createCommunications(groupItem.getParametersettings().getList());
                maxProgress += communications.length;
                communicationsList.add(communications);
            }
            downloadProgressBar.setMax(maxProgress);
            downloadProgressBar.setProgress(0);
            totalTextView.setText("100");
            currentTextView.setText("0/100");
            progressView.setVisibility(View.VISIBLE);
            ParameterDownloadActivity.this.startCommunication(0);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(ParameterDownloadActivity.this);
            builder.setTitle(R.string.save_file_failed_title);
            builder.setMessage(R.string.cannot_save_file);
            builder.setPositiveButton(R.string.dialog_btn_ok, null);
        }
    }

    /**
     * Create Communications
     *
     * @param list ParameterSettings List
     * @return BluetoothTalk[]
     */
    private BluetoothTalk[] createCommunications(final List<ParameterSettings> list) {
        final int size = list.size();
        final int count = size <= 10 ? 1 : ((size - size % 10) / 10 + (size % 10 == 0 ? 0 : 1));
        BluetoothTalk[] communications = new BluetoothTalk[count];
        for (int i = 0; i < count; i++) {
            final int position = i;
            final ParameterSettings firstItem = list.get(position * 10);
            final int length = size <= 10 ? size : (size % 10 == 0 ? 10 : ((position == count - 1) ? size % 10 : 10));
            communications[i] = new BluetoothTalk() {
                @Override
                public void beforeSend() {
                    this.setSendBuffer(SerialUtility.crc16(SerialUtility
                            .hexStr2Ints("0103"
                                    + ParseSerialsUtils.getCalculatedCode(firstItem)
                                    + String.format("%04x", length)
                                    + "0001")));
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
                        byte[] data = SerialUtility.trimEnd(getReceivedBuffer());
                        short bytesLength = ByteBuffer.wrap(new byte[]{data[2], data[3]}).getShort();
                        if (length * 2 == bytesLength) {
                            List<ParameterSettings> tempList = new ArrayList<ParameterSettings>();
                            for (int j = 0; j < length; j++) {
                                ParameterSettings item = list.get(position * 10 + j);
                                byte[] tempData = SerialUtility.crc16(SerialUtility.hexStr2Ints("01030002"
                                        + SerialUtility.byte2HexStr(new byte[]{data[4 + j * 2], data[5 + j * 2]})));
                                item.setReceived(tempData);
                                tempList.add(item);
                            }
                            ObjectListHolder holder = new ObjectListHolder();
                            holder.setParameterSettingsList(tempList);
                            return holder;
                        }
                    }
                    return null;
                }
            };
        }
        return communications;
    }

    /**
     * 开始发送并且接收参数
     *
     * @param position communications Lists Index
     */
    private void startCommunication(int position) {
        if (position >= 0 && position < communicationsList.size()) {
            downloadParameterHandler.index = position;
            if (BluetoothTool.getInstance(this).isConnected()) {
                BluetoothTool.getInstance(this)
                        .setHandler(downloadParameterHandler)
                        .setCommunications(communicationsList.get(position))
                        .send();
                if (confirmButton != null && cancelButton != null) {
                    confirmButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                }
            } else {
                Toast.makeText(this,
                        R.string.not_connect_device_error,
                        android.widget.Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    // ===================================== Download Parameter Handler ======================================

    /**
     * 下载参数配置Handler
     */
    private class DownloadParameterHandler extends BluetoothHandler {

        private int index = 0;

        private int receiveCount = 0;

        private List<ParameterSettings> tempParameterSettingsList;

        public DownloadParameterHandler(Activity activity) {
            super(activity);
            TAG = DownloadParameterHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            tempParameterSettingsList = new ArrayList<ParameterSettings>();
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            BluetoothTalk[] communications = ParameterDownloadActivity.this.communicationsList.get(index);
            if (communications.length == receiveCount) {
                ParameterDownloadActivity.this.downloadProgressBar.incrementProgress(receiveCount);
                int currentProgress = ParameterDownloadActivity.this.downloadProgressBar.getProgress();
                int maxProgress = ParameterDownloadActivity.this.downloadProgressBar.getMax();
                int calculateProgress = 0;
                if (currentProgress == maxProgress) {
                    calculateProgress = 100;
                } else {
                    calculateProgress = (100 * currentProgress) / maxProgress;
                }
                ParameterDownloadActivity.this.currentTextView.setText(calculateProgress + "/100");
                ParameterDownloadActivity.this.parameterGroupLists
                        .get(index)
                        .getParametersettings()
                        .setList(tempParameterSettingsList);
                index++;
                receiveCount = 0;
                if (index < ParameterDownloadActivity.this.communicationsList.size()) {
                    ParameterDownloadActivity.this.startCommunication(index);
                }
                if (ParameterDownloadActivity.this.downloadProgressBar.getMax() ==
                        ParameterDownloadActivity.this.downloadProgressBar.getProgress()) {
                    if (ParameterDownloadActivity.this.downloadDialog != null) {
                        ParameterDownloadActivity.this.downloadDialog.dismiss();
                    }
                    File fileName = new File(getApplicationContext().getExternalCacheDir().getPath()
                            + "/"
                            + DIRECTORY_NAME
                            + "/"
                            + fileNameEditText.getText().toString());
                    if (!fileName.exists()) {
                        try {
                            fileName.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    String JSONString = GenerateJSON
                            .getInstance()
                            .generateProfileJSON(ParameterDownloadActivity.this.parameterGroupLists);
                    try {
                        FileOutputStream outputStream = new FileOutputStream(fileName);
                        outputStream.write(JSONString.getBytes());
                        outputStream.close();
                        Toast.makeText(ParameterDownloadActivity.this,
                                R.string.download_parameter_save_successful,
                                Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(ParameterDownloadActivity.this,
                                R.string.save_parameter_failed,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                receiveCount = 0;
                ParameterDownloadActivity.this.startCommunication(index);
            }
        }

        @Override
        public void onTalkReceive(Message msg) {
            if (msg.obj != null && msg.obj instanceof ObjectListHolder) {
                ObjectListHolder holder = (ObjectListHolder) msg.obj;
                for (ParameterSettings item : holder.getParameterSettingsList()) {
                    tempParameterSettingsList.add(item);
                }
                receiveCount++;
            }
        }
    }

}