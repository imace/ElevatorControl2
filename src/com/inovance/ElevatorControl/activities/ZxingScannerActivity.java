package com.inovance.ElevatorControl.activities;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.zxing.Result;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.barcode.ZXingScannerView;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-7-30.
 * Time: 13:30.
 */
public class ZxingScannerActivity extends FragmentActivity {

    private final static String TAG = ZxingScannerActivity.class.getSimpleName();

    private TextView scanText;

    private Button scanButton;

    private Button saveButton;

    private ScannerFragment scannerFragment;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setContentView(R.layout.activity_zxing_scanner);

        scanText = (TextView) findViewById(R.id.scan_result_text);
        scanButton = (Button) findViewById(R.id.start_scan_button);
        saveButton = (Button) findViewById(R.id.save_picture_button);

        scannerFragment = new ScannerFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.cameraPreview, scannerFragment);
        fragmentTransaction.commit();

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanButton.setEnabled(false);
                saveButton.setEnabled(false);
                scanText.setText(R.string.scan_tips_text);
                scannerFragment.start();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scannerFragment.getBitmap() != null) {
                    saveButton.setEnabled(false);
                    File pictureFile = getOutputMediaFile();
                    if (pictureFile == null) {
                        return;
                    }
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(pictureFile);
                        fileOutputStream.write(scannerFragment.getBitmap());
                        fileOutputStream.close();
                        Toast.makeText(getApplicationContext(),
                                R.string.save_barcode_picture_successful_text,
                                Toast.LENGTH_SHORT)
                                .show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        scanButton.setEnabled(false);
        saveButton.setEnabled(false);
    }

    private static File getOutputMediaFile() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "/Picture");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs();
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public class ScannerFragment extends Fragment implements ZXingScannerView.ResultHandler {

        private ZXingScannerView mScannerView;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            mScannerView = new ZXingScannerView(getActivity());
            mScannerView.setAutoFocus(true);
            return mScannerView;
        }

        public void stop() {
            mScannerView.stopCamera();
        }

        public void start() {
            mScannerView.startCamera();
        }

        @Override
        public void onResume() {
            super.onResume();
            mScannerView.setResultHandler(this);
            mScannerView.startCamera();
        }

        public byte[] getBitmap() {
            return mScannerView.getBitmapData();
        }

        @Override
        public void handleResult(Result rawResult) {
            String prefix = getResources().getString(R.string.scan_result_text);
            scanText.setText(prefix + rawResult.getText());
            scanButton.setEnabled(true);
            saveButton.setEnabled(true);
            mScannerView.stopCamera();
        }

        @Override
        public void onPause() {
            super.onPause();
            mScannerView.stopCamera();
        }
    }
}