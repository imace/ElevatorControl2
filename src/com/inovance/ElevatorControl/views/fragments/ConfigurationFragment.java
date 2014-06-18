package com.inovance.ElevatorControl.views.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import com.bluetoothtool.BluetoothTool;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.activities.*;
import com.inovance.ElevatorControl.config.ApplicationConfig;
import com.inovance.ElevatorControl.daos.ParameterGroupSettingsDao;
import com.inovance.ElevatorControl.models.MoveInsideOutside;
import com.inovance.ElevatorControl.models.ParameterDuplicate;
import com.inovance.ElevatorControl.models.ParameterGroupSettings;
import com.inovance.ElevatorControl.models.RealTimeMonitor;
import com.inovance.ElevatorControl.views.dialogs.CustomDialog;
import com.mobsandgeeks.adapters.InstantAdapter;

import java.util.List;

/**
 * 电梯调试
 *
 * @author jch
 */
public class ConfigurationFragment extends Fragment {

    private static final String TAG = ConfigurationFragment.class.getSimpleName();

    // 当前tabIndex
    private int tabIndex;

    // 该碎片使用的布局的RId
    private int layoutId;

    private Context context;

    public InstantAdapter<RealTimeMonitor> monitorInstantAdapter;

    public InstantAdapter<ParameterGroupSettings> parameterGroupInstantAdapter;

    private ListView monitorListView;

    private List<ParameterGroupSettings> settingsGroup;

    private ListView settingGroupListView;

    private boolean hasBindListListener = false;

    private List<RealTimeMonitor> monitorList;

    /**
     * 记录下当前选中的tabIndex
     *
     * @param tabIndex tab index
     * @param ctx      context
     * @return fragment
     */
    public static ConfigurationFragment newInstance(int tabIndex, Context context, List<RealTimeMonitor> monitorList) {
        ConfigurationFragment configurationFragment = new ConfigurationFragment();
        configurationFragment.tabIndex = tabIndex;
        configurationFragment.context = context;
        int layout = R.layout.fragment_not_found;
        switch (tabIndex) {
            case 0:
                configurationFragment.monitorList = monitorList;
                layout = R.layout.configuration_tab_monitor;
                break;
            case 1:
                layout = R.layout.configuration_tab_setting;
                break;
            case 2:
                layout = R.layout.configuration_tab_debug;
                break;
            case 3:
                layout = R.layout.configuration_tab_duplicate;
                break;
        }
        configurationFragment.layoutId = layout;
        return configurationFragment;
    }

