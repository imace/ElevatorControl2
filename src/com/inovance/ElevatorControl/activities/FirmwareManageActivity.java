package com.inovance.ElevatorControl.activities;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.PopupMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import butterknife.InjectView;
import butterknife.Views;
import com.bluetoothtool.BluetoothTool;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.adapters.FirmwareManageAdapter;
import com.inovance.ElevatorControl.models.Firmware;
import com.inovance.ElevatorControl.views.fragments.FirmwareManageFragment;
import com.inovance.elevatorprogram.IProgram;
import com.inovance.elevatorprogram.MsgHandler;
import com.viewpagerindicator.TabPageIndicator;
import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.widget.Button;
import org.holoeverywhere.widget.ProgressBar;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * 固件管理
 */
public class FirmwareManageActivity extends Activity {

    private static final String TAG = FirmwareManageActivity.class.getSimpleName();

    /**
     * 注入页面元素
     */
    @InjectView(R.id.pager)
    public ViewPager pager;

    @InjectView(R.id.indicator)
    protected TabPageIndicator indicator;

    private View firmwareMetaView;

    private View burnView;

    private TextView firmwareMetaTextView;

    private TextView burningMessageTextView;

    private ProgressBar burningProgressBar;

    private Button dlgButton;

    private FirmwareManageAdapter mFirmwareManageAdapter;

    public int pageIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firmware_manage);
        Views.inject(this);
        mFirmwareManageAdapter = new FirmwareManageAdapter(this);
        pager.setAdapter(mFirmwareManageAdapter);
        pager.setOffscreenPageLimit(3);
        indicator.setViewPager(pager);
        indicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                pageIndex = i;
                FirmwareManageFragment fragment = mFirmwareManageAdapter.getItem(pageIndex);
                if (fragment != null) {
                    switch (pageIndex) {
                        case 0:
                            fragment.loadFirmwareApplyView();
                            break;
                        case 1:
                            fragment.loadFirmwareDownloadView();
                            break;
                        case 2:
                            fragment.loadFirmwareBurnView();
                            break;
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Change Pager Index
     *
     * @param index Index
     */
    public void changePagerIndex(int index) {
        if (index != pager.getCurrentItem()) {
            pager.setCurrentItem(index);
        }
    }

    /**
     * Firmware burn item more option button click
     *
     * @param view     View
     * @param position GridView index
     * @param firmware Firmware item
     */
    public void onClickFirmwareBurnItemMoreOption(View view, int position, Firmware firmware) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.burn_option_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                AlertDialog.Builder builder = new AlertDialog.Builder(FirmwareManageActivity.this);
                LayoutInflater inflater = FirmwareManageActivity.this.getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.firmware_burn_dialog, null);
                firmwareMetaView = dialogView.findViewById(R.id.firmware_meta_view);
                burnView = dialogView.findViewById(R.id.burn_view);
                firmwareMetaTextView = (TextView) dialogView.findViewById(R.id.firmware_meta);
                burningMessageTextView = (TextView) dialogView.findViewById(R.id.burning_message);
                burningProgressBar = (ProgressBar) dialogView.findViewById(R.id.progress_bar);
                builder.setTitle(R.string.will_burn_firmware_text);
                builder.setView(dialogView);
                builder.setPositiveButton(R.string.burn_firmware_text, null);
                burningProgressBar.setMax(100);
                try {
                    FileInputStream fileInputStream = new FileInputStream("/storage/emulated/0/Nice3000+_Mcbs.bin");
                    if (BluetoothTool.getInstance(FirmwareManageActivity.this).isConnected()) {
                        BurnHandler burnHandler = new BurnHandler();
                        BluetoothSocket socket = BluetoothTool.getInstance(FirmwareManageActivity.this).bluetoothSocket;
                        IProgram.getInstance().SetProgramPara(socket, fileInputStream, burnHandler);
                        IProgram.getInstance().GetBinFileInfo();
                        firmwareMetaTextView.setText(IProgram.getInstance().GetBinFileInfo());
                    } else {
                        Toast.makeText(FirmwareManageActivity.this,
                                R.string.not_connect_device_error,
                                android.widget.Toast.LENGTH_SHORT)
                                .show();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                final AlertDialog dialog = builder.create();
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.setTitle(R.string.burning_firmware_text);
                        dlgButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        dlgButton.setText(R.string.dialog_btn_cancel);
                        dlgButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialog.dismiss();
                            }
                        });
                        dlgButton.invalidate();
                        if (BluetoothTool.getInstance(FirmwareManageActivity.this).isConnected()) {
                            IProgram.getInstance().StartProgram();//开始烧录
                        } else {
                            Toast.makeText(FirmwareManageActivity.this,
                                    R.string.not_connect_device_error,
                                    android.widget.Toast.LENGTH_SHORT)
                                    .show();
                        }
                        firmwareMetaView.setVisibility(View.GONE);
                        burnView.setVisibility(View.VISIBLE);
                        burningProgressBar.setVisibility(View.VISIBLE);
                        dlgButton.setEnabled(false);
                    }
                });
                return false;
            }
        });
        popupMenu.show();
    }

    // ================================= Firmware burn handler ========================================

    /**
     * Burn firmware handler
     */
    private class BurnHandler extends MsgHandler {

        @Override
        public void ProgramInfo(android.os.Message msg) {

            switch (msg.arg1) {
                case IProgram.ERROR_CODE:
                    //故障码
                    dlgButton.setEnabled(true);
                    break;
                case IProgram.PROGRAM_PROGRESS:
                    //烧录进度
                    int percent = msg.arg2;//百分比
                    burningProgressBar.setProgress(percent);
                    break;
                case IProgram.PROGRAM_TEXT_INFO:
                    //文字信息
                    if (msg.obj == null)
                        break;
                    String str = (String) msg.obj;
                    burningMessageTextView.setText(str);
                    break;
                case IProgram.PROGRAM_TIME:
                    //烧录总时间
                    int second = msg.arg2;
                    String strTime = String.format("烧录用时%d秒", second);
                    burningMessageTextView.setText(strTime);
                    dlgButton.setEnabled(true);
                    dlgButton.setText(R.string.dialog_btn_over);//按钮文字显示为“完成”
                    burningProgressBar.setVisibility(View.GONE);
                    break;
            }

        }

    }

}