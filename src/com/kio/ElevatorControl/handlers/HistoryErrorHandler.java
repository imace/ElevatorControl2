package com.kio.ElevatorControl.handlers;

import android.app.Activity;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.hbluetooth.HHandler;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.models.ErrorHelp;
import com.kio.ElevatorControl.models.ErrorHelpLog;
import org.holoeverywhere.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-21.
 * Time: 11:00.
 */
public class HistoryErrorHandler extends HHandler {

    private List<ErrorHelp> errorList = new ArrayList<ErrorHelp>();

    private ViewPager pager;

    private List<WeakReference<LinearLayout>> viewList;

    private ErrorPagerAdapter pagerAdapter;

    public HistoryErrorHandler(Activity activity) {
        super(activity);
        viewList = new ArrayList<WeakReference<LinearLayout>>();
        TAG = FailureLogHandler.class.getSimpleName();
    }

    @Override
    public void onMultiTalkEnd(Message msg) {
        super.onMultiTalkEnd(msg);
        if (errorList.size() > 0) {
            if (pager == null) {
                pager = (ViewPager) activity.findViewById(R.id.history_view_pager);
            }
            if (pagerAdapter == null) {
                pagerAdapter = new ErrorPagerAdapter();
                pager.setAdapter(pagerAdapter);
            } else {
                pagerAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onTalkReceive(Message msg) {
        if (msg.obj != null && (msg.obj instanceof ErrorHelpLog)) {
            ErrorHelp errorHelpLog = (ErrorHelp) msg.obj;
            errorList.add(errorHelpLog);
        }
    }

    // ================================= History Error view Pager Adapter ==========================================

    /**
     * 历史故障 Pager Adapter
     */
    private class ErrorPagerAdapter extends PagerAdapter {

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public int getCount() {
            return HistoryErrorHandler.this.errorList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "";
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            LinearLayout view = (LinearLayout) object;
            container.removeView(view);
            viewList.add(new WeakReference<LinearLayout>(view));
        }

        public ErrorHelp getItem(int position) {
            return HistoryErrorHandler.this.errorList.get(position);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            if (pager == null) {
                pager = (ViewPager) container;
            }
            View view = null;
            // 从废弃的里去取 取到则使用 取不到则创建
            if (viewList.size() > 0) {
                if (viewList.get(0) != null) {
                    view = initView(viewList.get(0).get(), position);
                    viewList.remove(0);
                }
            }
            view = initView(null, position);
            pager.addView(view);
            return view;
        }

        private View initView(LinearLayout view, int position) {
            ViewHolder viewHolder = null;
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(HistoryErrorHandler.this.activity);
                view = (LinearLayout) inflater.inflate(R.layout.history_error_view, null);
                viewHolder = new ViewHolder();
                viewHolder.errorCode = (TextView) view.findViewById(R.id.dictionary_error_help_display);
                viewHolder.errorLevel = (TextView) view.findViewById(R.id.dictionary_error_help_level);
                viewHolder.errorName = (TextView) view.findViewById(R.id.dictionary_error_help_name);
                viewHolder.errorReason = (TextView) view.findViewById(R.id.dictionary_error_help_reason);
                viewHolder.errorSolution = (TextView) view.findViewById(R.id.dictionary_error_help_solution);
                view.setTag(viewHolder);
            } else {
                ErrorHelp errorHelp = getItem(position);
                viewHolder = (ViewHolder) view.getTag();
                viewHolder.errorCode.setText(errorHelp.getDisplay());
                viewHolder.errorName.setText(errorHelp.getName());
                viewHolder.errorLevel.setText(errorHelp.getLevel());
                viewHolder.errorReason.setText(errorHelp.getReason());
                viewHolder.errorSolution.setText(errorHelp.getSolution());
            }
            return view;
        }

        private class ViewHolder {
            TextView errorCode;
            TextView errorLevel;
            TextView errorName;
            TextView errorReason;
            TextView errorSolution;
        }
    }

}
