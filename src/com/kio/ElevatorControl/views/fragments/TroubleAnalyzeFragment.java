package com.kio.ElevatorControl.views.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.daos.ErrorHelpDao;
import com.kio.ElevatorControl.daos.MenuValuesDao;
import com.kio.ElevatorControl.models.ErrorHelp;

import java.lang.reflect.InvocationTargetException;

public class TroubleAnalyzeFragment extends Fragment {

    private static final String TAG = TroubleAnalyzeFragment.class.getSimpleName();

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
    public static TroubleAnalyzeFragment newInstance(int tabIndex, Context ctx) {
        TroubleAnalyzeFragment troubleAnalyzeFragment = new TroubleAnalyzeFragment();
        troubleAnalyzeFragment.tabIndex = tabIndex;
        troubleAnalyzeFragment.context = ctx;
        troubleAnalyzeFragment.layoutId = MenuValuesDao.getTroubleAnalyzeTabsLayoutId(tabIndex, ctx);
        if (troubleAnalyzeFragment.layoutId == 0) {
            troubleAnalyzeFragment.layoutId = R.layout.fragment_test;
        }
        return troubleAnalyzeFragment;
    }

    /**
     * 根据tabIndex来加载
     */
    @Override
    public void onResume() {
        super.onResume();
        try {
            // 反射执行
            ((Object) this).getClass().getMethod(MenuValuesDao.getTroubleAnalyzeTabsLoadMethodName(tabIndex, context)).invoke(this);
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
     * 当前故障
     */
    public void loadCurrentTroubleView() {

    }

    /**
     * 历史故障
     */
    public void loadHistoryTroubleView() {
//		// 数据,自带context
//		final List<ErrorHelpLog> loglist = ErrorHelpLogDao.findAll(getActivity());
//		// 我们要操作的列表控件
//		ListView lstv = (ListView) getActivity().findViewById(R.id.failurehistorylist);
//		InstantAdapter<ErrorHelpLog> itadp = new InstantAdapter<ErrorHelpLog>(getActivity().getApplicationContext(), R.layout.list_trouble_history_item, ErrorHelpLog.class,
//				loglist);
//		lstv.setAdapter(itadp);
//		lstv.setOnItemClickListener(new OnItemClickListener() {
//			@Override
//			public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
//				CustomDialog.failureHistoryDialog(loglist.get(pos).getErrorHelp(),getActivity()).show();
//			}
//		});
    }

    /**
     * 故障查询
     */
    public void loadSearchTroubleView() {
        final View contentContainer = getActivity().findViewById(R.id.content_container);
        final EditText searchEditText = (EditText) getActivity().findViewById(R.id.dictionary_edit);
        final TextView errorCode = (TextView) getActivity().findViewById(R.id.dictionary_error_help_display);
        final TextView level = (TextView) getActivity().findViewById(R.id.dictionary_error_help_level);
        final TextView name = (TextView) getActivity().findViewById(R.id.dictionary_error_help_name);
        final TextView reason = (TextView) getActivity().findViewById(R.id.dictionary_error_help_reason);
        final TextView solution = (TextView) getActivity().findViewById(R.id.dictionary_error_help_solution);
        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String searchCode = searchEditText.getText().toString().trim().replaceAll("'", "");
                    ErrorHelp errorHelp = ErrorHelpDao.findByDisplay(getActivity(), searchCode);
                    if (errorHelp != null) {
                        errorCode.setText(errorHelp.getDisplay());
                        level.setText(errorHelp.getLevel());
                        name.setText(errorHelp.getName());
                        reason.setText(errorHelp.getReason());
                        solution.setText(errorHelp.getSolution());
                        // 隐藏虚拟键盘
                        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.hideSoftInputFromWindow(searchEditText.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
                        contentContainer.setVisibility(View.VISIBLE);
                    } else {
                        errorCode.setText("");
                        errorCode.setText("");
                        level.setText("");
                        name.setText("");
                        reason.setText("");
                        solution.setText("");
                        contentContainer.setVisibility(View.INVISIBLE);
                        Toast toast = Toast.makeText(getActivity(), R.string.no_error_code_result, Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 返回layoutId对应的布局
        return inflater.inflate(layoutId, container, false);
    }

}