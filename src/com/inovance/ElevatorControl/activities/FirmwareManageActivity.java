package com.inovance.ElevatorControl.activities;

import android.app.AlertDialog;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.InjectView;
import butterknife.Views;
import com.bluetoothtool.BluetoothTool;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.adapters.FirmwareManageAdapter;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.daos.FirmwareDao;
import com.inovance.ElevatorControl.models.Firmware;
import com.inovance.ElevatorControl.utils.LogUtils;
import com.inovance.ElevatorControl.views.fragments.FirmwareManageFragment;
import com.inovance.ElevatorControl.web.WebApi;
import com.inovance.elevatorprogram.IProgram;
import com.inovance.elevatorprogram.MsgHandler;
import com.viewpagerindicator.TabPageIndicator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * 固件管理
 */
public class FirmwareManageActivity extends FragmentActivity {

    private static final String TAG = FirmwareManageActivity.class.getSimpleName();

    /**
     * View Pager
     */
    @InjectView(R.id.pager)
    public ViewPager pager;

    /**
     * View Pager Indicator
     */
    @InjectView(R.id.indicator)
    protected TabPageIndicator indicator;

    /**
     * 将要烧录的固件信息
     */
    private View firmwareMetaView;

    /**
     * 烧录视图
     */
    private View burnView;

    /**
     * 烧录的固件信息
     */
    private TextView firmwareMetaTextView;

    /**
     * 烧录中的提示信息
     */
    private TextView burningMessageTextView;

    /**
     * 烧录进度指示
     */
    private ProgressBar burningProgressBar;

    /**
     * 烧录取消按钮
     */
    private Button cancelButton;

    /**
     * 烧录确认按钮
     */
    private Button confirmButton;

    /**
     * 烧录对话框
     */
    private AlertDialog burnDialog;

    /**
     * View Pager Adapter
     */
    private FirmwareManageAdapter mFirmwareManageAdapter;

    private Firmware tempFirmWare;

    private BurnHandler burnHandler;

    /**
     * 当前的 View Pager Index
     */
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
        burnHandler = new BurnHandler();
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

