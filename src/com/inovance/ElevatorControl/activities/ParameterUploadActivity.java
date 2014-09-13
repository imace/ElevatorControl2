package com.inovance.elevatorcontrol.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.view.*;
import android.widget.*;
import butterknife.InjectView;
import butterknife.Views;
import com.inovance.bluetoothtool.*;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.daos.ProfileDao;
import com.inovance.elevatorcontrol.daos.RealTimeMonitorDao;
import com.inovance.elevatorcontrol.models.ParameterSettings;
import com.inovance.elevatorcontrol.models.Profile;
import com.inovance.elevatorcontrol.models.RealTimeMonitor;
import com.inovance.elevatorcontrol.utils.LogUtils;
import com.inovance.elevatorcontrol.utils.ParseSerialsUtils;
import com.inovance.elevatorcontrol.utils.TextLocalize;
import com.inovance.elevatorcontrol.views.dialogs.CustomDialog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-10.
 * Time: 11:35.
 */
public class ParameterUploadActivity extends Activity {

    private static final String TAG = ParameterUploadActivity.class.getSimpleName();

    /**
     * 参数保存目录
     */
    private final static String DIRECTORY_NAME = "Profile";

    /**
     * 可写入的参数列表 List View
     */
    @InjectView(R.id.upload_list)
    ListView listView;

    /**
     * 参数列表 ListView Adapter
     */
    private LocalProfileAdapter profileAdapter;

    /**
     * 写入参数的通信内容
     */
    private List<BluetoothTalk[]> communicationsList;

    private List<ParameterSettings> parameterSettingsList;

    /**
     * 写入参数 Handler
     */
    private UploadParameterHandler uploadParameterHandler;

    /**
     * 配置文件列表
     */
    private List<Profile> profileList;

    /**
     * 写入 Dialog
     */
    private AlertDialog uploadDialog;

    /**
     * 上传结束错误信息
     */
    private TextView uploadTipsTextView;

    /**
     * 上传文字指示
     */
    private View progressView;

    /**
     * 当前进度文字指示
     */
    private TextView currentProgress;

    /**
     * 写入进度
     */
    private ProgressBar uploadProgressBar;

    /**
     * 上传完毕后出错统计
     */
    private ScrollView tipsView;

    /**
     * Dialog 取消按钮
     */
    private Button dialogButton;

    /**
     * 用于取得电梯运行状态的通信内容
     */
    private BluetoothTalk[] getElevatorStatusCommunication;

    private ElevatorStatusHandler elevatorStatusHandler;

