package com.inovance.ElevatorControl.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import butterknife.InjectView;
import butterknife.Views;
import com.bluetoothtool.BluetoothTool;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.utils.ProfileDownloadUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-4.
 * Time: 10:29.
 */

public class RemoteHelpActivity extends Activity {

    private static final String TAG = RemoteHelpActivity.class.getSimpleName();

    private static final int PICK_CONTACT = 1;

    private static final int REQUEST_IMAGE_CAPTURE = 2;

    private static final int REQUEST_VIDEO_CAPTURE = 3;

    private static final int REQUEST_AUDIO_CAPTURE = 4;

    @InjectView(R.id.pick_contact)
    ImageButton pickContactButton;

    @InjectView(R.id.phone_number)
    EditText phoneNumberEditText;

    @InjectView(R.id.send_file)
    ImageButton sendButton;

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
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                startActivityForResult(intent, PICK_CONTACT);
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
                            case 0:
                                sendSceneProfile();
                                break;
                            case 1:
                                sendScenePicture();
                                break;
                            case 2:
                                sendSceneVideo();
                                break;
                            case 3:
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
        if (BluetoothTool.getInstance(this).isPrepared()) {
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
        // 选择联系人
        if (requestCode == PICK_CONTACT && resultCode == RESULT_OK) {
            Uri contactData = data.getData();
            Cursor contactCursor = getContentResolver().query(contactData,
                    new String[]{ContactsContract.Contacts._ID}, null, null,
                    null);
            String id = null;
            if (contactCursor.moveToFirst()) {
                id = contactCursor.getString(contactCursor
                        .getColumnIndex(ContactsContract.Contacts._ID));
            }
            contactCursor.close();
            String phoneNumber = null;
            Cursor phoneCursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "= ? ",
                    new String[]{id}, null);
            if (phoneCursor.moveToFirst()) {
                phoneNumber = phoneCursor
                        .getString(phoneCursor
                                .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            }
            if (phoneNumber != null) {
                phoneNumberEditText.setText(phoneNumber.replace(" ", "").replace("-", "").trim());
            }
            phoneCursor.close();
        }
        // 拍照
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] sendData = stream.toByteArray();
            // TODO
        }
        // 录制视频
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            Uri videoUri = data.getData();
            InputStream iStream = null;
            try {
                iStream = getContentResolver().openInputStream(videoUri);
                byte[] sendData = getBytes(iStream);
                // TODO
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 录音
        if (requestCode == REQUEST_AUDIO_CAPTURE && resultCode == RESULT_OK) {
            Uri audioUri = data.getData();
            InputStream iStream = null;
            try {
                iStream = getContentResolver().openInputStream(audioUri);
                byte[] sendData = getBytes(iStream);
                // TODO
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

}