package com.inovance.elevatorcontrol.views.fragments;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.view.*;
import android.widget.*;
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
import com.inovance.elevatorcontrol.views.component.SegmentControl;
import com.inovance.elevatorcontrol.views.form.NormalApplyForm;
import com.inovance.elevatorcontrol.views.form.SpecialApplyForm;
import com.inovance.elevatorcontrol.web.WebApi;
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
public class FirmwareManageFragment extends Fragment implements WebApi.OnGetResultListener,
        WebApi.OnRequestFailureListener {

    private static final String TAG = FirmwareManageFragment.class.getSimpleName();

    private int tabIndex;

    private int layoutId;

    private Context context;

    private SegmentControl segmentControl;

    private NormalApplyForm normalApplyView;

    private SpecialApplyForm specialApplyView;

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

    public static FirmwareManageFragment newInstance(int tabIndex, Context context) {
        FirmwareManageFragment firmwareManageFragment = new FirmwareManageFragment();
        firmwareManageFragment.tabIndex = tabIndex;
        firmwareManageFragment.context = context;
        int layout = R.layout.fragment_not_found;
        switch (tabIndex) {
            case 0:
                layout = R.layout.firmware_manage_tab_apply;
                break;
            case 1:
                layout = R.layout.firmware_manage_tab_download;
                break;
            case 2:
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
                loadFirmwareApplyView();
                break;
            case 1:
                refreshFirmwareBurnView();
                break;
        }
    }

    /**
     * 切换固件申请视图
     *
     * @param container View Container
     * @param type      申请类型
     */
    private void switchApplyView(LinearLayout container, int type) {
        container.removeAllViews();
        if (type == ParameterUpdateTool.Normal) {
            if (normalApplyView == null) {
                normalApplyView = new NormalApplyForm(getActivity());
            }
            container.addView(normalApplyView);
            WebApi.getInstance().setOnFailureListener(this);
            WebApi.getInstance().setOnResultListener(this);
            WebApi.getInstance().getNormalDeviceList(getActivity());
        }
        if (type == ParameterUpdateTool.Special) {
            if (specialApplyView == null) {
                specialApplyView = new SpecialApplyForm(getActivity());
            }
            container.addView(specialApplyView);
            WebApi.getInstance().setOnFailureListener(this);
            WebApi.getInstance().setOnResultListener(this);
            WebApi.getInstance().getVendorList(getActivity());
            WebApi.getInstance().getSpecialDeviceList(getActivity());
        }
    }

    /**
     * 固件申请
     */
    public void loadFirmwareApplyView() {
        if (segmentControl == null) {
            final LinearLayout container = (LinearLayout) getActivity().findViewById(R.id.apply_view_container);
            segmentControl = (SegmentControl) getActivity().findViewById(R.id.segment_control);
            if (ParameterUpdateTool.getInstance().getPermission() == ParameterUpdateTool.Normal) {
                segmentControl.setItems(getResources().getStringArray(R.array.permission_array_normal));
                normalApplyView = new NormalApplyForm(getActivity());
                segmentControl.setCurrentItem(0);
                switchApplyView(container, ParameterUpdateTool.Normal);
            }
            if (ParameterUpdateTool.getInstance().getPermission() == ParameterUpdateTool.Special) {
                segmentControl.setItems(getResources().getStringArray(R.array.permission_array_special));
                normalApplyView = new NormalApplyForm(getActivity());
                specialApplyView = new SpecialApplyForm(getActivity());
                switchApplyView(container, ParameterUpdateTool.Normal);
                segmentControl.setCurrentItem(0);
                segmentControl.setOnItemClickListener(new SegmentControl.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        switch (position) {
                            case 0:
                                switchApplyView(container, ParameterUpdateTool.Normal);
                                break;
                            case 1:
                                switchApplyView(container, ParameterUpdateTool.Special);
                                break;
                        }
                    }
                });
            }
        } else {
            switch (segmentControl.getCurrentItem()) {
                case 0:
                    WebApi.getInstance().setOnFailureListener(this);
                    WebApi.getInstance().setOnResultListener(this);
                    WebApi.getInstance().getNormalDeviceList(getActivity());
                    break;
                case 1:
                    WebApi.getInstance().setOnFailureListener(this);
                    WebApi.getInstance().setOnResultListener(this);
                    WebApi.getInstance().getVendorList(getActivity());
                    WebApi.getInstance().getSpecialDeviceList(getActivity());
                    break;
            }
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
            WebApi.getInstance().setOnResultListener(this);
            WebApi.getInstance().setOnFailureListener(this);
            downloadListLoadView.setVisibility(View.VISIBLE);
            emptyFirmwareView.setVisibility(View.GONE);
            firmwareDownloadListView.setVisibility(View.GONE);
            WebApi.getInstance().getAllFirmwareNotDownload(getActivity(), bluetoothAddress);
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
    }

    /**
     * 固件烧录
     */
    public void loadFirmwareBurnView() {
        TextView deviceType = (TextView) getActivity().findViewById(R.id.device_type);
        deviceType.setText(ParameterUpdateTool.getInstance().getDeviceName());
        TextView supplierCode = (TextView) getActivity().findViewById(R.id.supplier_code);
        supplierCode.setText(ParameterUpdateTool.getInstance().getSupplierCode());
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
                        R.style.CustomDialogStyle)
                        .setView(dialogView)
                        .setTitle(R.string.download_firmware_dialog_title);
                downloadDialog = builder.create();
                downloadDialog.show();
                downloadDialog.setCancelable(false);
                downloadDialog.setCanceledOnTouchOutside(false);
                FirmwareManageFragment.this.firmware = firmware;
                WebApi.getInstance().setOnFailureListener(FirmwareManageFragment.this);
                WebApi.getInstance().setOnResultListener(FirmwareManageFragment.this);
                WebApi.getInstance().downloadFirmwareFromServer(getActivity(), firmware.getId());
                return false;
            }
        });
        popupMenu.show();
    }

    // ============================================= Web APi Listener ================================= //
    @Override
    public void onResult(String tag, String responseString) {
            int currentPager = ((FirmwareManageActivity) getActivity()).pager.getCurrentItem();
            if (currentPager == 0) {
                if (tag.equalsIgnoreCase(ApplicationConfig.GetNormalDeviceList)) {
                    if (responseString != null && responseString.length() > 0) {
                        try {
                            List<NormalDevice> deviceList = new ArrayList<NormalDevice>();
                            JSONArray jsonArray = new JSONArray(responseString);
                            int size = jsonArray.length();
                            for (int i = 0; i < size; i++) {
                                JSONObject object = jsonArray.getJSONObject(i);
                                NormalDevice device = new NormalDevice(object);
                                deviceList.add(device);
                            }
                            normalApplyView.setSpinnerDataSource(deviceList);
                        } catch (JSONException e) {
                            Toast.makeText(getActivity(), R.string.read_data_error, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                if (tag.equalsIgnoreCase(ApplicationConfig.GetVendorList)) {
                    if (responseString != null && responseString.length() > 0) {
                        try {
                            List<Vendor> vendorList = new ArrayList<Vendor>();
                            JSONArray jsonArray = new JSONArray(responseString);
                            int size = jsonArray.length();
                            for (int i = 0; i < size; i++) {
                                JSONObject object = jsonArray.getJSONObject(i);
                                Vendor vendor = new Vendor(object);
                                vendorList.add(vendor);
                            }
                            specialApplyView.setVendorList(vendorList);
                        } catch (JSONException e) {
                            Toast.makeText(getActivity(), R.string.read_data_error, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                if (tag.equalsIgnoreCase(ApplicationConfig.GetSpecialDeviceList)) {
                    if (responseString != null && responseString.length() > 0) {
                        try {
                            List<SpecialDevice> deviceList = new ArrayList<SpecialDevice>();
                            JSONArray jsonArray = new JSONArray(responseString);
                            int size = jsonArray.length();
                            for (int i = 0; i < size; i++) {
                                JSONObject object = jsonArray.getJSONObject(i);
                                SpecialDevice device = new SpecialDevice(object);
                                deviceList.add(device);
                            }
                            specialApplyView.setDeviceList(deviceList);
                        } catch (JSONException e) {
                            Toast.makeText(getActivity(), R.string.read_data_error, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                if (tag.equalsIgnoreCase(ApplicationConfig.GetDeviceListByVendorID)) {
                    if (responseString != null && responseString.length() > 0) {
                        try {
                            List<SpecialDevice> deviceList = new ArrayList<SpecialDevice>();
                            JSONArray jsonArray = new JSONArray(responseString);
                            int size = jsonArray.length();
                            for (int i = 0; i < size; i++) {
                                JSONObject object = jsonArray.getJSONObject(i);
                                SpecialDevice device = new SpecialDevice(object);
                                deviceList.add(device);
                            }
                            specialApplyView.setDeviceList(deviceList);
                        } catch (JSONException e) {
                            Toast.makeText(getActivity(), R.string.read_data_error, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
            if (currentPager == 1) {
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
                    File directory = new File(getActivity().getExternalCacheDir().getPath()
                            + "/"
                            + ApplicationConfig.FIRMWARE_FOLDER);
                    if (!directory.exists()) {
                        directory.mkdir();
                    }
                    String fileName = UUID.randomUUID().toString();
                    File file = new File(getActivity().getExternalCacheDir().getPath()
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
                            WebApi.getInstance().deleteFileFromServer(getActivity(), firmware.getId());
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
                        WebApi.getInstance().getAllFirmwareNotDownload(getActivity(), bluetoothAddress);
                    }
                }
            }
    }

    @Override
    public void onFailure(int statusCode, Throwable throwable) {
        Toast.makeText(getActivity(), throwable.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
    }

}
