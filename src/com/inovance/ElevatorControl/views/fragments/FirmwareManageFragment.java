package com.inovance.ElevatorControl.views.fragments;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.activities.FirmwareManageActivity;
import com.inovance.ElevatorControl.adapters.FirmwareBurnAdapter;
import com.inovance.ElevatorControl.adapters.FirmwareDownloadAdapter;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.daos.FirmwareDao;
import com.inovance.ElevatorControl.models.Firmware;
import com.inovance.ElevatorControl.models.GeneralDevice;
import com.inovance.ElevatorControl.models.SpecialDevice;
import com.inovance.ElevatorControl.models.Vendor;
import com.inovance.ElevatorControl.web.WebApi;
import org.holoeverywhere.widget.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-5.
 * Time: 15:04.
 */
public class FirmwareManageFragment extends Fragment implements WebApi.onGetResultListener,
        WebApi.onRequestFailureListener {

    private static final String TAG = FirmwareManageFragment.class.getSimpleName();

    private int tabIndex;

    private int layoutId;

    private Context context;

    private View mContentView;

    private AutoCompleteTextView vendorSelectView;

    private TextView generalApply;

    private TextView specialApply;

    private LinearLayout vendorView;

    private Spinner deviceSelectView;

    private EditText remarkEditText;

    private View submitView;

    private View progressView;

    private View submitTextView;

    private List<Vendor> vendorList;

    private List<GeneralDevice> generalDeviceList;

    private List<SpecialDevice> specialDeviceList;

    private LinearLayout downloadListLoadView;

    private ListView firmwareDownloadListView;

    private AlertDialog downloadDialog;

    private Firmware firmware;

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
        int currentPager = ((FirmwareManageActivity) getActivity()).pager.getCurrentItem();
        if (currentPager == 0) {
            loadFirmwareApplyView();
        }
    }

    /**
     * 固件申请
     */
    public void loadFirmwareApplyView() {
        vendorView = (LinearLayout) getActivity().findViewById(R.id.vendor_view);
        vendorSelectView = (AutoCompleteTextView) getActivity().findViewById(R.id.vendor);
        deviceSelectView = (Spinner) getActivity().findViewById(R.id.equipment_model);
        remarkEditText = (EditText) getActivity().findViewById(R.id.remark);
        submitView = getActivity().findViewById(R.id.submit_apply);
        progressView = getActivity().findViewById(R.id.submit_progress);
        submitTextView = getActivity().findViewById(R.id.submit_text);
        vendorSelectView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i < vendorList.size()) {
                    submitView.setEnabled(false);
                    WebApi.getInstance().getDeviceListByVendorIDS(vendorList.get(i).getSerialNumber());
                }
            }
        });
        generalApply = (TextView) getActivity().findViewById(R.id.general_device);
        specialApply = (TextView) getActivity().findViewById(R.id.special_device);
        generalApply.setSelected(true);
        generalApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!generalApply.isSelected()) {
                    generalApply.setSelected(true);
                    specialApply.setSelected(false);
                    vendorView.setVisibility(View.GONE);
                    WebApi.getInstance().getDeviceList();
                }
            }
        });
        specialApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!specialApply.isSelected()) {
                    generalApply.setSelected(false);
                    specialApply.setSelected(true);
                    vendorView.setVisibility(View.VISIBLE);
                    WebApi.getInstance().getVendorList();
                }
            }
        });
        submitView.setEnabled(false);
        WebApi.getInstance().setOnFailureListener(this);
        WebApi.getInstance().setOnResultListener(this);
        WebApi.getInstance().getDeviceList();
        submitView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String bluetoothAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
                    if (deviceSelectView.getCount() > 0) {
                        int deviceIndex = deviceSelectView.getSelectedItemPosition();
                        if (generalApply.isSelected()) {
                            if (deviceIndex < generalDeviceList.size()) {
                                int deviceID = generalDeviceList.get(deviceIndex).getID();
                                WebApi.getInstance().applyFirmware(getActivity(),
                                        bluetoothAddress,
                                        String.valueOf(deviceID),
                                        remarkEditText.getText().toString());
                                progressView.setVisibility(View.VISIBLE);
                                submitTextView.setVisibility(View.GONE);
                                submitView.setEnabled(false);
                            }
                        }
                        if (specialApply.isSelected()) {
                            if (deviceIndex < specialDeviceList.size()) {
                                String deviceID = specialDeviceList.get(deviceIndex).getNumber();
                                WebApi.getInstance().applyFirmware(getActivity(),
                                        bluetoothAddress,
                                        deviceID,
                                        remarkEditText.getText().toString());
                                progressView.setVisibility(View.VISIBLE);
                                submitTextView.setVisibility(View.GONE);
                                submitView.setEnabled(false);
                            }
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(getActivity(),
                            R.string.get_bluetooth_address_error,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 固件提取
     */
    public void loadFirmwareDownloadView() {
        downloadListLoadView = (LinearLayout) getActivity().findViewById(R.id.load_view);
        firmwareDownloadListView = (ListView) getActivity().findViewById(R.id.download_list);
        try {
            String bluetoothAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
            WebApi.getInstance().setOnResultListener(this);
            WebApi.getInstance().setOnFailureListener(this);
            downloadListLoadView.setVisibility(View.VISIBLE);
            firmwareDownloadListView.setVisibility(View.GONE);
            WebApi.getInstance().getAllFirmwareNotDownload(bluetoothAddress);
        } catch (Exception e) {
            Toast.makeText(getActivity(), R.string.get_bluetooth_address_error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 固件烧录
     */
    public void loadFirmwareBurnView() {
        GridView gridView = (GridView) getActivity().findViewById(R.id.firmware_list);
        List<Firmware> firmwareLists = new ArrayList<Firmware>();
        FirmwareBurnAdapter adapter = new FirmwareBurnAdapter((FirmwareManageActivity) getActivity(),
                firmwareLists);
        gridView.setAdapter(adapter);
    }

    /**
     * Download Firmware
     *
     * @param firmware Firmware
     */
    private void downloadFirmware(Firmware firmware) {
        View dialogView = getActivity().getLayoutInflater().inflate(R.layout.firmware_download_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                R.style.CustomDialogStyle)
                .setView(dialogView)
                .setTitle(R.string.download_firmware_dialog_title);
        downloadDialog = builder.create();
        downloadDialog.show();
        downloadDialog.setCancelable(false);
        downloadDialog.setCanceledOnTouchOutside(false);
        this.firmware = firmware;
        WebApi.getInstance().setOnFailureListener(this);
        WebApi.getInstance().setOnResultListener(this);
        WebApi.getInstance().downloadFirmwareFromServer(firmware.getID());
    }

    // ============================================= Web APi Listener ================================= //
    @Override
    public void onResult(String tag, String responseString) {
        int currentPager = ((FirmwareManageActivity) getActivity()).pager.getCurrentItem();
        if (currentPager == 0) {
            if (tag.equalsIgnoreCase(ApplicationConfig.GetDeviceList)) {
                try {
                    ArrayList<String> modeNames = new ArrayList<String>();
                    generalDeviceList = new ArrayList<GeneralDevice>();
                    JSONArray jsonArray = new JSONArray(responseString);
                    int size = jsonArray.length();
                    for (int i = 0; i < size; i++) {
                        JSONObject object = jsonArray.getJSONObject(i);
                        GeneralDevice device = new GeneralDevice(object);
                        modeNames.add(object.optString("DeviceName") + "-" + object.optString("DeviceNum"));
                        generalDeviceList.add(device);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                            android.R.layout.simple_spinner_item,
                            modeNames.toArray(new String[modeNames.size()]));
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    if (deviceSelectView != null) {
                        deviceSelectView.setAdapter(adapter);
                        FirmwareManageFragment.this.submitView.setEnabled(true);
                    }
                } catch (JSONException e) {
                    Toast.makeText(getActivity(), R.string.read_data_error, Toast.LENGTH_SHORT).show();
                }
            }
            if (tag.equalsIgnoreCase(ApplicationConfig.GetVendorList)) {
                try {
                    ArrayList<String> vendorNames = new ArrayList<String>();
                    vendorList = new ArrayList<Vendor>();
                    JSONArray jsonArray = new JSONArray(responseString);
                    int size = jsonArray.length();
                    for (int i = 0; i < size; i++) {
                        JSONObject object = jsonArray.getJSONObject(i);
                        Vendor vendor = new Vendor();
                        vendor.setSerialNumber(object.optString("VendorNum"));
                        vendor.setName(object.optString("VendorName"));
                        vendorNames.add(vendor.getName());
                        vendorList.add(vendor);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                            android.R.layout.simple_dropdown_item_1line,
                            vendorNames.toArray(new String[vendorNames.size()]));
                    if (vendorSelectView != null) {
                        vendorSelectView.setAdapter(adapter);
                    }
                } catch (JSONException e) {
                    Toast.makeText(getActivity(), R.string.read_data_error, Toast.LENGTH_SHORT).show();
                }
            }
            if (tag.equalsIgnoreCase(ApplicationConfig.GetDeviceListByVendorID)) {
                try {
                    ArrayList<String> modeNames = new ArrayList<String>();
                    specialDeviceList = new ArrayList<SpecialDevice>();
                    JSONArray jsonArray = new JSONArray(responseString);
                    int size = jsonArray.length();
                    for (int i = 0; i < size; i++) {
                        JSONObject object = jsonArray.getJSONObject(i);
                        SpecialDevice device = new SpecialDevice(object);
                        modeNames.add(device.getNumber());
                        specialDeviceList.add(device);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                            android.R.layout.simple_spinner_item,
                            modeNames.toArray(new String[modeNames.size()]));
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    if (deviceSelectView != null) {
                        deviceSelectView.setAdapter(adapter);
                    }
                } catch (JSONException e) {
                    Toast.makeText(getActivity(), R.string.read_data_error, Toast.LENGTH_SHORT).show();
                }
            }
            if (tag.equalsIgnoreCase(ApplicationConfig.ApplyFirmwareApplication)) {
                if (responseString.equalsIgnoreCase("True")) {
                    if (progressView != null && submitTextView != null && submitView != null) {
                        progressView.setVisibility(View.GONE);
                        submitTextView.setVisibility(View.VISIBLE);
                        submitView.setEnabled(true);
                        Toast.makeText(getActivity(), R.string.wait_for_approve_text, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getActivity(), responseString, Toast.LENGTH_SHORT).show();
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
                            public void onClick(int position, Firmware firmware) {
                                FirmwareManageFragment.this.downloadFirmware(firmware);
                            }
                        });
                        if (downloadListLoadView != null && firmwareDownloadListView != null) {
                            downloadListLoadView.setVisibility(View.GONE);
                            firmwareDownloadListView.setVisibility(View.VISIBLE);
                            firmwareDownloadListView.setAdapter(adapter);
                        }
                    } catch (JSONException e) {
                        Toast.makeText(getActivity(), R.string.read_data_error, Toast.LENGTH_SHORT).show();
                    }
                }
            }
            if (tag.equalsIgnoreCase(ApplicationConfig.DownloadFirmware)) {
                byte[] binData = Base64.decode(responseString, Base64.DEFAULT);
                boolean exist = createDirIfNotExists("/FirmwareBin");
                if (exist) {
                    File file = new File(Environment.getExternalStorageDirectory().getPath()
                            + "/FirmwareBin"
                            + "/Test.bin");
                    try {
                        FileOutputStream outputStream = new FileOutputStream(file);
                        outputStream.write(binData);
                        outputStream.close();
                        if (firmware != null) {
                            // 存储到数据库中
                            FirmwareDao.saveItem(getActivity(), firmware);
                            // 从服务器删除已提取的程序
                            WebApi.getInstance().deleteFileFromServer(firmware.getID());
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (tag.equalsIgnoreCase(ApplicationConfig.DeleteFile)) {
                if (responseString.equalsIgnoreCase("True")) {
                    if (downloadDialog != null) {
                        downloadDialog.dismiss();
                    }
                    Toast.makeText(getActivity(), R.string.download_successful_message, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onFailure(int statusCode, Throwable throwable) {
        int currentPager = ((FirmwareManageActivity) getActivity()).pager.getCurrentItem();
        if (currentPager == 0) {
            if (progressView != null && submitTextView != null && submitView != null) {
                progressView.setVisibility(View.GONE);
                submitTextView.setVisibility(View.VISIBLE);
                submitView.setEnabled(true);
            }
        }
    }

    public static boolean createDirIfNotExists(String path) {
        boolean created = true;
        File file = new File(Environment.getExternalStorageDirectory(), path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                created = false;
            }
        }
        return created;
    }
}
