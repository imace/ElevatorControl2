package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
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
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.daos.ParameterGroupSettingsDao;
import com.kio.ElevatorControl.models.ListHolder;
import com.kio.ElevatorControl.models.ParameterGroupSettings;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.kio.ElevatorControl.utils.GenerateJSON;
import com.kio.ElevatorControl.utils.ParseSerialsUtils;
import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.ProgressBar;
import org.holoeverywhere.widget.TextView;

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

    private ProgressBar downloadProgressBar;

    private EditText fileNameEditText;

    private List<HCommunication[]> communicationsList;

    private AlertDialog downloadDialog;

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
    protected void onResume() {
        super.onResume();
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
        downloadDialog = builder.create();
        downloadDialog.setCancelable(false);
        downloadDialog.setCanceledOnTouchOutside(false);
        downloadDialog.show();
        downloadDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
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
            int maxProgress = 0;
            fileNameEditText.setVisibility(View.GONE);
            parameterGroupLists = ParameterGroupSettingsDao.findAll(ParameterDownloadActivity.this);
            communicationsList = new ArrayList<HCommunication[]>();
            for (ParameterGroupSettings groupItem : parameterGroupLists) {
                List<ParameterSettings> detailSettings = groupItem.getParametersettings().getList();
                HCommunication[] communications = combinationCommunications(detailSettings);
                maxProgress += communications.length;
                communicationsList.add(communications);
            }
            downloadProgressBar.setMax(maxProgress);
            downloadProgressBar.setProgress(0);
            downloadProgressBar.setVisibility(View.VISIBLE);
            ParameterDownloadActivity.this.startCommunication(0);
        } else {
            // 未能保存文件时弹出警告
            AlertDialog.Builder builder = new AlertDialog.Builder(ParameterDownloadActivity.this);
            builder.setTitle(R.string.save_file_failed_title);
            builder.setMessage(R.string.cannot_save_file);
            builder.setPositiveButton(R.string.dialog_btn_ok, null);
        }
    }

    /**
     * Combination Communications
     *
     * @param list ParameterSettings List
     * @return HCommunication[]
     */
    private HCommunication[] combinationCommunications(final List<ParameterSettings> list) {
        final int size = list.size();
        final int count = size <= 10 ? 1 : ((size - size % 10) / 10 + (size % 10 == 0 ? 0 : 1));
        HCommunication[] communications = new HCommunication[count];
        for (int i = 0; i < count; i++) {
            final int position = i;
            final ParameterSettings firstItem = list.get(position * 10);
            final int length = size <= 10 ? size : (size % 10 == 0 ? 10 : ((position == count - 1) ? size % 10 : 10));
            communications[i] = new HCommunication() {
                @Override
                public void beforeSend() {
                    this.setSendBuffer(HSerial.crc16(HSerial
                            .hexStr2Ints("0103"
                                    + ParseSerialsUtils.getCalculatedCode(firstItem)
                                    + String.format("%04x ", length)
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
                        byte[] data = HSerial.trimEnd(getReceivedBuffer());
                        short bytesLength = ByteBuffer.wrap(new byte[]{data[2], data[3]}).getShort();
                        if (length * 2 == bytesLength) {
                            List<ParameterSettings> tempList = new ArrayList<ParameterSettings>();
                            for (int j = 0; j < length; j++) {
                                ParameterSettings item = list.get(position * 10 + j);
                                byte[] tempData = HSerial.crc16(HSerial.hexStr2Ints("01030002"
                                        + HSerial.byte2HexStr(new byte[]{data[4 + j * 2], data[5 + j * 2]})));
                                item.setReceived(tempData);
                                tempList.add(item);
                            }
                            ListHolder holder = new ListHolder();
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
            if (HBluetooth.getInstance(this).isPrepared()) {
                HBluetooth.getInstance(this)
                        .setHandler(downloadParameterHandler)
                        .setCommunications(communicationsList.get(position))
                        .Start();
            }
        }
    }

    // ===================================== Download Parameter Handler ======================================

    /**
     * 下载参数配置Handler
     */
    private class DownloadParameterHandler extends HHandler {

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
            HCommunication[] communications = ParameterDownloadActivity.this.communicationsList.get(index);
            if (communications.length == receiveCount) {
                ParameterDownloadActivity.this.downloadProgressBar.incrementProgress(receiveCount);
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
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // 重新获取
                receiveCount = 0;
                ParameterDownloadActivity.this.startCommunication(index);
            }
        }

        @Override
        public void onTalkReceive(Message msg) {
            if (msg.obj != null && msg.obj instanceof ListHolder) {
                ListHolder holder = (ListHolder) msg.obj;
                for (ParameterSettings item : holder.getParameterSettingsList()) {
                    if (!item.getName().contains(ApplicationConfig.RETAIN_NAME)) {
                        tempParameterSettingsList.add(item);
                    }
                }
                receiveCount++;
            }
        }
    }

}