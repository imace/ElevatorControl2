package com.kio.ElevatorControl.views.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import com.devspark.progressfragment.ProgressFragment;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.activities.CheckAuthorizationActivity;
import com.kio.ElevatorControl.activities.MoveInsideActivity;
import com.kio.ElevatorControl.activities.MoveOutsideActivity;
import com.kio.ElevatorControl.activities.ParameterDetailActivity;
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
public class ConfigurationFragment extends ProgressFragment {

    private static final String TAG = ConfigurationFragment.class.getSimpleName();

    // 当前tabIndex
    private int tabIndex;

    // 该碎片使用的布局的RId
    private int layoutId;

    private Context context;

    private View mContentView;

    // 内容数据源
    private List<ParameterGroupSettings> settingsList;

    /**
     * 记录下当前选中的tabIndex
     *
     * @param tabIndex
     * @param ctx
     * @return
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
            ((Object)this).getClass()
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
        // 返回layoutId对应的布局
        mContentView = inflater.inflate(layoutId, container, false);
        //return mContentView;
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setContentView(mContentView);
    }

    /**
     * 实时监控,加载内容
     */
    public void loadMonitorView() {
        setContentShown(false);
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
        // 停止加载动画，显示内容。
        setContentShown(true);
    }

    /**
     * 测试功能，加载内容，内招和外招。
     */
    public void loadDebugView() {
        // 列表值绑定到InsideOut对象 获取InsideOut对象列表,数据源
        final List<MoveInsideOutside> insideOut = MoveInsideOutside.getInsideOutLists();
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
                switch (position){
                    case 0:{
                        ConfigurationFragment.this.getActivity().startActivity(
                                new Intent(ConfigurationFragment.this.getActivity(),
                                        MoveInsideActivity.class));
                    }
                    break;
                    case 1:{
                        ConfigurationFragment.this.getActivity().startActivity(
                                new Intent(ConfigurationFragment.this.getActivity(),
                                        MoveOutsideActivity.class));
                    }
                    break;
                }
            }
        });
        setContentShown(true);
    }

    /**
     * 参数拷贝,加载内容
     */
    public void loadDuplicateView() {
        // 列表值绑定到InsideOut对象 获取InsideOut对象列表,数据源
        final List<ParameterDuplicate> paramDuplicate = ParameterDuplicate.getParamDuplicateLists();
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
                ConfigurationFragment.this.getActivity().startActivity(
                        new Intent(ConfigurationFragment.this.getActivity(),
                                CheckAuthorizationActivity.class));

            }
        });
        setContentShown(true);
    }
}
