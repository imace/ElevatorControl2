package com.inovance.ElevatorControl.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
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
import com.bluetoothtool.BluetoothTool;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.adapters.ChatMessageAdapter;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.models.ChatMessage;
import com.inovance.ElevatorControl.models.ConfigFactory;
import com.inovance.ElevatorControl.models.User;
import com.inovance.ElevatorControl.utils.FileTransport;
import com.inovance.ElevatorControl.utils.FileTransport.OnFileDownloadComplete;
import com.inovance.ElevatorControl.utils.FileTransport.OnFileUploadComplete;
import com.inovance.ElevatorControl.utils.ProfileDownloadUtils;
import com.inovance.ElevatorControl.web.WebApi;
import com.inovance.ElevatorControl.web.WebApi.OnGetResultListener;
import com.inovance.ElevatorControl.web.WebApi.OnRequestFailureListener;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-4.
 * Time: 10:29.
 */

public class RemoteHelpActivity extends Activity implements OnGetResultListener,
        OnRequestFailureListener, OnFileUploadComplete, OnFileDownloadComplete {

    private static final String TAG = RemoteHelpActivity.class.getSimpleName();

    private static final int REQUEST_IMAGE_CAPTURE = 2;

    private static final int REQUEST_VIDEO_CAPTURE = 3;

    private static final int REQUEST_AUDIO_CAPTURE = 4;

    private List<ChatMessage> chatMessageList = new ArrayList<ChatMessage>();

    private ChatMessageAdapter chatMessageAdapter;

    @InjectView(R.id.pick_contact)
    ImageButton pickContactButton;

    @InjectView(R.id.phone_number)
    EditText phoneNumberEditText;

    @InjectView(R.id.send_file)
    ImageButton sendButton;

    @InjectView(R.id.chat_list_view)
    ListView chatListView;

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
        pickContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<String> items = new ArrayList<String>();
                String template = getString(R.string.contact_item_template);
                for (User user : registUserList) {
                    items.add(template.replace("{param0}", user.getName()).replace("{param1}", user.getCellPhone()));
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
        chatMessageAdapter = new ChatMessageAdapter(this);
        chatMessageAdapter.setOnMessageItemClickListener(new ChatMessageAdapter.OnMessageItemClickListener() {
            @Override
            public void onClick(View view, int position, ChatMessage message) {
                if (message.getChatType() == ChatMessage.RECEIVE) {
                    selectChatContentOperation(message);
                }
            }
        });
        chatListView.setAdapter(chatMessageAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        chatMessageList.clear();
        WebApi.getInstance().setOnResultListener(this);
        WebApi.getInstance().setOnFailureListener(this);
        WebApi.getInstance().getSendChatMessage(this, getPhoneNumber());
        WebApi.getInstance().getReceiveChatMessage(this, getPhoneNumber());
        WebApi.getInstance().getRegistUserList(this);
        FileTransport.getInstance().setOnFileDownloadComplete(this);
        FileTransport.getInstance().setOnFileUploadComplete(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        WebApi.getInstance().removeListener();
        FileTransport.getInstance().removeListener();
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
                                FileTransport.getInstance().downloadFile(RemoteHelpActivity.this,
                                        ApplicationConfig.DomainName + ApplicationConfig.GetChatMessageFile + message.getId());
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
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(intent, REQUEST_VIDEO_CAPTURE);
    }

    /**
     * 发送现场音频
     */
    private void sendSceneAudio() {
        Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        startActivityForResult(intent, REQUEST_AUDIO_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 拍照
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
            InputStream inputStream = new ByteArrayInputStream(stream.toByteArray());
            // 上传图片
            showSendChatContentDialog(ChatMessage.TYPE_PICTURE, "jpeg", inputStream);
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
                    FileTransport.getInstance().uploadFile(RemoteHelpActivity.this,
                            getPhoneNumber(),
                            phoneNumberEditText.getText().toString().trim(),
                            type,
                            titleInput.getText().toString(),
                            "txt",
                            titleInput.getText().toString(),
                            inputStream);
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
                FileTransport.getInstance().uploadFile(RemoteHelpActivity.this,
                        getPhoneNumber(),
                        phoneNumberEditText.getText().toString().trim(),
                        type,
                        fileNameInput.getText().toString(),
                        extension,
                        fileNameInput.getText().toString(),
                        inputStream);
                dialog.dismiss();
            }
        });
    }

    private String getPhoneNumber() {
        User user = ConfigFactory.getInstance().getCurrentUser();
        if (user != null) {
            return user.getCellPhone();
        }
        return "";
    }

    /**
     * Get byte from inputStream
     *
     * @param inputStream inputStream
     * @return byte[]
     * @throws IOException
     */
    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    @Override
    public void onResult(String tag, String responseString) {
        // 发送的信息
        if (tag.equalsIgnoreCase(ApplicationConfig.GetSendChatMessage)) {
            if (responseString != null && responseString.length() > 0) {
                try {
                    JSONArray jsonArray = new JSONArray(responseString);
                    int size = jsonArray.length();
                    for (int i = 0; i < size; i++) {
                        JSONObject object = jsonArray.getJSONObject(i);
                        ChatMessage message = new ChatMessage(object);
                        message.setChatType(ChatMessage.SEND);
                        chatMessageList.add(message);
                        chatMessageAdapter.updateChatMessageList(chatMessageList);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        // 接收的消息
        if (tag.equalsIgnoreCase(ApplicationConfig.GetReceiveChatMessage)) {
            if (responseString != null && responseString.length() > 0) {
                try {
                    JSONArray jsonArray = new JSONArray(responseString);
                    int size = jsonArray.length();
                    for (int i = 0; i < size; i++) {
                        JSONObject object = jsonArray.getJSONObject(i);
                        ChatMessage message = new ChatMessage(object);
                        message.setChatType(ChatMessage.RECEIVE);
                        chatMessageList.add(message);
                        chatMessageAdapter.updateChatMessageList(chatMessageList);
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
                        user.setName(object.optString("UserName"));
                        user.setCellPhone(object.optString("MobilePhone"));
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
        Toast.makeText(this, throwable.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
    }

    /**
     * 下载完毕
     *
     * @param filePath File path
     */
    @Override
    public void onDownloadComplete(File file, String contentType) {
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        int index = file.getName().lastIndexOf('.') + 1;
        String extension = file.getName().substring(index).toLowerCase();
        String type = mime.getMimeTypeFromExtension(extension);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), type);
        startActivity(intent);
    }

    @Override
    public void onUploadComplete() {

    }
}