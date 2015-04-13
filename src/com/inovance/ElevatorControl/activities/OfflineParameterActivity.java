package com.inovance.elevatorcontrol.activities;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.inovance.bluetoothtool.BuildConfig;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.daos.DeviceDao;
import com.inovance.elevatorcontrol.models.Device;
import com.inovance.elevatorcontrol.web.WebInterface;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import butterknife.InjectView;
import butterknife.Views;

// TODO Get normal device and user special device list.
// TODO Download available device function code, state code, error code.

public class OfflineParameterActivity extends Activity implements WebInterface.OnRequestListener {

    private static final String TAG = OfflineParameterActivity.class.getSimpleName();

    @InjectView(R.id.list_view)
    ListView listView;

    private String[] deviceTypes;

    private List<Device> localDeviceList;

    private DownloadManager downloadManager;

    private IntentFilter downloadCompleteIntentFilter;

    private DownloadCompleteReceiver downloadCompleteReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setTitle(R.string.offline_parameter_title);
        setContentView(R.layout.activity_offline_parameter);
        Views.inject(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        localDeviceList = DeviceDao.findAll(this);

        deviceTypes = new String[]{"NICE 1000", "NICE 1000+", "NICE 3000", "NICE 3000+"};

        ParameterAdapter adapter = new ParameterAdapter();
        listView.setAdapter(adapter);

        // Get DownloadManager instance
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        downloadCompleteReceiver = new DownloadCompleteReceiver();

        downloadCompleteIntentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

        addDownloadQueue("https://www.baidu.com/");
    }

    @Override
    protected void onResume() {
        super.onResume();
        WebInterface.getInstance().setOnRequestListener(this);
        registerReceiver(downloadCompleteReceiver, downloadCompleteIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        WebInterface.getInstance().removeListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(downloadCompleteReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Add download queue to DownloadManager
     *
     * @param url Request URL String
     */
    private long addDownloadQueue(String url) {
        if (Environment.isExternalStorageEmulated()) {
            File path = new File(getExternalCacheDir().toString() + "/offline");
            if (!path.exists()) {
                path.mkdir();
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setAllowedOverRoaming(true);
            request.setVisibleInDownloadsUi(false);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);

            Uri destinationUri = Uri.parse("file://" + getExternalCacheDir().toString() + "/offline/" + MD5(url) + ".xml");
            request.setDestinationUri(destinationUri);

            return downloadManager.enqueue(request);
        }
        return -1;
    }

    private static String MD5(String string) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(string.getBytes("UTF-8"));
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            return bigInt.toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError();
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError();
        }
    }

    @Override
    public void onResult(String tag, String responseString) {

    }

    @Override
    public void onFailure(int statusCode, Throwable throwable) {

    }

    private class ParameterAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return deviceTypes.length;
        }

        @Override
        public String getItem(int position) {
            return deviceTypes[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            LayoutInflater mInflater = getLayoutInflater();
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.offline_parameter_item, null);
                holder = new ViewHolder();
                holder.deviceName = (TextView) convertView.findViewById(R.id.device_name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.deviceName.setText(getItem(position));
            return convertView;
        }

        class ViewHolder {
            TextView deviceName;
        }
    }

    private class DownloadCompleteReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
            if (BuildConfig.DEBUG) {
                Log.v(TAG, id + "");
            }
        }
    }
}