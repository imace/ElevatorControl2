package com.kio.ElevatorControl.handlers;

import android.app.Activity;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.TextView;
import com.hbluetooth.HHandler;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.activities.TroubleAnalyzeActivity;
import com.kio.ElevatorControl.models.ErrorHelp;

/**
 * 运行在CoreAcivity主线程,仅做一些ui操作
 *
 * @author jch
 */
public class CurrentErrorHandler extends HHandler {

    private ErrorHelp errorHelp;

    public CurrentErrorHandler(Activity activity) {
        super(activity);
        TAG = CurrentErrorHandler.class.getSimpleName();
    }

    @Override
    public void onMultiTalkBegin(Message msg) {
        super.onMultiTalkBegin(msg);
        errorHelp = null;
    }

    @Override
    public void onMultiTalkEnd(Message msg) {
        super.onMultiTalkEnd(msg);
        if (errorHelp != null) {
            ViewPager pager = ((TroubleAnalyzeActivity) activity).pager;
            TextView display = (TextView) pager.findViewById(R.id.current_error_help_display);
            TextView level = (TextView) pager.findViewById(R.id.current_error_help_level);
            TextView name = (TextView) pager.findViewById(R.id.current_error_help_name);
            TextView reason = (TextView) pager.findViewById(R.id.current_error_help_reason);
            TextView solution = (TextView) pager.findViewById(R.id.current_error_help_solution);
            name.setText(errorHelp.getName());
            display.setText(errorHelp.getDisplay());
            level.setText(errorHelp.getLevel());
            reason.setText(errorHelp.getReason());
            solution.setText(errorHelp.getSolution());
        } else {
            ((TroubleAnalyzeActivity) activity).loadCurrentTroubleView();
        }
    }

    @Override
    public void onTalkReceive(Message msg) {
        if (msg.obj != null && (msg.obj instanceof ErrorHelp)) {
            errorHelp = (ErrorHelp) msg.obj;
        }
    }

    @Override
    public void onTalkError(Message msg) {
        ((TroubleAnalyzeActivity) activity).loadCurrentTroubleView();
    }
}
