package com.inovance.ElevatorControl.activities;

/**
 * Created by IntelliJ IDEA.
 * 条形码扫描
 * User: keith.
 * Date: 14-3-4.
 * Time: 13:42.
 */

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.views.zbar.CameraPreview;
import net.sourceforge.zbar.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/* Import ZBar Class files */

public class BarcodeCaptureActivity extends Activity {

    private static final String TAG = BarcodeCaptureActivity.class.getSimpleName();

    private Camera mCamera;

    /**
     * 扫描窗口
     */
    private CameraPreview mPreview;

    private Handler autoFocusHandler;

    private TextView scanText;

    private Button scanButton;

    private Button saveButton;

    private ImageScanner scanner;

    private boolean barcodeScanned = false;

    private boolean previewing = true;

    private byte[] bitmapData;

    static {
        System.loadLibrary("iconv");
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setContentView(R.layout.activity_barcode_capture);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        autoFocusHandler = new Handler();
        mCamera = getCameraInstance();

        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
        preview.addView(mPreview);

        scanText = (TextView) findViewById(R.id.scan_result_text);
        scanButton = (Button) findViewById(R.id.start_scan_button);
        saveButton = (Button) findViewById(R.id.save_picture_button);

        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (barcodeScanned) {
                    scanButton.setEnabled(false);
                    saveButton.setEnabled(false);
                    barcodeScanned = false;
                    scanText.setText(R.string.scan_tips_text);
                    mCamera.setPreviewCallback(previewCb);
                    mCamera.startPreview();
                    previewing = true;
                    mCamera.autoFocus(autoFocusCB);
                }
            }
        });

        saveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bitmapData != null) {
                    saveButton.setEnabled(false);
                    File pictureFile = getOutputMediaFile();
                    if (pictureFile == null) {
                        return;
                    }
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(pictureFile);
                        fileOutputStream.write(bitmapData);
                        fileOutputStream.close();
                    } catch (Exception e) {
                        Log.e(TAG, "Save Error.");
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
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        releaseCamera();
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception ignored) {

        }
        return c;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing)
                mCamera.autoFocus(autoFocusCB);
        }
    };

    PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Size size = parameters.getPreviewSize();
            YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
            ByteArrayOutputStream jpgData = new ByteArrayOutputStream();
            yuvimage.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, jpgData);
            bitmapData = jpgData.toByteArray();
            Image barcode = new Image(size.width, size.height, "Y800");
            barcode.setData(data);
            int result = scanner.scanImage(barcode);
            if (result != 0) {
                previewing = false;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                SymbolSet symbols = scanner.getResults();
                for (Symbol sym : symbols) {
                    String prefix = getResources().getString(R.string.scan_result_text);
                    scanText.setText(prefix + sym.getData());
                    scanButton.setEnabled(true);
                    saveButton.setEnabled(true);
                    barcodeScanned = true;
                }
            }
        }
    };

    // Mimic continuous auto-focusing
    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };
}