package com.inovance.elevatorcontrol.views.fragments;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.activities.FirmwareManageActivity;
import com.inovance.elevatorcontrol.adapters.FirmwareBurnAdapter;
import com.inovance.elevatorcontrol.adapters.FirmwareDownloadAdapter;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.config.ParameterUpdateTool;
import com.inovance.elevatorcontrol.daos.FirmwareDao;
import com.inovance.elevatorcontrol.models.Firmware;
import com.inovance.elevatorcontrol.models.NormalDevice;
import com.inovance.elevatorcontrol.models.SpecialDevice;
import com.inovance.elevatorcontrol.models.Vendor;
import com.inovance.elevatorcontrol.web.WebInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-5.
 * Time: 15:04.
 */
public class FirmwareManageFragment extends Fragment implements WebInterface.OnRequestListener {

    private static final String TAG = FirmwareManageFragment.class.getSimpleName();

    private int tabIndex;

    private int layoutId;

    private Context context;

    /**
     * 读取提取固件列表进度
     */
    private LinearLayout downloadListLoadView;

    /**
     * 没有可以提取的固件显示的视图
     */
    private LinearLayout emptyFirmwareView;

    /**
     * 可以提取的固件列表
     */
    private ListView firmwareDownloadListView;

    private AlertDialog downloadDialog;

    private Firmware firmware;

    private List<Firmware> burnFirmwareList;

    private FirmwareBurnAdapter firmwareBurnAdapter;

    private TextView deviceType;

    private TextView vendorName;

    private List<Vendor> vendorList = new ArrayList<Vendor>();

    public static FirmwareManageFragment newInstance(int tabIndex, Context context) {
        FirmwareManageFragment firmwareManageFragment = new FirmwareManageFragment();
        firmwareManageFragment.tabIndex = tabIndex;
        firmwareManageFragment.context = context;
        int layout = R.layout.fragment_not_found;
        switch (tabIndex) {
            case 0:
                layout = R.layout.firmware_manage_tab_download;
                break;
            case 1:
                layout = R.layout.firmware_manage_tab_burn;
                break;
        }
        firmwareManageFragment.layoutId = layout;
        return firmwareManageFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        return inflater.inflate(layoutId, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        int currentPage = ((FirmwareManageActivity) getActivity()).pager.getCurrentItem();
        switch (currentPage) {
            case 0:
                refreshFirmwareBurnView();
                break;
        }
    }

    /**
     * 固件提取
     */
    public void loadFirmwareDownloadView() {
        downloadListLoadView = (LinearLayout) getActivity().findViewById(R.id.load_view);
        emptyFirmwareView = (LinearLayout) getActivity().findViewById(R.id.empty_view);
        firmwareDownloadListView = (ListView) getActivity().findViewById(R.id.download_list);
        try {
            String bluetoothAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
            WebInterface.getInstance().setOnRequestListener(this);
            downloadListLoadView.setVisibility(View.VISIBLE);
            emptyFirmwareView.setVisibility(View.GONE);
            firmwareDownloadListView.setVisibility(View.GONE);
            WebInterface.getInstance().getAllFirmwareNotDownload(getActivity(), bluetoothAddress);
        } catch (Exception e) {
            Toast.makeText(getActivity(), R.string.get_bluetooth_address_error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 刷新烧录视图
     */
    public void refreshFirmwareBurnView() {
        if (firmwareBurnAdapter != null) {
            burnFirmwareList = new ArrayList<Firmware>();
            burnFirmwareList.addAll(FirmwareDao.findAll(context));
            firmwareBurnAdapter.setFirmwareList(burnFirmwareList);
        }
        WebInterface.getInstance().setOnRequestListener(this);
        WebInterface.getInstance().getVendorList(getActivity());
        NormalDevice normalDevice = ParameterUpdateTool.getInstance().getNormalDevice();
        SpecialDevice specialDevice = ParameterUpdateTool.getInstance().getSpecialDevice();
        if (normalDevice != null) {
            if (deviceType != null && vendorName != null && normalDevice.getName() != null) {
                deviceType.setText(normalDevice.getName());
                vendorName.setText("标准设备");
            }
        }
        if (specialDevice != null) {
            if (deviceType != null && vendorName != null) {
                deviceType.setText(specialDevice.getName());
                for (Vendor vendor : vendorList) {
                    if (specialDevice.getVendorID() == vendor.getId()) {
                        vendorName.setText(vendor.getName());
                    }
                }
            }
        }
    }

    /**
     * 固件烧录
     */
    public void loadFirmwareBurnView() {
        deviceType = (TextView) getActivity().findViewById(R.id.device_type);
        deviceType.setText(ParameterUpdateTool.getInstance().getDeviceName());
        vendorName = (TextView) getActivity().findViewById(R.id.supplier_code);
        vendorName.setText(ParameterUpdateTool.getInstance().getSupplierCode());
        GridView gridView = (GridView) getActivity().findViewById(R.id.firmware_list);
        burnFirmwareList = FirmwareDao.findAll(context);
        if (firmwareBurnAdapter == null) {
            firmwareBurnAdapter = new FirmwareBurnAdapter((FirmwareManageActivity) getActivity(),
                    burnFirmwareList);
            gridView.setAdapter(firmwareBurnAdapter);
        } else {
            refreshFirmwareBurnView();
        }
    }

    /**
     * 下载固件
     *
     * @param firmware Firmware
     */
    private void downloadFirmware(View view, final Firmware firmware) {
        PopupMenu popupMenu = new PopupMenu(this.getActivity(), view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.firmware_download_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                View dialogView = getActivity().getLayoutInflater().inflate(R.layout.firmware_download_dialog, null);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                        R.style.GlobalDialogStyle)
                        .setView(dialogView)
                        .setTitle(R.string.download_firmware_dialog_title)
                        .setNegativeButton(R.string.dialog_btn_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });
                downloadDialog = builder.create();
                downloadDialog.show();
                downloadDialog.setCancelable(false);
                downloadDialog.setCanceledOnTouchOutside(false);
                Button negativeButton = downloadDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                negativeButton.setEnabled(false);

                FirmwareManageFragment.this.firmware = firmware;
                WebInterface.getInstance().setOnRequestListener(FirmwareManageFragment.this);
                WebInterface.getInstance().downloadFirmwareFromServer(getActivity(), firmware.getId());
                return false;
            }
        });
        popupMenu.show();
    }

