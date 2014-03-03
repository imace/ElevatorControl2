package com.kio.ElevatorControl.views.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.daos.ErrorHelpDao;
import com.kio.ElevatorControl.daos.ValuesDao;
import com.kio.ElevatorControl.models.ErrorHelp;

import java.lang.reflect.InvocationTargetException;

public class FailureFragment extends Fragment {

    private final String HTAG = "FailureFragment";

    // 当前tabIndex
    private int tabIndex;

    // 该碎片使用的布局的RId
    private int layoutId;

    private Context context;

    /**
     * 记录下当前选中的tabIndex
     *
     * @param tabIndex
     * @param ctx
     * @return
     */
    public static FailureFragment newInstance(int tabIndex, Context ctx) {
        FailureFragment t = new FailureFragment();
        t.tabIndex = tabIndex;
        t.context = ctx;
        t.layoutId = ValuesDao.getFailureTabsLayoutId(tabIndex, ctx);
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
        super.onResume();
        try {
            // 反射执行
            this.getClass().getMethod(ValuesDao.getFailureTabsLoadMethodName(tabIndex, context)).invoke(this);
        } catch (NoSuchMethodException e) {
            Log.e(HTAG, e.getMessage());
        } catch (IllegalArgumentException e) {
            Log.e(HTAG, e.getMessage());
        } catch (IllegalAccessException e) {
            Log.e(HTAG, e.getMessage());
        } catch (InvocationTargetException e) {
            Log.e(HTAG, e.getTargetException().getMessage());
        }
    }

    /**
     * 当前故障
     */
    public void loadCurrent() {
    }

    /**
     * 历史故障
     */
    public void loadHistory() {
//		// 数据,自带context
//		final List<ErrorHelpLog> loglist = ErrorHelpLogDao.findAll(getActivity());
//		// 我们要操作的列表控件
//		ListView lstv = (ListView) getActivity().findViewById(R.id.failurehistorylist);
//		InstantAdapter<ErrorHelpLog> itadp = new InstantAdapter<ErrorHelpLog>(getActivity().getApplicationContext(), R.layout.list_failure_history_item, ErrorHelpLog.class,
//				loglist);
//		lstv.setAdapter(itadp);
//		lstv.setOnItemClickListener(new OnItemClickListener() {
//			@Override
//			public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
//				CustomDialoger.failureHistoryDailog(loglist.get(pos).getErrorHelp(),getActivity()).show();
//			}
//		});

    }

    public void loadDictionary() {
        ((Button) getActivity().findViewById(R.id.dictionary_error_btn)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                EditText edit = (EditText) getActivity().findViewById(R.id.dictionary_edit);
                TextView display = (TextView) getActivity().findViewById(R.id.dictionary_error_help_display);
                TextView level = (TextView) getActivity().findViewById(R.id.dictionary_error_help_level);
                TextView name = (TextView) getActivity().findViewById(R.id.dictionary_error_help_name);
                TextView reason = (TextView) getActivity().findViewById(R.id.dictionary_error_help_reason);
                TextView solution = (TextView) getActivity().findViewById(R.id.dictionary_error_help_solution);

                ErrorHelp ehp = ErrorHelpDao.findByDisplay(getActivity(), edit.getText().toString().trim().replaceAll("'", ""));
                if (ehp != null) {
                    display.setText(ehp.getDisplay());
                    level.setText(ehp.getLevel());
                    name.setText(ehp.getName());
                    reason.setText(ehp.getReason());
                    solution.setText(ehp.getSolution());
                }
            }
        });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 返回layoutId对应的布局
        return inflater.inflate(layoutId, container, false);
    }

}
