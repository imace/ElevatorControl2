package com.inovance.elevatorcontrol.activities;

import android.os.Bundle;
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
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.barcode.ZXingScannerView;
import net.rdrei.android.dirchooser.DirectoryChooserFragment;
import net.rdrei.android.dirchooser.DirectoryChooserFragment.OnFragmentInteractionListener;

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
public class ZxingScannerActivity extends FragmentActivity implements OnFragmentInteractionListener {

    private final static String TAG = ZxingScannerActivity.class.getSimpleName();

    private TextView scanText;

    private Button scanButton;

    private Button saveButton;

    private ScannerFragment scannerFragment;

    private DirectoryChooserFragment directoryChooserDialog;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setContentView(R.layout.activity_zxing_scanner);
        directoryChooserDialog = DirectoryChooserFragment.newInstance("DirectoryChooserDialog", null);
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
                directoryChooserDialog.show(getFragmentManager(), null);
            }
        });

        scanButton.setEnabled(false);
        saveButton.setEnabled(false);
    }

    private void savePicture(String path) {
        if (scannerFragment.getBitmap() != null) {
            saveButton.setEnabled(false);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File pictureFile = new File(path + File.separator + "IMG_" + timeStamp + ".jpg");
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

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSelectDirectory(String path) {
        directoryChooserDialog.dismiss();
        if (path != null && path.length() > 0) {
            savePicture(path);
        }
    }

    @Override
    public void onCancelChooser() {
        directoryChooserDialog.dismiss();
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