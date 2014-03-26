package com.kio.ElevatorControl.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import butterknife.InjectView;
import butterknife.Views;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HHandler;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.config.ApplicationConfig;
import com.kio.ElevatorControl.models.ParameterSettings;
import com.kio.ElevatorControl.models.Profile;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.widget.Button;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.ProgressBar;
import org.holoeverywhere.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-10.
 * Time: 11:35.
 */
public class ParameterUploadActivity extends Activity {

    private final static String DIRECTORY_NAME = "Profile";

    private static final String TAG = ParameterUploadActivity.class.getSimpleName();

    @InjectView(R.id.upload_list)
    ListView listView;

    private LocalProfileAdapter adapter;

    private List<HCommunication[]> communicationsList;

    private List<ParameterSettings> parameterSettingsList;

    private UploadParameterHandler uploadParameterHandler;

    private List<Profile> profileList;

    private AlertDialog uploadDialog;

    private TextView uploadTipsTextView;

    private ProgressBar uploadProgressBar;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.parameter_upload_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_parameter_upload);
        Views.inject(this);
        profileList = new ArrayList<Profile>();
        getProfileList();
        uploadParameterHandler = new UploadParameterHandler(this);
        adapter = new LocalProfileAdapter(profileList);
        listView.setAdapter(adapter);
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
     * Get Profile List
     */
    private void getProfileList() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File directory = new File(getApplicationContext().getExternalCacheDir().getPath()
                    + "/"
                    + DIRECTORY_NAME);
            if (!directory.exists()) {
                directory.mkdir();
            }
            File[] files = directory.listFiles();
            for (File inFile : files) {
                if (inFile.isFile()) {
                    Profile profile = new Profile();
                    profile.setVersion("1.2");
                    profile.setUpdateDate("2014-5-12");
                    profile.setFileName(inFile.getName());
                    profileList.add(profile);
                }
            }
        }
    }

    /**
     * 上传参数
     *
     * @param position ListView index
     */
    private void onUploadButtonClick(int position) {
        Profile profile = adapter.getItem(position);
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
                ParameterUploadActivity.this.startCommunication(0);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(ParameterUploadActivity.this);
        LayoutInflater inflater = ParameterUploadActivity.this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.parameters_upload_dialog, null);
        uploadTipsTextView = (TextView) dialogView.findViewById(R.id.upload_tips);
        uploadProgressBar = (ProgressBar) dialogView.findViewById(R.id.progress_bar);
        uploadProgressBar.setMax(parameterSettingsList.size());
        builder.setTitle(R.string.uploading_profile_text);
        builder.setView(dialogView);
        builder.setNegativeButton(R.string.dialog_btn_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        uploadDialog = builder.create();
        uploadDialog.setCancelable(false);
        uploadDialog.setCanceledOnTouchOutside(false);
        uploadDialog.show();
    }

    /**
     * Generate Upload Communications List
     *
     * @param groupArray JSON Array
     */
    private void generateCommunicationsList(JSONArray groupArray) {
        communicationsList = new ArrayList<HCommunication[]>();
        parameterSettingsList = new ArrayList<ParameterSettings>();
        int size = groupArray.length();
        for (int i = 0; i < size; i++) {
            try {
                JSONObject groupsJSONObject = groupArray.getJSONObject(i);
                JSONArray detailArray = groupsJSONObject.getJSONArray("parameterSettings");
                int length = detailArray.length();
                HCommunication[] communications = new HCommunication[length];
                for (int j = 0; j < length; j++) {
                    JSONObject jsonObject = detailArray.getJSONObject(j);
                    final ParameterSettings item = new ParameterSettings();
                    item.setCode(jsonObject.optString("code"));
                    item.setName(jsonObject.optString("name"));
                    item.setProductId(String.valueOf(jsonObject.optInt("productId")));
                    item.setDescription(jsonObject.optString("description"));
                    item.setDescriptiontype(ParameterSettings
                            .ParseDescriptionToType(item.getDescription()));
                    item.setChildId(jsonObject.optString("childId"));
                    item.setScope(jsonObject.optString("scope"));
                    item.setUserValue(jsonObject.optString("userValue"));
                    item.setHexValueString(jsonObject.optString("hexValue"));
                    item.setDefaultValue(String.valueOf(jsonObject.optInt("defaultValue")));
                    item.setScale(String.valueOf(jsonObject.optDouble("scale")));
                    item.setUnit(jsonObject.optString("unit"));
                    item.setType(String.valueOf(jsonObject.optInt("type")));
                    item.setMode(String.valueOf(jsonObject.optInt("mode")));
                    parameterSettingsList.add(item);
                    communications[j] = new HCommunication() {
                        @Override
                        public void beforeSend() {
                            this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("0106"
                                    + item.getCode()
                                    + item.getHexValueString()
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
                communicationsList.add(communications);
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
            if (HBluetooth.getInstance(this).isPrepared()) {
                HBluetooth.getInstance(this)
                        .setHandler(uploadParameterHandler)
                        .setCommunications(communicationsList.get(position))
                        .Start();
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
                String errorString = settings.getCode()
                        + "参数"
                        + ApplicationConfig.ERROR_NAME_ARRAY[settings.getWriteErrorCode()];
                errorAndWarningMessage += errorString + "\n";
            }
            // 无返回列表
            for (ParameterSettings settings : noRespondList) {
                String warningString = settings.getCode()
                        + "参数"
                        + ApplicationConfig.NO_RESPOND;
                errorAndWarningMessage += warningString + "\n";
            }
            uploadTipsTextView.setText(errorAndWarningMessage);
        }
        uploadDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "结束", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        uploadDialog.setButton(AlertDialog.BUTTON_POSITIVE, "继续上传", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
    }

    // ========================= Local Profile Adapter =====================================

    /**
     * 本地配置文件Adapter
     */
    private class LocalProfileAdapter extends BaseAdapter {

        private List<Profile> profileLists;

        public LocalProfileAdapter(List<Profile> lists) {
            profileLists = lists;
        }

        @Override
        public int getCount() {
            return profileLists.size();
        }

        @Override
        public Profile getItem(int i) {
            return profileLists.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            ViewHolder holder = null;
            LayoutInflater mInflater = LayoutInflater.from(ParameterUploadActivity.this);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.parameter_upload_item, null);
                holder = new ViewHolder();
                holder.profileVersion = (TextView) convertView.findViewById(R.id.profile_version);
                holder.profileUpdateDate = (TextView) convertView.findViewById(R.id.profile_update_date);
                holder.uploadButton = (Button) convertView.findViewById(R.id.upload_button);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Profile profile = getItem(position);
            holder.profileVersion.setText(profile.getVersion());
            holder.profileUpdateDate.setText(profile.getUpdateDate());
            final int index = position;
            holder.uploadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 上传对应的参数
                    ParameterUploadActivity.this.onUploadButtonClick(index);
                }
            });
            return convertView;
        }

        /**
         * View Holder
         */
        private class ViewHolder {
            TextView profileVersion;
            TextView profileUpdateDate;
            Button uploadButton;
        }

    }

    // ====================================== Upload Parameter Handler =========================================

    /**
     * 上传参数 Handler
     */
    private class UploadParameterHandler extends HHandler {

        public int index = 0;

        private int receiveCount = 0;

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
                String hexString = HSerial.byte2HexStr(settings.getReceived());
                int index = 0;
                for (String errorCode : ApplicationConfig.ERROR_CODE_ARRAY) {
                    if (hexString.contains(errorCode)) {
                        settings.setWriteErrorCode(index);
                    }
                    index++;
                }
                ParameterUploadActivity.this.uploadProgressBar.setProgress(receiveCount);
            }
        }

    }

}