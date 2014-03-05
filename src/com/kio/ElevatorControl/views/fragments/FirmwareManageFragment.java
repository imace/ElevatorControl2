package com.kio.ElevatorControl.views.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.daos.MenuValues;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-5.
 * Time: 15:04.
 */
public class FirmwareManageFragment extends Fragment{

    private static final String TAG = FirmwareManageFragment.class.getSimpleName();

    // 当前tabIndex
    private int tabIndex;

    // 该碎片使用的布局的RId
    private int layoutId;

    private Context context;

    private View mContentView;

    public static FirmwareManageFragment newInstance(int tabIndex, Context context) {
        FirmwareManageFragment firmwareManageFragment = new FirmwareManageFragment();
        firmwareManageFragment.tabIndex = tabIndex;
        firmwareManageFragment.context = context;
        firmwareManageFragment.layoutId = MenuValues.getFirmwareManageTabsLayoutId(tabIndex, context);
        if (firmwareManageFragment.layoutId == 0) {
            firmwareManageFragment.layoutId = R.layout.fragment_test;
        }
        return firmwareManageFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        return inflater.inflate(layoutId, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            // 反射执行
            this.getClass().getMethod(MenuValues.getFirmwareManageTabsLoadMethodName(tabIndex, context))
                    .invoke(this);
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

    public void loadFirmwareApplyView(){

    }

    public void loadFirmwareDownloadView(){

    }

    public void loadFirmwareBurnView(){

    }

}
