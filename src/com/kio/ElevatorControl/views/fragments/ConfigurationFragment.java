package com.kio.ElevatorControl.views.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import com.hbluetooth.HBluetooth;
import com.hbluetooth.HCommunication;
import com.hbluetooth.HSerial;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.activities.*;
import com.kio.ElevatorControl.daos.MenuValuesDao;
import com.kio.ElevatorControl.daos.ParameterGroupSettingsDao;
import com.kio.ElevatorControl.models.MoveInsideOutside;
import com.kio.ElevatorControl.models.ParameterDuplicate;
import com.kio.ElevatorControl.models.ParameterGroupSettings;
import com.mobsandgeeks.adapters.InstantAdapter;

import java.lang.reflect.InvocationTargetException;
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

    // 内容数据源
    private List<ParameterGroupSettings> settingsList;

    /**
     * 记录下当前选中的tabIndex
     *
     * @param tabIndex tab index
     * @param ctx      context
     * @return fragment
     */
    public static ConfigurationFragment newInstance(int tabIndex, Context ctx) {
        ConfigurationFragment configurationFragment = new ConfigurationFragment();
        configurationFragment.tabIndex = tabIndex;
        configurationFragment.context = ctx;
        configurationFragment.layoutId = MenuValuesDao.getConfigurationTabsLayoutId(tabIndex, ctx);
        if (configurationFragment.layoutId == 0) {
            configurationFragment.layoutId = R.layout.fragment_test;
        }
        return configurationFragment;
    }

    /**
     * 根据tabIndex来加载
     */
    @Override
    public void onResume() {
        try {
            // 反射执行
            ((Object) this).getClass()
                    .getMethod(
                            MenuValuesDao.getConfigurationLoadMethodName(
                                    tabIndex, context)).invoke(this);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, e.getMessage());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
        } catch (IllegalAccessException e) {
            Log.e(TAG, e.getMessage());
        } catch (InvocationTargetException e) {
            Log.e(TAG, e.getTargetException().getMessage());
        } finally {
            super.onResume();
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

    }

    /**
     * 参数设置,加载内容
     */
    public void loadSettingView() {
        // 列表值绑定到ParameterSettings对象 获取ParameterSettings对象列表,数据源
        settingsList = ParameterGroupSettingsDao.findAll(context);
        // 我们要操作的列表控件
        ListView lstView = (ListView) getActivity()
                .findViewById(R.id.settings_list);
        InstantAdapter<ParameterGroupSettings> instantAdapter = new InstantAdapter<ParameterGroupSettings>(
                getActivity().getApplicationContext(),
                R.layout.list_configuration_setting_item,
                ParameterGroupSettings.class, settingsList);
        lstView.setAdapter(instantAdapter);
        lstView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent intent = new Intent(
                        ConfigurationFragment.this.getActivity(),
                        ParameterDetailActivity.class);
                intent.putExtra("SelectedId", settingsList.get(position).getId());
                ConfigurationFragment.this.getActivity().startActivity(intent);
            }
        });
    }

    /**
     * 测试功能，加载内容，内召和外召。
     */
    public void loadDebugView() {
        // 列表值绑定到InsideOut对象 获取InsideOut对象列表,数据源
        final List<MoveInsideOutside> insideOut = MoveInsideOutside
                .getInsideOutLists(ConfigurationFragment.this.getActivity());
        // 我们要操作的列表控件
        ListView lstView = (ListView) this.getActivity().findViewById(
                R.id.test_list);
        InstantAdapter<MoveInsideOutside> instantAdapter = new InstantAdapter<MoveInsideOutside>(
                getActivity().getApplicationContext(),
                R.layout.list_configuration_debug_item, MoveInsideOutside.class, insideOut);
        lstView.setAdapter(instantAdapter);
        lstView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
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
        });
    }

    /**
     * 参数拷贝,加载内容
     */
    public void loadDuplicateView() {
        // 列表值绑定到InsideOut对象 获取InsideOut对象列表,数据源
        final List<ParameterDuplicate> paramDuplicate = ParameterDuplicate
                .getParamDuplicateLists(ConfigurationFragment.this.getActivity());
        // 我们要操作的列表控件
        ListView lstView = (ListView) this.getActivity().findViewById(
                R.id.copy_list);
        InstantAdapter<ParameterDuplicate> instantAdapter = new InstantAdapter<ParameterDuplicate>(
                getActivity().getApplicationContext(),
                R.layout.list_configuration_duplicate_item, ParameterDuplicate.class,
                paramDuplicate);
        lstView.setAdapter(instantAdapter);
        lstView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
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
                        // 恢复出厂参数设置
                        /*
                        HCommunication[] communications = new HCommunication[1];
                        communications[0] = new HCommunication() {
                            @Override
                            public void beforeSend() {
                                this.setSendBuffer(HSerial.crc16(HSerial.hexStr2Ints("010660030001")));
                            }

                            @Override
                            public void afterSend() {

                            }

                            @Override
                            public void beforeReceive() {

                            }

                            @Override
                            public void afterReceive() {

                            }

                            @Override
                            public Object onParse() {
                                if (HSerial.isCRC16Valid(getReceivedBuffer())) {
                                    // 通过验证
                                    byte[] received = HSerial.trimEnd(getReceivedBuffer());
                                    Log.v(TAG, HSerial.byte2HexStr(received));
                                    return null;
                                }
                                return null;
                            }
                        };
                        if (HBluetooth.getInstance(ConfigurationFragment.this.getActivity()).isPrepared()) {
                            HBluetooth.getInstance(ConfigurationFragment.this.getActivity())
                                    .setCommunications(communications)
                                    .Start();
                        }
                        */
                    }
                    break;
                }
            }
        });
    }
}
