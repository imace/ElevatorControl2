package com.inovance.elevatorcontrol.views.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.daos.ErrorHelpDao;
import com.inovance.elevatorcontrol.models.ErrorHelp;

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
     * @param tabIndex Tab Index
     * @param ctx      Context
     * @return Fragment
     */
    public static TroubleAnalyzeFragment newInstance(int tabIndex, Context context) {
        TroubleAnalyzeFragment troubleAnalyzeFragment = new TroubleAnalyzeFragment();
        troubleAnalyzeFragment.tabIndex = tabIndex;
        troubleAnalyzeFragment.context = context;
        int layout = R.layout.fragment_not_found;
        switch (tabIndex) {
            case 0:
                layout = R.layout.trouble_analyze_tab_current;
                break;
            case 1:
                layout = R.layout.trouble_analyze_tab_history;
                break;
            case 2:
                layout = R.layout.trouble_analyze_tab_search;
                break;
        }
        troubleAnalyzeFragment.layoutId = layout;
        return troubleAnalyzeFragment;
    }

    /**
     * 根据tabIndex来加载
     */
    @Override
    public void onResume() {
        super.onResume();
        switch (tabIndex) {
            case 2:
                loadSearchTroubleView();
                break;
        }
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
                    String inputText = searchEditText.getText().toString().trim();
                    String searchCode = "";
                    if (inputText.contains("E") || inputText.contains("e")) {
                        searchCode = String.format("E%02d",
                                Integer.parseInt(inputText.substring(1, inputText.length())));
                    } else {
                        if (isNumeric(inputText)) {
                            searchCode = String.format("E%02d", Integer.parseInt(inputText));
                        }
                    }
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

    /**
     * Check String
     *
     * @param str Number String
     * @return True Or False
     */
    public static boolean isNumeric(String str) {
        try {
            double d = Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(layoutId, container, false);
    }

}
