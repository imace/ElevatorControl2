package com.inovance.elevatorcontrol.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.*;
import butterknife.InjectView;
import butterknife.Views;
import com.inovance.bluetoothtool.BluetoothTool;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.adapters.ChatMessageAdapter;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.config.ParameterUpdateTool;
import com.inovance.elevatorcontrol.daos.ChatMessageDao;
import com.inovance.elevatorcontrol.models.ChatMessage;
import com.inovance.elevatorcontrol.models.User;
import com.inovance.elevatorcontrol.utils.FileTransport;
import com.inovance.elevatorcontrol.utils.FileTransport.OnFileDownloadComplete;
import com.inovance.elevatorcontrol.utils.FileTransport.OnFileUploadComplete;
import com.inovance.elevatorcontrol.utils.ProfileDownloadUtils;
import com.inovance.elevatorcontrol.web.WebApi;
import com.inovance.elevatorcontrol.web.WebApi.OnGetResultListener;
import com.inovance.elevatorcontrol.web.WebApi.OnRequestFailureListener;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-4.
 * Time: 10:29.
 */

public class RemoteHelpActivity extends Activity implements OnGetResultListener,
        OnRequestFailureListener, OnFileUploadComplete, OnFileDownloadComplete, Runnable {

    private static final String TAG = RemoteHelpActivity.class.getSimpleName();

    private static final String LastTimestampTag = "LastTimestamp";

    private static final int REQUEST_IMAGE_CAPTURE = 2;

    private static final int REQUEST_VIDEO_CAPTURE = 3;

    private static final int REQUEST_AUDIO_CAPTURE = 4;

    private SharedPreferences sharedPreferences;

    private ChatMessageAdapter chatMessageAdapter;

    @InjectView(R.id.pick_contact)
    ImageButton pickContactButton;

    @InjectView(R.id.phone_number)
    EditText phoneNumberEditText;

    @InjectView(R.id.send_file)
    ImageButton sendButton;

    @InjectView(R.id.chat_list_view)
    ListView chatListView;

    private Runnable syncTask;

    private boolean running = false;

    private static final int SYNC_TIME = 3000;

    private ChatMessage currentSelectMessage;

    private long lastReadTimestamp;

    private InputStream tempInputStream;

    private Handler syncHandler = new Handler();

    private ExecutorService pool = Executors.newSingleThreadExecutor();

    private Handler refreshHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            chatMessageAdapter.updateChatMessageList(ChatMessageDao.findAll(RemoteHelpActivity.this));
        }

    };

    /**
     * 所有已注册用户列表
     */
    private List<User> registUserList = new ArrayList<User>();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setTitle(R.string.remote_help_text);
        setContentView(R.layout.activity_remote_help);
        Views.inject(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        sharedPreferences = getSharedPreferences(ApplicationConfig.PREFERENCE_FILE_NAME, 0);
        pickContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<String> items = new ArrayList<String>();
                for (User user : registUserList) {
                    items.add(user.getName() + " - " + user.getCellPhone());
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(RemoteHelpActivity.this);
                AlertDialog dialog = builder.setTitle(R.string.select_receiver_title)
                        .setItems(items.toArray(new String[items.size()]),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int position) {
                                        User user = registUserList.get(position);
                                        phoneNumberEditText.setText(user.getCellPhone());
                                    }
                                }).create();
                dialog.show();
            }
        });
        phoneNumberEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                sendButton.setEnabled(charSequence.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        sendButton.setEnabled(false);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(phoneNumberEditText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                selectContentToSend();
            }
        });
        chatMessageAdapter = new ChatMessageAdapter(this, ChatMessageDao.findAll(this));
        chatMessageAdapter.setOnMessageItemClickListener(new ChatMessageAdapter.OnMessageItemClickListener() {
            @Override
            public void onClick(View view, int position, ChatMessage message) {
                switch (message.getChatType()) {
                    case ChatMessage.SEND:
                        // Open local saved file
                        openLocalSendFile(message);
                        break;
                    case ChatMessage.RECEIVE:
                        selectChatContentOperation(message);
                        break;
                }
            }
        });
        chatListView.setAdapter(chatMessageAdapter);
        syncTask = new Runnable() {
            @Override
            public void run() {
                if (running) {
                    pool.execute(RemoteHelpActivity.this);
                    syncHandler.postDelayed(this, SYNC_TIME);
                }
            }
        };
        lastReadTimestamp = sharedPreferences.getLong(LastTimestampTag, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        WebApi.getInstance().setOnResultListener(this);
        WebApi.getInstance().setOnFailureListener(this);
        WebApi.getInstance().getRegistUserList(this);
        FileTransport.getInstance().setOnFileDownloadComplete(this);
        FileTransport.getInstance().setOnFileUploadComplete(this);
        running = true;
        syncHandler.postDelayed(syncTask, SYNC_TIME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        WebApi.getInstance().removeListener();
        FileTransport.getInstance().removeListener();
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(LastTimestampTag, lastReadTimestamp);
        editor.commit();
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
     * 选择操作类型
     *
     * @param message Message
     */
    private void selectChatContentOperation(final ChatMessage message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.chat_content_operation_title)
                .setItems(R.array.chat_content_operation_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int position) {
                        switch (position) {
                            case 0:
                                if (!openLocalCachedFile(message)) {
                                    currentSelectMessage = message;
                                    FileTransport.getInstance().downloadFile(RemoteHelpActivity.this,
                                            ApplicationConfig.APIUri
                                                    + ApplicationConfig.GetChatMessageFile
                                                    + message.getRemoteID());
                                }
                                break;
                            case 1:
                                phoneNumberEditText.setText(message.getFromNumber());
                                selectContentToSend();
                                break;
                        }
                    }
                }).setNegativeButton(R.string.dialog_btn_cancel, null);
        builder.create().show();
    }

    /**
     * 打开本地缓存的文件
     *
     * @param message ChatMessage
     * @return boolean
     */
    private boolean openLocalCachedFile(ChatMessage message) {
        File filePath;
        switch (message.getChatType()) {
            case ChatMessage.RECEIVE:
                filePath = new File(getFilesDir().getPath()
                        + "/" + ApplicationConfig.ReceiveFileFolder + "/"
                        + message.getLocalFileName());
                return openFileWithDefaultIntent(filePath);
        }
        return false;
    }

    /**
     * Open local saved file
     *
     * @param message Message
     */
    private void openLocalSendFile(ChatMessage message) {
        String fileName = message.getFromNumber()
                + "-" + message.getToNumber()
                + "-" + message.getContentType()
                + "-" + message.getTitle();
        File directory = new File(getApplicationContext().getFilesDir().getPath()
                + "/"
                + ApplicationConfig.SentFolder);
        if (directory.exists()) {
            File[] files = directory.listFiles();
            for (File file : files) {
                int index = file.getName().lastIndexOf('.');
                String name = file.getName().substring(0, index);
                if (name.equalsIgnoreCase(fileName)) {
                    openFileWithDefaultIntent(file);
                }
            }
        }
    }

    /**
     * 选择操作类型
     */
    private void selectContentToSend() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.choice_send_type_title)
                .setItems(R.array.send_type_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int position) {
                        switch (position) {
                            case ChatMessage.TYPE_TEXT:
                                showSendTextContentDialog(ChatMessage.TYPE_TEXT);
                                break;
                            case ChatMessage.TYPE_PROFILE:
                                sendSceneProfile();
                                break;
                            case ChatMessage.TYPE_PICTURE:
                                sendScenePicture();
                                break;
                            case ChatMessage.TYPE_VIDEO:
                                sendSceneVideo();
                                break;
                            case ChatMessage.TYPE_AUDIO:
                                sendSceneAudio();
                                break;
                        }
                    }
                }).setNegativeButton(R.string.dialog_btn_cancel, null);
        builder.create().show();
    }

    /**
     * 发送现场参数
     */
    private void sendSceneProfile() {
        if (BluetoothTool.getInstance().isPrepared()) {
            ProfileDownloadUtils downloadUtils = new ProfileDownloadUtils(this);
            downloadUtils.startDownloadProfile(new ProfileDownloadUtils.OnDownloadCompleteListener() {
                @Override
                public void onComplete(String JSONString) {
                    try {
                        InputStream inputStream = IOUtils.toInputStream(JSONString, "UTF-8");
                        showSendChatContentDialog(ChatMessage.TYPE_PROFILE, "json", inputStream);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            Toast.makeText(this, R.string.no_connected_device, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 发送现场图片
     */
    private void sendScenePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    /**
     * 发送现场视频
     */
    private void sendSceneVideo() {
        try {
            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            startActivityForResult(intent, REQUEST_VIDEO_CAPTURE);
        } catch (Exception e) {
            Toast.makeText(this, R.string.not_found_video_program, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 发送现场音频
     */
    private void sendSceneAudio() {
        try {
            Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
            startActivityForResult(intent, REQUEST_AUDIO_CAPTURE);
        } catch (Exception e) {
            Toast.makeText(this, R.string.not_found_record_program, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 拍照
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            InputStream inputStream = new ByteArrayInputStream(stream.toByteArray());
            // 上传图片
            showSendChatContentDialog(ChatMessage.TYPE_PICTURE, "jpg", inputStream);
        }
        // 录制视频
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            Uri videoUri = data.getData();
            ContentResolver cR = getApplicationContext().getContentResolver();
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            String type = mime.getExtensionFromMimeType(cR.getType(videoUri));
            try {
                InputStream inputStream = getContentResolver().openInputStream(videoUri);
                // 上传视频
                showSendChatContentDialog(ChatMessage.TYPE_VIDEO, type, inputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        // 录音
        if (requestCode == REQUEST_AUDIO_CAPTURE && resultCode == RESULT_OK) {
            Uri audioUri = data.getData();
            ContentResolver cR = getApplicationContext().getContentResolver();
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            String type = mime.getExtensionFromMimeType(cR.getType(audioUri));
            try {
                InputStream inputStream = getContentResolver().openInputStream(audioUri);
                // 上传音频
                showSendChatContentDialog(ChatMessage.TYPE_AUDIO, type, inputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 显示发送文本内容对话框
     *
     * @param type 类型
     */
    private void showSendTextContentDialog(final int type) {
        View dialogView = getLayoutInflater().inflate(R.layout.send_text_cotnent_dialog, null);
        final EditText titleInput = (EditText) dialogView.findViewById(R.id.message_title);
        final EditText messageInput = (EditText) dialogView.findViewById(R.id.message_content);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.input_text_content_title)
                .setView(dialogView)
                .setPositiveButton(R.string.dialog_btn_ok, null)
                .setNegativeButton(R.string.dialog_btn_cancel, null);
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        final Button cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        final Button confirmButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        confirmButton.setEnabled(false);
        titleInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (charSequence.length() > 0 && messageInput.getText().toString().length() > 0) {
                    confirmButton.setEnabled(true);
                } else {
                    confirmButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (charSequence.length() > 0 && titleInput.getText().toString().length() > 0) {
                    confirmButton.setEnabled(true);
                } else {
                    confirmButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    InputStream inputStream = IOUtils.toInputStream(messageInput.getText().toString().trim(), "UTF-8");
                    InputStream uploadStream = copyStream(inputStream);
                    FileTransport.getInstance().uploadFile(RemoteHelpActivity.this,
                            getPhoneNumber(),
                            phoneNumberEditText.getText().toString().trim(),
                            type,
                            titleInput.getText().toString(),
                            "txt",
                            titleInput.getText().toString(),
                            uploadStream);
                    dialog.dismiss();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 显示发送远程协助内容对话框
     *
     * @param type        类型
     * @param extension   扩展名
     * @param inputStream InputStream
     */
    private void showSendChatContentDialog(final int type, final String extension, final InputStream inputStream) {
        View dialogView = getLayoutInflater().inflate(R.layout.send_caht_dialog, null);
        final EditText fileNameInput = (EditText) dialogView.findViewById(R.id.message_title);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.input_message_title_text_title_text)
                .setView(dialogView)
                .setPositiveButton(R.string.dialog_btn_ok, null)
                .setNegativeButton(R.string.dialog_btn_cancel, null);
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        final Button cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        final Button confirmButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        confirmButton.setEnabled(false);

        fileNameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (charSequence.length() > 0) {
                    confirmButton.setEnabled(true);
                } else {
                    confirmButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(fileNameInput.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                InputStream uploadStream = copyStream(inputStream);
                FileTransport.getInstance().uploadFile(RemoteHelpActivity.this,
                        getPhoneNumber(),
                        phoneNumberEditText.getText().toString().trim(),
                        type,
                        fileNameInput.getText().toString(),
                        extension,
                        fileNameInput.getText().toString(),
                        uploadStream);
                dialog.dismiss();
            }
        });
    }

    private String getPhoneNumber() {
        User user = ParameterUpdateTool.getInstance().getCurrentUser();
        if (user != null) {
            return user.getCellPhone();
        }
        return "";
    }

    @Override
    public void onResult(String tag, String responseString) {
        // 所有的消息列表
        if (tag.equalsIgnoreCase(ApplicationConfig.GetChatMessage)) {
            if (responseString != null && responseString.length() > 0) {
                try {
                    JSONArray jsonArray = new JSONArray(responseString);
                    int size = jsonArray.length();
                    if (size > 0) {
                        long lastTime = 0;
                        for (int i = 0; i < size; i++) {
                            JSONObject object = jsonArray.getJSONObject(i);
                            ChatMessage message = new ChatMessage(object);
                            ChatMessageDao.save(RemoteHelpActivity.this, message);
                            lastTime = Long.parseLong(message.getTimeString());
                        }
                        // 保存最近读取的截止时间戳
                        lastReadTimestamp = lastTime;
                        refreshHandler.sendEmptyMessage(0);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if (tag.equalsIgnoreCase(ApplicationConfig.GetRegistUserList)) {
            if (responseString != null && responseString.length() > 0) {
                try {
                    JSONArray jsonArray = new JSONArray(responseString);
                    registUserList.clear();
                    int size = jsonArray.length();
                    for (int i = 0; i < size; i++) {
                        JSONObject object = jsonArray.getJSONObject(i);
                        User user = new User();
                        user.setName(object.optString("UserName".toUpperCase()));
                        user.setCellPhone(object.optString("MobilePhone".toUpperCase()));
                        registUserList.add(user);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onFailure(int statusCode, Throwable throwable) {
        Toast.makeText(this, R.string.server_error_text, Toast.LENGTH_SHORT).show();
    }

    private InputStream copyStream(InputStream inputStream) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) > -1) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
            byteArrayOutputStream.flush();
            tempInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 下载完毕
     *
     * @param filePath File path
     */
    @Override
    public void onDownloadComplete(File file, String fileName, String contentType) {
        if (currentSelectMessage != null) {
            currentSelectMessage.setLocalFileName(fileName);
            ChatMessageDao.update(this, currentSelectMessage);
        }
        openFileWithDefaultIntent(file);
    }

    /**
     * 打开文件
     *
     * @param file File
     * @return boolean
     */
    private boolean openFileWithDefaultIntent(File file) {
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        int index = file.getName().lastIndexOf('.') + 1;
        String extension = file.getName().substring(index).toLowerCase();
        String type = mime.getMimeTypeFromExtension(extension);
        if (extension.contains("json")) {
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
                Intent intent = new Intent(RemoteHelpActivity.this, ParameterViewerActivity.class);
                intent.putExtra("profileName", "Remote profile");
                intent.putExtra("profileContent", stringBuilder.toString());
                startActivity(intent);
                return true;
            } catch (FileNotFoundException e) {
                return false;
            } catch (IOException e) {
                return false;
            }
        } else {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), type);
                startActivity(intent);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    @Override
    public void onUploadComplete(String fileName) {
        File directory = new File(getApplicationContext().getFilesDir().getPath()
                + "/"
                + ApplicationConfig.SentFolder);
        if (!directory.exists()) {
            directory.mkdir();
        }
        File filePath = new File(getApplicationContext().getFilesDir().getPath()
                + "/"
                + ApplicationConfig.SentFolder
                + "/"
                + fileName);
        if (!filePath.exists()) {
            try {
                filePath.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileOutputStream outputStream = new FileOutputStream(filePath);
            if (tempInputStream != null) {
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                int length = -1;
                while ((length = tempInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }
                if (tempInputStream != null) {
                    tempInputStream.close();
                    tempInputStream = null;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        WebApi.getInstance().getChatMessage(this, getPhoneNumber(), lastReadTimestamp);
    }
}