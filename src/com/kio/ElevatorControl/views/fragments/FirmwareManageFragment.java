package com.kio.ElevatorControl.views.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.daos.MenuValuesDao;
import com.kio.ElevatorControl.models.Firmware;
import com.mobsandgeeks.adapters.InstantAdapter;

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
            ((Object)this).getClass().getMethod(MenuValuesDao.getFirmwareManageTabsLoadMethodName(tabIndex, context))
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

    public void loadFirmwareApplyView() {

    }

    public void loadFirmwareDownloadView() {
        ListView listView = (ListView) this.getActivity().findViewById(
                R.id.download_list);
        List<Firmware> firmwareLists = new ArrayList<Firmware>();
        Firmware firmware = new Firmware();
        firmware.setName("固件1");
        firmware.setVersion("0.1");
        firmware.setStatus(true);
        firmwareLists.add(firmware);
        InstantAdapter<Firmware> instantAdapter = new InstantAdapter<Firmware>(
                getActivity().getApplicationContext(),
                R.layout.firmware_download_item, Firmware.class, firmwareLists);
        listView.setAdapter(instantAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            }
        });
    }

    public void loadFirmwareBurnView() {

    }

}
