package com.kio.ElevatorControl.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Views;
import com.kio.ElevatorControl.R;
import org.holoeverywhere.app.Activity;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-4.
 * Time: 10:39.
 */

public class ScanBarcodeActivity extends Activity {

    @InjectView(R.id.start_scan_button)
    Button startScanButton;

    @InjectView(R.id.scan_result_edit_text)
    EditText scanResultEditText;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.scan_barcode_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_scan_barcode);
        Views.inject(this);
    }

    @OnClick(R.id.start_scan_button)
    public void startScanButtonClick(View view) {
        startActivity(new Intent(ScanBarcodeActivity.this, BarcodeCaptureActivity.class));
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