    @Override
    protected void onPause() {
        super.onPause();
        WebApi.getInstance().removeListener();
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
    public void onClickFirmwareBurnItemMoreOption(View view, int position, final Firmware firmware) {
        tempFirmWare = firmware;
        PopupMenu popupMenu = new PopupMenu(this, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        // 烧录 DSP 只需要连接蓝牙设备，不需要识别设备类型
        if (BluetoothTool.getInstance().isConnected()) {
            inflater.inflate(R.menu.burn_option_menu_connected, popupMenu.getMenu());
        } else {
            inflater.inflate(R.menu.burn_option_menu_not_connected, popupMenu.getMenu());
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_burn:
                        showBurnFirmwareDialog();
                        break;
                    case R.id.action_delete:
                        showDeleteFirmwareDialog();
                        break;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    /**
     * 弹出烧录确认对话框
     */
    private void showBurnFirmwareDialog() {
        if (tempFirmWare.isExpired()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.firmware_expired_title)
                    .setMessage(R.string.firmware_expired_message)
                    .setPositiveButton(R.string.dialog_btn_ok, null);
            builder.create().show();
        } else {
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
            builder.setNegativeButton(R.string.dialog_btn_cancel, null);
            builder.setPositiveButton(R.string.burn_firmware_text, null);
            burningProgressBar.setMax(100);
            burnDialog = builder.create();
            burnDialog.setCancelable(false);
            burnDialog.setCanceledOnTouchOutside(false);
            burnDialog.show();
            try {
                File file = new File(getExternalCacheDir().getPath()
                        + "/" + ApplicationConfig.FIRMWARE_FOLDER
                        + "/" + tempFirmWare.getFileName() + ".bin");
                FileInputStream fileInputStream = new FileInputStream(file);
                // 蓝牙设备连接已经建立
                if (BluetoothTool.getInstance().isConnected()) {
                    BluetoothSocket socket = BluetoothTool.getInstance().bluetoothSocket;
                    IProgram.getInstance().SetProgramPara(socket, fileInputStream, burnHandler);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            cancelButton = burnDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            confirmButton = burnDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (burnDialog != null && burnDialog.isShowing()) {
                        burnDialog.dismiss();
                    }
                }
            });
            confirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (BluetoothTool.getInstance().isPrepared()) {
                        burnDialog.setTitle(R.string.burning_firmware_text);
                        confirmButton.setEnabled(false);
                        cancelButton.setEnabled(false);
                        firmwareMetaView.setVisibility(View.GONE);
                        burnView.setVisibility(View.VISIBLE);
                        burningProgressBar.setVisibility(View.VISIBLE);
                        IProgram.getInstance().StartProgram();
                    }
                }
            });
            cancelButton.setEnabled(true);
            confirmButton.setEnabled(false);
        }
    }

    /**
     * 弹出删除固件对话框
     */
    private void showDeleteFirmwareDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(FirmwareManageActivity.this);
        builder.setTitle(R.string.delete_firmware_title);
        builder.setMessage(R.string.delete_firmware_message);
        builder.setNegativeButton(R.string.dialog_btn_cancel, null);
        builder.setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // 删除固件
                File file = new File(getExternalCacheDir().getPath()
                        + "/" + ApplicationConfig.FIRMWARE_FOLDER
                        + "/" + tempFirmWare.getFileName() + ".bin");
                file.delete();
                // 删除数据库记录
                FirmwareDao.deleteItem(FirmwareManageActivity.this, tempFirmWare);
                // 刷新烧录固件列表
                FirmwareManageFragment fragment = mFirmwareManageAdapter.getItem(pageIndex);
                if (fragment != null) {
                    fragment.refreshFirmwareBurnView();
                }
            }
        });
        builder.create().show();
    }

    // ================================= Firmware burn handler ========================================

    /**
     * Burn firmware handler
     */
    private class BurnHandler extends MsgHandler {

        @Override
        public void ProgramInfo(android.os.Message msg) {
            switch (msg.arg1) {
                case IProgram.PROGRAM_BINFILE_READINFO:
                    if (1 == msg.arg2) {
                        cancelButton.setEnabled(true);
                        confirmButton.setEnabled(true);
                        firmwareMetaTextView.setText(IProgram.getInstance().GetBinFileInfo());
                    } else {
                        cancelButton.setEnabled(true);
                        confirmButton.setEnabled(false);
                        firmwareMetaTextView.setText((String) msg.obj);
                    }
                    break;
                case IProgram.ERROR_CODE:
                    // 故障码
                    cancelButton.setEnabled(true);
                    confirmButton.setEnabled(false);
                    // 写入出错日志
                    LogUtils.getInstance().write(ApplicationConfig.LogBurn, false, "");
                    break;
                case IProgram.PROGRAM_PROGRESS:
                    // 烧录进度
                    int percent = msg.arg2;
                    // 百分比
                    burningProgressBar.setProgress(percent);
                    break;
                case IProgram.PROGRAM_TEXT_INFO:
                    // 文字信息
                    if (msg.obj == null)
                        break;
                    String str = (String) msg.obj;
                    burningMessageTextView.setText(str);
                    break;
                case IProgram.PROGRAM_TIME:
                    // 烧录总时间
                    int second = msg.arg2;
                    String strTime = String.format("烧录用时%d分%d秒", second / 60, second % 60);
                    burningMessageTextView.setText(strTime);
                    cancelButton.setEnabled(true);
                    confirmButton.setEnabled(false);
                    cancelButton.setText(R.string.dialog_btn_over);
                    // 按钮文字显示为“完成”
                    burningProgressBar.setVisibility(View.GONE);
                    // 记录烧录次数
                    if (tempFirmWare != null) {
                        int burnTime = tempFirmWare.getBurnTimes() + 1;
                        if (burnTime == tempFirmWare.getTotalBurnTimes()) {
                            // 删除固件
                            File file = new File(getExternalCacheDir().getPath()
                                    + "/" + ApplicationConfig.FIRMWARE_FOLDER
                                    + "/" + tempFirmWare.getFileName() + ".bin");
                            file.delete();
                            // 删除数据库记录
                            FirmwareDao.deleteItem(FirmwareManageActivity.this, tempFirmWare);
                        } else {
                            tempFirmWare.setBurnTimes(burnTime);
                            FirmwareDao.updateItem(FirmwareManageActivity.this, tempFirmWare);
                        }
                        // 刷新烧录固件列表
                        FirmwareManageFragment fragment = mFirmwareManageAdapter.getItem(pageIndex);
                        if (fragment != null) {
                            fragment.refreshFirmwareBurnView();
                        }
                    }
                    // 写入烧录成功日志日志
                    LogUtils.getInstance().write(ApplicationConfig.LogBurn, true, "");
                    break;
            }

        }

    }

}