    /**
     * 根据tabIndex来加载
     */
    @Override
    public void onResume() {
        super.onResume();
        switch (tabIndex) {
            case 0:
                loadMonitorView();
                break;
            case 1:
                loadSettingView();
                break;
            case 2:
                loadDebugView();
                break;
            case 3:
                loadDuplicateView();
                break;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        return inflater.inflate(layoutId, container, false);
    }

    /**
     * 实时监控,加载内容
     */
    public void loadMonitorView() {
        if (monitorListView == null) {
            monitorListView = (ListView) getActivity().findViewById(R.id.monitor_list);
        }
        if (monitorList != null && monitorListView.getAdapter() == null) {
            monitorInstantAdapter = new InstantAdapter<RealTimeMonitor>(
                    getActivity().getBaseContext(),
                    R.layout.list_configuration_monitor_item,
                    RealTimeMonitor.class,
                    monitorList);
            monitorListView.setAdapter(monitorInstantAdapter);
        }
    }

    /**
     * Update Data
     *
     * @param monitorList RealTimeMonitor List
     */
    public void syncMonitorViewData(List<RealTimeMonitor> monitorList) {
        if (monitorInstantAdapter != null) {
            this.monitorList.clear();
            this.monitorList.addAll(monitorList);
            monitorInstantAdapter.notifyDataSetChanged();
        }
        if (monitorListView != null && !hasBindListListener) {
            monitorListView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    RealTimeMonitor monitor = ConfigurationFragment.this.monitorList.get(position);
                    if (monitor.getDescriptionType() == ApplicationConfig.DESCRIPTION_TYPE[2] ||
                            monitor.getDescriptionType() == ApplicationConfig.DESCRIPTION_TYPE[3]) {
                        AlertDialog dialog = CustomDialog.terminalDetailDialog(getActivity(), monitor).create();
                        dialog.setInverseBackgroundForced(true);
                        dialog.show();
                    }
                    if (monitor.getStateID() == ApplicationConfig.MonitorStateCode[14]) {
                        ((ConfigurationActivity) getActivity()).setHVInputTerminalStatus(monitor);
                    }
                    if (monitor.getStateID() == ApplicationConfig.MonitorStateCode[5]) {
                        ((ConfigurationActivity) getActivity()).seeInputTerminalStatus(monitor);
                    }
                    if (monitor.getStateID() == ApplicationConfig.MonitorStateCode[6]) {
                        ((ConfigurationActivity) getActivity()).seeOutputTerminalStatus(monitor);
                    }
                }
            });
            hasBindListListener = true;
        }
    }

    /**
     * 重新加载详细设置数据
     */
    public void reloadSettingViewData() {
        this.settingsGroup.clear();
        this.settingsGroup.addAll(ParameterGroupSettingsDao.findAll(context));
        parameterGroupInstantAdapter.notifyDataSetChanged();
    }

    /**
     * 参数设置,加载内容
     */
    public void loadSettingView() {
        if (settingsGroup == null && settingGroupListView == null) {
            settingsGroup = ParameterGroupSettingsDao.findAll(context);
            settingGroupListView = (ListView) getActivity().findViewById(R.id.settings_list);
            parameterGroupInstantAdapter = new InstantAdapter<ParameterGroupSettings>(
                    getActivity().getApplicationContext(),
                    R.layout.list_configuration_setting_item,
                    ParameterGroupSettings.class, settingsGroup);
            settingGroupListView.setAdapter(parameterGroupInstantAdapter);
            settingGroupListView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    if (BluetoothTool.getInstance().isPrepared()) {
                        Intent intent = new Intent(
                                ConfigurationFragment.this.getActivity(),
                                ParameterDetailActivity.class);
                        intent.putExtra("SelectedId", settingsGroup.get(position).getId());
                        ConfigurationFragment.this.getActivity().startActivity(intent);
                    }
                }
            });
        }
    }

    /**
     * 测试功能，加载内容，内召和外召。
     */
    public void loadDebugView() {
        final List<MoveInsideOutside> insideOut = MoveInsideOutside
                .getInsideOutLists(ConfigurationFragment.this.getActivity());
        ListView listView = (ListView) this.getActivity().findViewById(R.id.test_list);
        InstantAdapter<MoveInsideOutside> instantAdapter = new InstantAdapter<MoveInsideOutside>(
                getActivity().getApplicationContext(),
                R.layout.list_configuration_debug_item, MoveInsideOutside.class, insideOut);
        listView.setAdapter(instantAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (BluetoothTool.getInstance().isPrepared()) {
                    switch (position) {
                        case 0: {
                            ConfigurationFragment.this.getActivity().startActivity(
                                    new Intent(ConfigurationFragment.this.getActivity(),
                                            MoveInsideActivity.class));
                        }
                        break;
                        case 1: {
                            ConfigurationFragment.this.getActivity().startActivity(
                                    new Intent(ConfigurationFragment.this.getActivity(),
                                            MoveOutsideActivity.class));
                        }
                        break;
                    }
                }
            }
        });
    }

    /**
     * 参数拷贝,加载内容
     */
    public void loadDuplicateView() {
        final List<ParameterDuplicate> paramDuplicate = ParameterDuplicate
                .getParamDuplicateLists(ConfigurationFragment.this.getActivity());
        ListView listView = (ListView) this.getActivity().findViewById(R.id.copy_list);
        InstantAdapter<ParameterDuplicate> instantAdapter = new InstantAdapter<ParameterDuplicate>(
                getActivity().getApplicationContext(),
                R.layout.list_configuration_duplicate_item, ParameterDuplicate.class,
                paramDuplicate);
        listView.setAdapter(instantAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (BluetoothTool.getInstance().isPrepared()) {
                    switch (position) {
                        case 0: {
                            Intent intent = new Intent(ConfigurationFragment.this.getActivity(),
                                    ParameterUploadActivity.class);
                            ConfigurationFragment.this.getActivity().startActivity(intent);
                        }
                        break;
                        case 1: {
                            Intent intent = new Intent(ConfigurationFragment.this.getActivity(),
                                    ParameterDownloadActivity.class);
                            ConfigurationFragment.this.getActivity().startActivity(intent);
                        }
                        break;
                        case 2: {
                            final ConfigurationActivity activity = (ConfigurationActivity)
                                    ConfigurationFragment.this.getActivity();
                            AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                                    R.style.CustomDialogStyle)
                                    .setTitle(R.string.confirm_restore_title)
                                    .setMessage(R.string.confirm_restore_message)
                                    .setNegativeButton(R.string.dialog_btn_cancel, null)
                                    .setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (activity != null) {
                                                activity.restoreFactory();
                                            }
                                        }
                                    });
                            builder.create().show();
                        }
                        break;
                    }
                }
            }
        });
    }
}
