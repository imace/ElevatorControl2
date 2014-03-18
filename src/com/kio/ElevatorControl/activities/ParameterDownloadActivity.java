package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HHandler;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.daos.ParameterGroupSettingsDao;
import com.kio.ElevatorControl.models.ParameterGroupSettings;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.kio.ElevatorControl.utils.ParseSerialsUtils;
import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.ProgressBar;
import org.holoeverywhere.widget.TextView;

import java.io.File;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-10.
 * Time: 11:35.
 */
// TODO ////////////////////// 16进制 0xFR 无法转换问题 /////////////////////////
public class ParameterDownloadActivity extends Activity {

    private final static String TAG = ParameterDownloadActivity.class.getSimpleName();

    private final static String DIRECTORY_NAME = "Profile";

    private DownloadParameterHandler downloadParameterHandler;

    private ProgressBar downloadProgressBar;

    private EditText fileNameEditText;

    private List<ParameterGroupSettings> parameterGroupLists;

    @InjectView(R.id.equipment_model)
    TextView equipmentModel;

    @InjectView(R.id.manufacturers_serial_number)
    TextView manufacturersSerialNumber;

    @InjectView(R.id.current_parameters_version)
    TextView currentParametersVersion;

    @InjectView(R.id.parameters_update_date)
    TextView parametersUpdateDate;

    @InjectView(R.id.download_button)
    View downloadButton;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.parameter_download_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_parameter_download);
        Views.inject(this);
        downloadParameterHandler = new DownloadParameterHandler(this);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(ParameterDownloadActivity.this);
        LayoutInflater inflater = ParameterDownloadActivity.this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.parameters_duplicate_dialog, null);
        downloadProgressBar = (ProgressBar) dialogView.findViewById(R.id.progress_bar);
        fileNameEditText = (EditText) dialogView.findViewById(R.id.file_name);
        builder.setTitle(R.string.save_file_dialog_title);
        builder.setView(dialogView);
        builder.setPositiveButton(R.string.dialog_btn_ok, null);
        builder.setNegativeButton(R.string.dialog_btn_cancel, null);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ParameterDownloadActivity.this.saveProfileToLocal(fileNameEditText.getText().toString());
            }
        });
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
            fileNameEditText.setVisibility(View.GONE);
            parameterGroupLists = ParameterGroupSettingsDao.findAll(ParameterDownloadActivity.this);
            // Start to download
            int maxProgress = 0;
            for (ParameterGroupSettings groupItem : parameterGroupLists) {
                maxProgress += groupItem.getParametersettings().getList().size();
            }
            downloadProgressBar.setMax(maxProgress);
            downloadProgressBar.setProgress(0);
            downloadProgressBar.setVisibility(View.VISIBLE);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (ParameterGroupSettings groupItem : parameterGroupLists) {
                        List<ParameterSettings> detailSettings = groupItem.getParametersettings().getList();
                        int detailSize = detailSettings.size();
                        HCommunication[] communications = new HCommunication[detailSize];
                        for (int i = 0; i < detailSize; i++) {
                            final ParameterSettings item = detailSettings.get(i);
                            Log.v(TAG, item.getCode());
                            communications[i] = new HCommunication() {
                                @Override
                                public void beforeSend() {
                                    this.setSendBuffer(HSerial.crc16(HSerial
                                            .hexStr2Ints("0103"
                                                    + ParseSerialsUtils.getCalculatedCode(item)
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
                                    if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                                        byte[] received = HSerial.trimEnd(getReceivedBuffer());
                                        item.setReceived(received);
                                        return item;
                                    }
                                    return null;
                                }
                            };
                        }
                        if (HBluetooth.getInstance(ParameterDownloadActivity.this).isPrepared()) {
                            HBluetooth.getInstance(ParameterDownloadActivity.this)
                                    .setHandler(downloadParameterHandler)
                                    .setCommunications(communications)
                                    .Start();
                            try {
                                // 休眠一段时间等待接受完毕
                                Thread.sleep(detailSize * 120);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }).start();
        } else {
            // 未能保存文件时弹出警告
            AlertDialog.Builder builder = new AlertDialog.Builder(ParameterDownloadActivity.this);
            builder.setTitle(R.string.save_file_failed_title);
            builder.setMessage(R.string.cannot_save_file);
            builder.setPositiveButton(R.string.dialog_btn_ok, null);
        }
    }

    // ===================================== Download Parameter Handler ======================================

    /**
     * 下载参数配置Handler
     */
    private class DownloadParameterHandler extends HHandler {

        private int receiveDataCount = 0;

        public DownloadParameterHandler(Activity activity) {
            super(activity);
            TAG = DownloadParameterHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
        }

        @Override
        public void onTalkReceive(Message msg) {
            if (msg.obj != null && msg.obj instanceof ParameterSettings) {
                ParameterSettings detailItem = (ParameterSettings) msg.obj;
                receiveDataCount++;
                ParameterDownloadActivity.this.downloadProgressBar.setProgress(receiveDataCount);
            }
        }
    }

}