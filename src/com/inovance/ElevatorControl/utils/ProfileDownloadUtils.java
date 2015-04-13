package com.inovance.elevatorcontrol.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.inovance.bluetoothtool.BluetoothTalk;
import com.inovance.bluetoothtool.BluetoothTool;
import com.inovance.bluetoothtool.SerialUtility;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.daos.ParameterGroupSettingsDao;
import com.inovance.elevatorcontrol.handlers.UnlockHandler;
import com.inovance.elevatorcontrol.models.ObjectListHolder;
import com.inovance.elevatorcontrol.models.ParameterGroupSettings;
import com.inovance.elevatorcontrol.models.ParameterSettings;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-6-9.
 * Time: 10:12.
 */
public class ProfileDownloadUtils implements Runnable {

    /**
     * 用于读取参数的通信内容
     */
    private List<BluetoothTalk[]> communicationsList;

    private int currentPosition;

    private List<ParameterGroupSettings> parameterGroupLists;

    /**
     * 当前读取的参数位置
     */
    private TextView currentTextView;

    /**
     * 读取参数进度
     */
    private ProgressBar downloadProgressBar;

    /**
     * 读取 Dialog
     */
    private AlertDialog downloadDialog;

    private Activity parentActivity;

    private DownloadHandler downloadHandler;

    public static interface OnDownloadCompleteListener {
        void onComplete(String JSONString);
    }

    private OnDownloadCompleteListener mListener;

    private ExecutorService pool = Executors.newSingleThreadExecutor();

    public ProfileDownloadUtils(Activity activity) {
        this.parentActivity = activity;
        downloadHandler = new DownloadHandler(activity);
        AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.parameters_duplicate_dialog, null);
        dialogView.findViewById(R.id.progress_view).setVisibility(View.VISIBLE);
        dialogView.findViewById(R.id.file_name).setVisibility(View.GONE);
        currentTextView = (TextView) dialogView.findViewById(R.id.current_progress);
        downloadProgressBar = (ProgressBar) dialogView.findViewById(R.id.progress_bar);
        parameterGroupLists = ParameterGroupSettingsDao.findAll(parentActivity);
        communicationsList = new ArrayList<BluetoothTalk[]>();
        int maxProgress = 0;
        for (ParameterGroupSettings groupItem : parameterGroupLists) {
            BluetoothTalk[] communications = createCommunications(groupItem.getParametersettings().getList());
            maxProgress += communications.length;
            communicationsList.add(communications);
        }
        downloadProgressBar.setMax(maxProgress);
        downloadProgressBar.setProgress(0);
        currentTextView.setText("0%");
        builder.setTitle(R.string.save_file_dialog_title);
        builder.setView(dialogView);
        builder.setNegativeButton(R.string.dialog_btn_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                BluetoothTool.getInstance().setCommunications(null);
                BluetoothTool.getInstance().setHandler(null);
            }
        });
        downloadDialog = builder.create();
        downloadDialog.setCancelable(false);
        downloadDialog.setCanceledOnTouchOutside(false);
        downloadDialog.show();
    }

    /**
     * 开始下载参数配置
     */
    public void startDownloadProfile(OnDownloadCompleteListener listener) {
        mListener = listener;
        currentPosition = 0;
        pool.execute(this);
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
                    this.setSendBuffer(SerialUtility.crc16("0103"
                            + ParseSerialsUtils.getCalculatedCode(firstItem)
                            + String.format("%04x", length)
                            + "0001"));
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
                                byte[] tempData = SerialUtility.crc16("01030002"
                                        + SerialUtility.byte2HexStr(new byte[]{data[4 + j * 2], data[5 + j * 2]}));
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
            downloadHandler.index = position;
            if (BluetoothTool.getInstance().isPrepared()) {
                BluetoothTool.getInstance()
                        .setHandler(downloadHandler)
                        .setCommunications(communicationsList.get(position))
                        .startTask();
            }
        }
    }

    @Override
    public void run() {
        startCommunication(currentPosition);
    }

    // =====================================下载参数配置 Handler======================================

    private class DownloadHandler extends UnlockHandler {

        private int index = 0;

        private int receiveCount = 0;

        private List<ParameterSettings> tempParameterSettingsList;

        public DownloadHandler(Activity activity) {
            super(activity);
            TAG = DownloadHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            tempParameterSettingsList = new ArrayList<ParameterSettings>();
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            BluetoothTalk[] communications = ProfileDownloadUtils.this.communicationsList.get(index);
            if (communications.length == receiveCount) {
                ProfileDownloadUtils.this.downloadProgressBar.incrementProgressBy(receiveCount);
                int currentProgress = ProfileDownloadUtils.this.downloadProgressBar.getProgress();
                int maxProgress = ProfileDownloadUtils.this.downloadProgressBar.getMax();
                int calculateProgress = 0;
                if (currentProgress == maxProgress) {
                    calculateProgress = 100;
                } else {
                    calculateProgress = (100 * currentProgress) / maxProgress;
                }
                ProfileDownloadUtils.this.currentTextView.setText(calculateProgress + "%");
                ProfileDownloadUtils.this.parameterGroupLists
                        .get(index)
                        .getParametersettings()
                        .setList(tempParameterSettingsList);
                index++;
                receiveCount = 0;
                if (index < ProfileDownloadUtils.this.communicationsList.size()) {
                    currentPosition = index;
                    pool.execute(ProfileDownloadUtils.this);
                }
                if (ProfileDownloadUtils.this.downloadProgressBar.getMax() ==
                        ProfileDownloadUtils.this.downloadProgressBar.getProgress()) {
                    if (ProfileDownloadUtils.this.downloadDialog != null) {
                        ProfileDownloadUtils.this.downloadDialog.dismiss();
                    }
                    String profileJSON = GenerateJSON.getInstance()
                            .generateProfileJSON(ProfileDownloadUtils.this.parameterGroupLists);
                    if (mListener != null) {
                        mListener.onComplete(profileJSON);
                    }
                }
            } else {
                receiveCount = 0;
                currentPosition = index;
                pool.execute(ProfileDownloadUtils.this);
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

        @Override
        public void onTalkError(Message msg) {
            super.onTalkError(msg);
            receiveCount = 0;
            currentPosition = index;
            pool.execute(ProfileDownloadUtils.this);
        }
    }

}