    // ============================================= Web APi Listener ================================= //
    @Override
    public void onResult(String tag, String responseString) {
        if (tag.equalsIgnoreCase(ApplicationConfig.GetVendorList)) {
            if (responseString != null && responseString.length() > 0) {
                try {
                    vendorList = new ArrayList<Vendor>();
                    JSONArray jsonArray = new JSONArray(responseString);
                    int size = jsonArray.length();
                    for (int i = 0; i < size; i++) {
                        JSONObject object = jsonArray.getJSONObject(i);
                        Vendor vendor = new Vendor(object);
                        vendorList.add(vendor);
                    }
                } catch (JSONException e) {
                    Toast.makeText(getActivity(), R.string.read_data_error, Toast.LENGTH_SHORT).show();
                }
            }
        }
        int currentPager = ((FirmwareManageActivity) getActivity()).pager.getCurrentItem();
        if (currentPager == 0) {
            if (tag.equalsIgnoreCase(ApplicationConfig.GetAllFirmwareNotDownload)) {
                if (responseString != null && responseString.length() > 0) {
                    try {
                        JSONArray jsonArray = new JSONArray(responseString);
                        int size = jsonArray.length();
                        List<Firmware> firmwareList = new ArrayList<Firmware>();
                        for (int i = 0; i < size; i++) {
                            JSONObject object = jsonArray.getJSONObject(i);
                            Firmware firmware = new Firmware(object);
                            firmwareList.add(firmware);
                        }
                        FirmwareDownloadAdapter adapter = new FirmwareDownloadAdapter(getActivity(), firmwareList);
                        adapter.setOnDownloadButtonClickListener(new FirmwareDownloadAdapter
                                .onDownloadButtonClickListener() {
                            @Override
                            public void onClick(View view, int position, Firmware firmware) {
                                FirmwareManageFragment.this.downloadFirmware(view, firmware);
                            }
                        });
                        firmwareDownloadListView.setAdapter(adapter);
                        downloadListLoadView.setVisibility(View.GONE);
                        emptyFirmwareView.setVisibility(View.GONE);
                        firmwareDownloadListView.setVisibility(View.VISIBLE);
                    } catch (JSONException e) {
                        Toast.makeText(getActivity(), R.string.read_data_error, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    firmwareDownloadListView.setVisibility(View.GONE);
                    downloadListLoadView.setVisibility(View.GONE);
                    emptyFirmwareView.setVisibility(View.VISIBLE);
                }
            }
            if (tag.equalsIgnoreCase(ApplicationConfig.DownloadFirmware)) {
                byte[] binData = Base64.decode(responseString, Base64.DEFAULT);
                File directory = new File(getActivity().getFilesDir().getPath()
                        + "/"
                        + ApplicationConfig.FIRMWARE_FOLDER);
                if (!directory.exists()) {
                    directory.mkdir();
                }
                String fileName = UUID.randomUUID().toString();
                File file = new File(getActivity().getFilesDir().getPath()
                        + "/"
                        + ApplicationConfig.FIRMWARE_FOLDER
                        + "/"
                        + fileName + ".bin");
                try {
                    FileOutputStream outputStream = new FileOutputStream(file);
                    outputStream.write(binData);
                    outputStream.close();
                    if (firmware != null) {
                        // 存储到数据库中
                        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        firmware.setDownloadDate(dateFormat.format(new Date()));
                        firmware.setFileName(fileName);
                        FirmwareDao.saveItem(getActivity(), firmware);
                        // 从服务器删除已提取的程序
                        WebInterface.getInstance().deleteFileFromServer(getActivity(), firmware.getId());
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (tag.equalsIgnoreCase(ApplicationConfig.DeleteFile)) {
                if (responseString.equalsIgnoreCase("success")) {
                    if (downloadDialog != null && downloadDialog.isShowing()) {
                        downloadDialog.dismiss();
                    }
                    Toast.makeText(getActivity(), R.string.download_successful_message, Toast.LENGTH_SHORT).show();
                    downloadListLoadView.setVisibility(View.VISIBLE);
                    firmwareDownloadListView.setVisibility(View.GONE);
                    String bluetoothAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
                    WebInterface.getInstance().getAllFirmwareNotDownload(getActivity(), bluetoothAddress);
                }
            }
        }
    }

    @Override
    public void onFailure(int statusCode, Throwable throwable) {
        Toast.makeText(getActivity(), R.string.server_error_text, Toast.LENGTH_SHORT).show();
        if (downloadDialog != null && downloadDialog.isShowing()) {
            Button negativeButton = downloadDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setEnabled(true);
        }
    }

}
