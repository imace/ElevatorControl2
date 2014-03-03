package com.kio.ElevatorControl.handlers;

import android.app.Activity;
import android.os.Message;
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
public class FailureCurrentHandler extends HHandler {

    public FailureCurrentHandler(Activity activity) {
        super(activity);
        HTAG = "FailureHandler";
    }

    @Override
    public void onTalkReceive(Message msg) {
        if (msg.obj != null && (msg.obj instanceof ErrorHelp)) {
            ErrorHelp errorhelp = (ErrorHelp) msg.obj;

            TextView display = (TextView) ((TroubleAnalyzeActivity) activity).pager.findViewById(R.id.current_error_help_display);
            TextView level = (TextView) ((TroubleAnalyzeActivity) activity).pager.findViewById(R.id.current_error_help_level);
            TextView name = (TextView) ((TroubleAnalyzeActivity) activity).pager.findViewById(R.id.current_error_help_name);
            TextView reason = (TextView) ((TroubleAnalyzeActivity) activity).pager.findViewById(R.id.current_error_help_reason);
            TextView solution = (TextView) ((TroubleAnalyzeActivity) activity).pager.findViewById(R.id.current_error_help_solution);

            name.setText(errorhelp.getName());
            display.setText(errorhelp.getDisplay());
            level.setText(errorhelp.getLevel());
            reason.setText(errorhelp.getReason());
            solution.setText(errorhelp.getSolution());
        }
    }
}