    /**
     * 电梯运行状态是否已读取到
     */
    private boolean hasGetElevatorStatus;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setTitle(R.string.parameter_upload_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_parameter_upload);
        Views.inject(this);
        profileList = ProfileDao.findAll(this);
        uploadParameterHandler = new UploadParameterHandler(this);
        elevatorStatusHandler = new ElevatorStatusHandler(this);
        hasGetElevatorStatus = false;
        profileAdapter = new LocalProfileAdapter();
        listView.setAdapter(profileAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BluetoothTool.getInstance().setHandler(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BluetoothTool.getInstance().setHandler(null);
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
     * 读取电梯运行状态
     *
     * @param index ListView Item Index
     */
    private void getElevatorStatus(final int index) {
        if (getElevatorStatusCommunication == null) {
            final RealTimeMonitor monitor = RealTimeMonitorDao.findByStateID(this,
                    ApplicationConfig.RunningStatusType);
            if (monitor != null) {
                getElevatorStatusCommunication = new BluetoothTalk[]{
                        new BluetoothTalk() {
                            @Override
                            public void beforeSend() {
                                this.setSendBuffer(SerialUtility.crc16("0103"
                                        + monitor.getCode()
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
                                    if (data.length == 8) {
                                        monitor.setReceived(data);
                                        return monitor;
                                    }
                                }
                                return null;
                            }
                        }
                };
            }
        }
        if (getElevatorStatusCommunication != null) {
            if (BluetoothTool.getInstance().isPrepared()) {
                if (!ParameterUploadActivity.this.hasGetElevatorStatus) {
                    elevatorStatusHandler.index = index;
                    BluetoothTool.getInstance()
                            .setCommunications(getElevatorStatusCommunication)
                            .setHandler(elevatorStatusHandler)
                            .send();
                }

            }
        }
    }

    /**
     * 已经读取到电梯状态
     *
     * @param position Item position
     * @param status   Status
     */
    private void onGetElevatorStatus(int position, int status) {
        if (status == 3) {
            // 电梯停机状态
            Profile profile = profileList.get(position);
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File directory = new File(getApplicationContext().getExternalCacheDir().getPath()
                        + "/"
                        + DIRECTORY_NAME);
                File file = new File(directory, profile.getFileName());
                try {
                    InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file));
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String receiveString = "";
                    StringBuilder stringBuilder = new StringBuilder();
                    while ((receiveString = bufferedReader.readLine()) != null) {
                        stringBuilder.append(receiveString);
                    }
                    bufferedReader.close();
                    inputStreamReader.close();
                    JSONArray groups = new JSONArray(stringBuilder.toString());
                    // 生成通讯内容
                    generateCommunicationsList(groups);
                    // 开始上传参数
                    uploadParameterHandler.receiveCount = 0;
                    ParameterUploadActivity.this.startCommunication(0);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = ParameterUploadActivity.this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.parameters_upload_dialog, null);
            tipsView = (ScrollView) dialogView.findViewById(R.id.tips_view);
            progressView = dialogView.findViewById(R.id.progress_view);
            currentProgress = (TextView) dialogView.findViewById(R.id.current_progress);
            uploadTipsTextView = (TextView) dialogView.findViewById(R.id.upload_tips);
            uploadProgressBar = (ProgressBar) dialogView.findViewById(R.id.progress_bar);
            uploadProgressBar.setProgress(0);
            uploadProgressBar.setMax(parameterSettingsList.size());
            currentProgress.setText("0%");
            builder.setTitle(R.string.uploading_profile_text);
            builder.setView(dialogView);
            builder.setNegativeButton(R.string.dialog_btn_cancel, null);
            uploadDialog = builder.create();
            uploadDialog.setCancelable(false);
            uploadDialog.setCanceledOnTouchOutside(false);
            uploadDialog.show();
            dialogButton = uploadDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            dialogButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    BluetoothTool.getInstance().setHandler(null);
                    uploadProgressBar.setProgress(0);
                    uploadDialog.dismiss();
                    uploadDialog = null;
                }
            });
        } else {
            // 电梯处于运行状态
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.upload_profile_failed_title)
                    .setMessage(R.string.upload_profile_failed_message)
                    .setPositiveButton(R.string.dialog_btn_ok, null);
            builder.create().show();
        }
    }

    /**
     * Generate Upload Communications List
     *
     * @param groupArray JSON Array
     */
    private void generateCommunicationsList(JSONArray groupArray) {
        communicationsList = new ArrayList<BluetoothTalk[]>();
        parameterSettingsList = new ArrayList<ParameterSettings>();
        int size = groupArray.length();
        for (int i = 0; i < size; i++) {
            try {
                JSONObject groupsJSONObject = groupArray.getJSONObject(i);
                JSONArray detailArray = groupsJSONObject.getJSONArray("parameterSettings".toUpperCase());
                int length = detailArray.length();
                List<BluetoothTalk> talkList = new ArrayList<BluetoothTalk>();
                for (int j = 0; j < length; j++) {
                    JSONObject jsonObject = detailArray.getJSONObject(j);
                    int mode = Integer.parseInt(jsonObject.optString("mode".toUpperCase()));
                    if (mode != 3) {
                        final ParameterSettings item = new ParameterSettings();
                        item.setCode(jsonObject.optString("code".toUpperCase()));
                        item.setName(jsonObject.optString("name".toUpperCase()));
                        item.setProductId(String.valueOf(jsonObject.optInt("productId".toUpperCase())));
                        item.setDescription(jsonObject.optString("description".toUpperCase()));
                        item.setDescriptionType(ParameterSettings
                                .ParseDescriptionToType(item.getDescription()));
                        item.setChildId(jsonObject.optString("childId".toUpperCase()));
                        item.setScope(jsonObject.optString("scope".toUpperCase()));
                        item.setUserValue(jsonObject.optString("userValue".toUpperCase()));
                        item.setHexValueString(jsonObject.optString("hexValue".toUpperCase()));
                        item.setDefaultValue(String.valueOf(jsonObject.optInt("defaultValue".toUpperCase())));
                        item.setScale(String.valueOf(jsonObject.optDouble("scale".toUpperCase())));
                        item.setUnit(jsonObject.optString("unit".toUpperCase()));
                        item.setType(String.valueOf(jsonObject.optInt("type".toUpperCase())));
                        item.setMode(String.valueOf(jsonObject.optInt("mode".toUpperCase())));
                        parameterSettingsList.add(item);
                        BluetoothTalk talk = new BluetoothTalk() {
                            @Override
                            public void beforeSend() {
                                Log.v(TAG, "0106"
                                        + ParseSerialsUtils.getCalculatedCode(item)
                                        + item.getHexValueString()
                                        + "0001");
                                this.setSendBuffer(SerialUtility.crc16("0106"
                                        + ParseSerialsUtils.getCalculatedCode(item)
                                        + item.getHexValueString()
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
                                    byte[] received = SerialUtility.trimEnd(getReceivedBuffer());
                                    item.setReceived(received);
                                    return item;
                                }
                                return null;
                            }
                        };
                        talkList.add(talk);
                    }
                }
                communicationsList.add(talkList.toArray(new BluetoothTalk[talkList.size()]));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 开始写入参数
     *
     * @param position Communications List Index
     */
    private void startCommunication(int position) {
        if (position >= 0 && position < communicationsList.size()) {
            uploadParameterHandler.index = position;
            if (BluetoothTool.getInstance().isPrepared()) {
                BluetoothTool.getInstance()
                        .setHandler(uploadParameterHandler)
                        .setCommunications(communicationsList.get(position))
                        .send();
            }
        }
    }

    /**
     * 处理上传错误
     *
     * @param errorList     Error List
     * @param noRespondList No Respond List
     */
    private void handleParameterUploadComplete(List<ParameterSettings> errorList,
                                               List<ParameterSettings> noRespondList) {
        uploadDialog.setTitle(R.string.uploading_profile_end);
        int errorListSize = errorList.size();
        int noRespondListSize = noRespondList.size();
        if (errorListSize == 0 && noRespondListSize == 0) {
            uploadTipsTextView.setText(R.string.uploading_profile_complete);
        } else {
            // 错误列表
            String errorAndWarningMessage = "";
            for (ParameterSettings settings : errorList) {
                String errorString = settings.getCodeText()
                        + " "
                        + ApplicationConfig.ERROR_NAME_ARRAY[settings.getWriteErrorCode()];
                errorAndWarningMessage += errorString + "\n";
            }
            // 无返回列表
            for (ParameterSettings settings : noRespondList) {
                String warningString = settings.getCodeText()
                        + " "
                        + TextLocalize.getInstance().getWriteFailedText();
                errorAndWarningMessage += warningString + "\n";
            }
            uploadTipsTextView.setText(errorAndWarningMessage);
            tipsView.setVisibility(View.VISIBLE);
        }
        dialogButton.setText(R.string.dialog_btn_ok);
        // 写入日志
        LogUtils.getInstance().write(ApplicationConfig.LogUploadProfile);
    }

    /**
     * 点击 ListView 项目菜单动作
     *
     * @param view  View
     * @param index Index
     */
    private void onClickListViewItemMenuWithIndex(View view, final int index) {
        final Profile profile = profileList.get(index);
        PopupMenu popupMenu = new PopupMenu(this, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.parameter_upload_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    // 查看配置文件
                    case R.id.action_view: {
                        Intent intent = new Intent(ParameterUploadActivity.this, ParameterViewerActivity.class);
                        intent.putExtra("profileName", profile.getFileName());
                        startActivity(intent);
                    }
                    break;
                    // 写入配置文件
                    case R.id.action_upload: {
                        if (BluetoothTool.getInstance().isPrepared()) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(ParameterUploadActivity.this,
                                    R.style.CustomDialogStyle)
                                    .setTitle(R.string.upload_dialog_title)
                                    .setMessage(R.string.upload_dialog_message)
                                    .setNegativeButton(R.string.dialog_btn_cancel, null)
                                    .setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            ParameterUploadActivity.this.hasGetElevatorStatus = false;
                                            // 读取电梯状态
                                            ParameterUploadActivity.this.getElevatorStatus(index);
                                        }
                                    });
                            builder.create().show();
                        }
                    }
                    break;
                    // 删除配置文件
                    case R.id.action_delete: {
                        deleteProfile(profile.getFileName(), index);
                    }
                    break;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    /**
     * 删除配置文件
     *
     * @param fileName 配置文件名称
     */
    private void deleteProfile(final String fileName, final int index) {
        AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this, R.style.CustomDialogStyle)
                .setTitle(R.string.upload_dialog_title)
                .setMessage(R.string.upload_dialog_message)
                .setNegativeButton(R.string.dialog_btn_cancel, null)
                .setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                            Profile profile = profileList.get(index);
                            ProfileDao.deleteItem(ParameterUploadActivity.this, profile);
                            profileList.remove(profile);
                            profileAdapter.notifyDataSetChanged();
                        }
                    }
                });
        builder.create().show();
    }

    // ========================= Local Profile Adapter =====================================

    /**
     * 本地配置文件Adapter
     */
    private class LocalProfileAdapter extends BaseAdapter {

        public LocalProfileAdapter() {

        }

        @Override
        public int getCount() {
            return profileList.size();
        }

        @Override
        public Profile getItem(int i) {
            return profileList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            ViewHolder holder;
            LayoutInflater mInflater = LayoutInflater.from(ParameterUploadActivity.this);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.parameter_upload_item, null);
                holder = new ViewHolder();
                holder.profileName = (TextView) convertView.findViewById(R.id.profile_name);
                holder.createDate = (TextView) convertView.findViewById(R.id.create_date);
                holder.vendorNameView = convertView.findViewById(R.id.vendor_name_view);
                holder.vendorName = (TextView) convertView.findViewById(R.id.vendor_name);
                holder.deviceType = (TextView) convertView.findViewById(R.id.device_type);
                holder.operationButton = (ImageButton) convertView.findViewById(R.id.more_option);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final Profile profile = getItem(position);
            holder.profileName.setText(profile.getFileName());
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timeString = dateFormat.format(new Date(Long.parseLong(profile.getCreateTime())));
            holder.createDate.setText(timeString);
            holder.deviceType.setText(profile.getDeviceType());
            if (profile.getVendorName().length() == 0 || profile.getVendorName() == null) {
                holder.vendorName.setText("");
                holder.vendorNameView.setVisibility(View.GONE);
            } else {
                holder.vendorName.setText(profile.getVendorName());
                holder.vendorNameView.setVisibility(View.VISIBLE);
            }
            final int index = position;
            holder.operationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ParameterUploadActivity.this.onClickListViewItemMenuWithIndex(view, index);
                }
            });
            return convertView;
        }

        /**
         * View Holder
         */
        private class ViewHolder {
            TextView profileName;
            TextView createDate;
            View vendorNameView;
            TextView vendorName;
            TextView deviceType;
            ImageButton operationButton;
        }

    }

    // ====================================== Upload Parameter Handler ========================================= //

    /**
     * 上传参数 Handler
     */
    private class UploadParameterHandler extends BluetoothHandler {

        public int index = 0;

        public int receiveCount = 0;

        public UploadParameterHandler(Activity activity) {
            super(activity);
            TAG = UploadParameterHandler.class.getSimpleName();
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            if (index == ParameterUploadActivity.this.communicationsList.size() - 1) {
                List<ParameterSettings> noRespondList = new ArrayList<ParameterSettings>();
                List<ParameterSettings> errorList = new ArrayList<ParameterSettings>();
                for (ParameterSettings settings : ParameterUploadActivity.this.parameterSettingsList) {
                    // 未写入
                    if (settings.getReceived() == null) {
                        noRespondList.add(settings);
                    }
                    // 写入出错
                    if (settings.getWriteErrorCode() != -1) {
                        errorList.add(settings);
                    }
                }
                ParameterUploadActivity.this.currentProgress.setText("100%");
                ParameterUploadActivity.this.progressView.setVisibility(View.GONE);
                ParameterUploadActivity.this.handleParameterUploadComplete(errorList, noRespondList);
            }
            index++;
            if (index < ParameterUploadActivity.this.communicationsList.size()) {
                ParameterUploadActivity.this.startCommunication(index);
            }
        }

        @Override
        public void onTalkReceive(Message msg) {
            if (msg.obj != null && msg.obj instanceof ParameterSettings) {
                receiveCount++;
                ParameterSettings settings = (ParameterSettings) msg.obj;
                String hexString = SerialUtility.byte2HexStr(settings.getReceived());
                int index = 0;
                for (String errorCode : ApplicationConfig.ERROR_CODE_ARRAY) {
                    if (hexString.contains(errorCode)) {
                        settings.setWriteErrorCode(index);
                    }
                    index++;
                }
                ParameterUploadActivity.this.uploadProgressBar.setProgress(receiveCount);
                int maxProgress = ParameterUploadActivity.this.uploadProgressBar.getMax();
                int percentage = 100 * receiveCount / maxProgress;
                ParameterUploadActivity.this.currentProgress.setText(percentage + "%");
            }
        }

        @Override
        public void onBluetoothConnectException(Message message) {
            super.onBluetoothConnectException(message);
            CustomDialog.showBluetoothExceptionDialog(ParameterUploadActivity.this,
                    new CustomDialog.OnRetryListener() {
                        @Override
                        public void onClick() {
                            BluetoothTool.getInstance().currentState = BluetoothState.CONNECTED;
                            if (uploadDialog != null && uploadDialog.isShowing()) {
                                uploadDialog.dismiss();
                                uploadDialog = null;
                            }
                            // 重新写入参数配置
                            ParameterUploadActivity.this.hasGetElevatorStatus = false;
                            // 读取电梯状态
                            ParameterUploadActivity.this.getElevatorStatus(index);
                        }
                    });
        }
    }

    /**
     * 读取电梯运行状态 Handler
     */
    private class ElevatorStatusHandler extends BluetoothHandler {

        public int index;

        private RealTimeMonitor monitor;

        public ElevatorStatusHandler(Activity activity) {
            super(activity);
        }

        @Override
        public void onMultiTalkBegin(Message msg) {
            super.onMultiTalkBegin(msg);
            monitor = null;
        }

        @Override
        public void onTalkReceive(Message msg) {
            super.onTalkReceive(msg);
            if (msg.obj != null && msg.obj instanceof RealTimeMonitor) {
                monitor = (RealTimeMonitor) msg.obj;
                ParameterUploadActivity.this.hasGetElevatorStatus = true;
                int status = ParseSerialsUtils.getElevatorStatus(monitor);
                // 读取到电梯运行状态
                ParameterUploadActivity.this.onGetElevatorStatus(index, status);
            }
        }

        @Override
        public void onMultiTalkEnd(Message msg) {
            super.onMultiTalkEnd(msg);
            if (monitor == null) {
                ParameterUploadActivity.this.getElevatorStatus(index);
            }
        }
    }

}