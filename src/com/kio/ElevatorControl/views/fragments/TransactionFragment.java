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
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.activities.CheckAuthorizationActivity;
import com.kio.ElevatorControl.activities.ParameterGroupActivity;
import com.kio.ElevatorControl.daos.ParameterGroupSettingsDao;
import com.kio.ElevatorControl.daos.ValuesDao;
import com.kio.ElevatorControl.models.InsideOut;
import com.kio.ElevatorControl.models.ParameterCopy;
import com.kio.ElevatorControl.models.ParameterGroupSettings;
import com.mobsandgeeks.adapters.InstantAdapter;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * 电梯调试
 *
 * @author jch
 */
public class TransactionFragment extends Fragment {

    private final String HTAG = "TransactionFragment";

    // 当前tabIndex
    private int tabIndex;

    // 该碎片使用的布局的RId
    private int layoutId;

    private Context context;

    // 内容数据源
    private List<ParameterGroupSettings> settingslist;

    /**
     * 记录下当前选中的tabIndex
     *
     * @param tabIndex
     * @param ctx
     * @return
     */
    public static TransactionFragment newInstance(int tabIndex, Context ctx) {
        TransactionFragment t = new TransactionFragment();
        t.tabIndex = tabIndex;
        t.context = ctx;
        t.layoutId = ValuesDao.getTransactionTabsLayoutId(tabIndex, ctx);
        if (t.layoutId == 0) {
            t.layoutId = R.layout.fragment_test;
        }
        return t;
    }

    /**
     * 根据tabIndex来加载
     */
    @Override
    public void onResume() {
        try {
            // 反射执行
            this.getClass()
                    .getMethod(
                            ValuesDao.getTransactionTabsLoadMethodName(
                                    tabIndex, context)).invoke(this);
        } catch (NoSuchMethodException e) {
            Log.e(HTAG, e.getMessage());
        } catch (IllegalArgumentException e) {
            Log.e(HTAG, e.getMessage());
        } catch (IllegalAccessException e) {
            Log.e(HTAG, e.getMessage());
        } catch (InvocationTargetException e) {
            Log.e(HTAG, e.getTargetException().getMessage());
        } finally {
            super.onResume();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 返回layoutId对应的布局
        return inflater.inflate(layoutId, container, false);
    }

    /**
     * 实时监控,加载内容
     */
    public void loadMonitorLV() {
    }

    /**
     * 参数设置,加载内容
     */
    public void loadSettingsLV() {
        // 列表值绑定到ParameterSettings对象 获取ParameterSettings对象列表,数据源
        settingslist = ParameterGroupSettingsDao.findAll(context);
        // 我们要操作的列表控件
        ListView lstv = (ListView) getActivity()
                .findViewById(R.id.settings_list);
        InstantAdapter<ParameterGroupSettings> itadp = new InstantAdapter<ParameterGroupSettings>(
                getActivity().getApplicationContext(),
                R.layout.list_transaction_settings_item,
                ParameterGroupSettings.class, settingslist);
        lstv.setAdapter(itadp);
        lstv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent inet = new Intent(
                        TransactionFragment.this.getActivity(),
                        ParameterGroupActivity.class);
                inet.putExtra("SelectedId", settingslist.get(position).getId());
                TransactionFragment.this.getActivity().startActivity(inet);
            }
        });
    }

    /**
     * 测试功能,加载内容
     */
    public void loadTestLV() {
        // 列表值绑定到InsideOut对象 获取InsideOut对象列表,数据源
        final List<InsideOut> iiout = InsideOut.INSIDEOUT();
        // 我们要操作的列表控件
        ListView lstv = (ListView) this.getActivity().findViewById(
                R.id.test_list);
        InstantAdapter<InsideOut> itadp = new InstantAdapter<InsideOut>(
                getActivity().getApplicationContext(),
                R.layout.list_transaction_test_item, InsideOut.class, iiout);

        lstv.setAdapter(itadp);

        lstv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                TransactionFragment.this.getActivity().startActivity(
                        new Intent(TransactionFragment.this.getActivity(),
                                CheckAuthorizationActivity.class));
            }
        });
    }

    /**
     * 参数拷贝,加载内容
     */
    public void loadCopyLV() {
        // 列表值绑定到InsideOut对象 获取InsideOut对象列表,数据源
        final List<ParameterCopy> paramcp = ParameterCopy.PARAMCOPY();
        // 我们要操作的列表控件
        ListView lstv = (ListView) this.getActivity().findViewById(
                R.id.copy_list);
        InstantAdapter<ParameterCopy> itadp = new InstantAdapter<ParameterCopy>(
                getActivity().getApplicationContext(),
                R.layout.list_transaction_copy_item, ParameterCopy.class,
                paramcp);
        lstv.setAdapter(itadp);
        lstv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                TransactionFragment.this.getActivity().startActivity(
                        new Intent(TransactionFragment.this.getActivity(),
                                CheckAuthorizationActivity.class));

            }
        });
    }
}
