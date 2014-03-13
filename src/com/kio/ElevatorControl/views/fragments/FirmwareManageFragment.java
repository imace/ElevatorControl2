package com.kio.ElevatorControl.views.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.adapters.FirmwareBurnAdapter;
import com.kio.ElevatorControl.adapters.FirmwareDownloadAdapter;
import com.kio.ElevatorControl.daos.MenuValuesDao;
import com.kio.ElevatorControl.models.Firmware;
import com.kio.ElevatorControl.views.customspinner.NoDefaultSpinner;
import org.holoeverywhere.widget.AutoCompleteTextView;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.ListView;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-5.
 * Time: 15:04.
 */
public class FirmwareManageFragment extends Fragment {

    private static final String TAG = FirmwareManageFragment.class.getSimpleName();

    // 当前tabIndex
    private int tabIndex;

    // 该碎片使用的布局的RId
    private int layoutId;

    private Context context;

    private View mContentView;

    public static FirmwareManageFragment newInstance(int tabIndex, Context context) {
        FirmwareManageFragment firmwareManageFragment = new FirmwareManageFragment();
        firmwareManageFragment.tabIndex = tabIndex;
        firmwareManageFragment.context = context;
        firmwareManageFragment.layoutId = MenuValuesDao.getFirmwareManageTabsLayoutId(tabIndex, context);
        if (firmwareManageFragment.layoutId == 0) {
            firmwareManageFragment.layoutId = R.layout.fragment_test;
        }
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
        try {
            // 反射执行
            ((Object) this).getClass().getMethod(MenuValuesDao.getFirmwareManageTabsLoadMethodName(tabIndex, context))
                    .invoke(this);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, e.getMessage());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
        } catch (IllegalAccessException e) {
            Log.e(TAG, e.getMessage());
        } catch (InvocationTargetException e) {
            Log.e(TAG, e.getTargetException().getMessage());
        }
    }

    /**
     * 固件申请
     */
    public void loadFirmwareApplyView() {
        AutoCompleteTextView equipmentManufacturers = (AutoCompleteTextView) getActivity().findViewById(R.id.equipment_manufacturers);
        NoDefaultSpinner equipmentModel = (NoDefaultSpinner) getActivity().findViewById(R.id.equipment_model);
        NoDefaultSpinner firmwareVersion = (NoDefaultSpinner) getActivity().findViewById(R.id.firmware_version);
        EditText remark = (EditText) getActivity().findViewById(R.id.remark);
        View submitApply = getActivity().findViewById(R.id.submit_apply);
        View progressView = getActivity().findViewById(R.id.submit_progress);
        // 提交申请 Async异步提交
        submitApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    /**
     * 固件提取
     */
    public void loadFirmwareDownloadView() {
        ListView listView = (ListView) getActivity().findViewById(R.id.download_list);
        List<Firmware> firmwareLists = new ArrayList<Firmware>();
        Firmware firmware = new Firmware(getActivity());
        firmware.setName("固件1");
        firmware.setVersion("0.1");
        firmware.setStatus(true);
        firmwareLists.add(firmware);
        FirmwareDownloadAdapter adapter = new FirmwareDownloadAdapter(getActivity(), firmwareLists);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            }
        });
    }

    /**
     * 固件烧录
     */
    public void loadFirmwareBurnView() {
        ListView listView = (ListView) getActivity().findViewById(R.id.firmware_list);
        List<Firmware> firmwareLists = new ArrayList<Firmware>();
        FirmwareBurnAdapter adapter = new FirmwareBurnAdapter(getActivity(), firmwareLists);
        listView.setAdapter(adapter);
    }

}
