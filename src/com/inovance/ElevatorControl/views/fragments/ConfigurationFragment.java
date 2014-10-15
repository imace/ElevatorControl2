package com.inovance.elevatorcontrol.views.fragments;

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
import com.inovance.bluetoothtool.BluetoothTool;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.activities.*;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.daos.ParameterGroupSettingsDao;
import com.inovance.elevatorcontrol.models.MoveInsideOutside;
import com.inovance.elevatorcontrol.models.ParameterDuplicate;
import com.inovance.elevatorcontrol.models.ParameterGroupSettings;
import com.inovance.elevatorcontrol.models.RealTimeMonitor;
import com.mobsandgeeks.adapters.InstantAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 电梯调试
 *
 * @author jch
 */
public class ConfigurationFragment extends Fragment {

    private static final String TAG = ConfigurationFragment.class.getSimpleName();

    private int tabIndex;

    private int layoutId;

    private Context context;

    public InstantAdapter<RealTimeMonitor> monitorAdapter;

    public InstantAdapter<ParameterGroupSettings> groupAdapter;

    private ListView monitorListView;

    private ListView groupListView;

    private ListView debugListView;

    private ListView duplicateListView;

    private List<RealTimeMonitor> monitorList = new ArrayList<RealTimeMonitor>();

    private List<ParameterGroupSettings> groupSettingsList = new ArrayList<ParameterGroupSettings>();

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        View view = getLayoutInflater(savedInstanceState).inflate(layoutId, container, false);
        switch (tabIndex) {
            case 0:
                monitorListView = (ListView) view.findViewById(R.id.monitor_list);
                initMonitorListView();
                break;
            case 1:
                groupListView = (ListView) view.findViewById(R.id.settings_list);
                initGroupListView();
                break;
            case 2:
                debugListView = (ListView) view.findViewById(R.id.test_list);
                initDebugListView();
                break;
            case 3:
                duplicateListView = (ListView) view.findViewById(R.id.copy_list);
                initDuplicateView();
                break;
        }
        return view;
    }

    private void initMonitorListView() {
        monitorAdapter = new InstantAdapter<RealTimeMonitor>(
                getActivity().getBaseContext(),
                R.layout.list_configuration_monitor_item,
                RealTimeMonitor.class,
                monitorList);
        monitorListView.setAdapter(monitorAdapter);
        monitorListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (BluetoothTool.getInstance().isPrepared()) {
                    RealTimeMonitor monitor = ConfigurationFragment.this.monitorList.get(position);
                    /*
                    if (monitor.getDescriptionType() == ApplicationConfig.DESCRIPTION_TYPE[2] ||
                            monitor.getDescriptionType() == ApplicationConfig.DESCRIPTION_TYPE[3]) {
                        AlertDialog dialog = CustomDialog.terminalDetailDialog(getActivity(), monitor).create();
                        dialog.show();
                    }
                    */
                    // 系统状态
                    if (monitor.getStateID() == ApplicationConfig.MonitorStateCode[12]) {
                        ((ConfigurationActivity) getActivity()).viewSystemTerminalStatus(position);
                    }
                    // 轿顶板输入状态
                    if (monitor.getStateID() == ApplicationConfig.MonitorStateCode[10]) {
                        ((ConfigurationActivity) getActivity()).viewCeilingInputStatus(position);
                    }
                    // 轿顶板输出状态
                    if (monitor.getStateID() == ApplicationConfig.MonitorStateCode[11]) {
                        ((ConfigurationActivity) getActivity()).viewCeilingOutputStatus(position);
                    }
                    // 高压输入端子状态
                    if (monitor.getStateID() == ApplicationConfig.MonitorStateCode[14]) {
                        ((ConfigurationActivity) getActivity()).viewHVInputTerminalStatus(position);
                    }
                    // 输入端子状态
                    if (monitor.getStateID() == ApplicationConfig.MonitorStateCode[5]) {
                        ((ConfigurationActivity) getActivity()).viewInputTerminalStatus(position);
                    }
                    // 输出端子状态
                    if (monitor.getStateID() == ApplicationConfig.MonitorStateCode[6]) {
                        ((ConfigurationActivity) getActivity()).viewOutputTerminalStatus(position);
                    }
                }
            }
        });
    }

    private void initGroupListView() {
        groupSettingsList.clear();
        groupSettingsList.addAll(ParameterGroupSettingsDao.findAll(context));
        groupAdapter = new InstantAdapter<ParameterGroupSettings>(
                getActivity().getApplicationContext(),
                R.layout.list_configuration_setting_item,
                ParameterGroupSettings.class, groupSettingsList);
        groupListView.setAdapter(groupAdapter);
        groupListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (BluetoothTool.getInstance().isPrepared()) {
                    Intent intent = new Intent(getActivity(), ParameterDetailActivity.class);
                    intent.putExtra("SelectedId", groupSettingsList.get(position).getId());
                    getActivity().startActivity(intent);
                }
            }
        });
    }

    private void initDebugListView() {
        final List<MoveInsideOutside> insideOut = MoveInsideOutside
                .getInsideOutLists(ConfigurationFragment.this.getActivity());
        InstantAdapter<MoveInsideOutside> instantAdapter = new InstantAdapter<MoveInsideOutside>(
                getActivity().getApplicationContext(),
                R.layout.list_configuration_debug_item, MoveInsideOutside.class, insideOut);
        debugListView.setAdapter(instantAdapter);
        debugListView.setOnItemClickListener(new OnItemClickListener() {
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

    private void initDuplicateView() {
        final List<ParameterDuplicate> paramDuplicate = ParameterDuplicate
                .getParamDuplicateLists(ConfigurationFragment.this.getActivity());
        InstantAdapter<ParameterDuplicate> instantAdapter = new InstantAdapter<ParameterDuplicate>(
                getActivity().getApplicationContext(),
                R.layout.list_configuration_duplicate_item, ParameterDuplicate.class,
                paramDuplicate);
        duplicateListView.setAdapter(instantAdapter);
        duplicateListView.setOnItemClickListener(new OnItemClickListener() {
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

    public void reloadDataSource(List<RealTimeMonitor> items) {
        monitorList.clear();
        monitorList.addAll(items);
        monitorAdapter.notifyDataSetChanged();
    }

    public void reloadDataSource() {
        groupSettingsList.clear();
        groupSettingsList.addAll(ParameterGroupSettingsDao.findAll(context));
        groupAdapter.notifyDataSetChanged();
    }
}
