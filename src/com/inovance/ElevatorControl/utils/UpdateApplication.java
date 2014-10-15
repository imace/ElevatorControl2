package com.inovance.elevatorcontrol.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.inovance.bluetoothtool.BluetoothTool;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.web.WebApi;
import net.tsz.afinal.core.AsyncTask;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by IntelliJ IDEA.
 * 更新软件安装包
 * User: keith.
 * Date: 14-6-3.
 * Time: 14:47.
 */
public class UpdateApplication {

    private static final String TAG = UpdateApplication.class.getSimpleName();

    private static UpdateApplication instance = new UpdateApplication();

    private Context mContext;

    private Activity mActivity;

    public static interface OnNoUpdateFoundListener {
        void onNoUpdate();
    }

    private OnNoUpdateFoundListener mListener;

    public void setOnNoUpdateFoundListener(OnNoUpdateFoundListener listener) {
        this.mListener = listener;
    }

    /**
     * 确认更新提示
     */
    private TextView confirmTextView;

    /**
     * 当前下载的长度
     */
    private TextView currentLengthTextView;

    /**
     * 文件总长度
     */
    private TextView totalLengthTextView;

    /**
     * 下载进度条
     */
    private ProgressBar downloadProgressBar;

    /**
     * 当前版本号
     */
    private String currentVersionName;

    /**
     * 最新版本号
     */
    private String lastVersionName;

    private AlertDialog noNetworkDialog;

    /**
     * Is no network dialog showing
     */
    private boolean isDialogShowing = false;

    private LinearLayout progressView;

    public static UpdateApplication getInstance() {
        return instance;
    }

    private UpdateApplication() {

    }

    public void init(Context context) {
        this.mContext = context;
        mContext.registerReceiver(getBroadcastReceiver(), getIntentFilter());
        try {
            currentVersionName = mContext.getPackageManager()
                    .getPackageInfo(mContext.getPackageName(), 0)
                    .versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setCurrentActivity(Activity activity) {
        this.mActivity = activity;
    }

    /**
     * 检查网络连接是否可用
     *
     * @param context Context
     * @return Available Status
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager connectivity = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (NetworkInfo anInfo : info) {
                    if (anInfo.getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 检查是否有新版本
     */
    public void checkUpdate() {
        if (mActivity != null) {
            if (isNetworkAvailable()) {
                startCheckRemoteLastVersion();
            } else {
                showNoNetworkDialog();
            }
        }
    }

    private void startCheckRemoteLastVersion() {
        WebApi.getInstance().setOnResultListener(new WebApi.OnGetResultListener() {
            @Override
            public void onResult(String tag, String responseString) {
                boolean needUpdate = false;
                if (responseString != null && responseString.length() > 0) {
                    String[] lastVersion = responseString.split(".");
                    String[] currentVersion = currentVersionName.split(".");
                    if (lastVersion.length == 3 && currentVersion.length == 3) {
                        int lastMainVersion = Integer.parseInt(lastVersion[0]);
                        int lastSecondVersion = Integer.parseInt(lastVersion[1]);
                        int lastBuildVersion = Integer.parseInt(lastVersion[2]);
                        int currentMainVersion = Integer.parseInt(currentVersion[0]);
                        int currentSecondVersion = Integer.parseInt(currentVersion[1]);
                        int currentBuildVersion = Integer.parseInt(currentVersion[2]);
                        if (currentMainVersion > lastMainVersion) {
                            needUpdate = true;
                        }
                        if (currentSecondVersion > lastSecondVersion) {
                            needUpdate = true;
                        }
                        if (currentBuildVersion > lastBuildVersion) {
                            needUpdate = true;
                        }
                    }
                }
                if (needUpdate) {
                    lastVersionName = responseString;
                    confirmUpdateApplication();
                } else {
                    if (mListener != null) {
                        mListener.onNoUpdate();
                    }
                }
            }
        });
        WebApi.getInstance().setOnFailureListener(new WebApi.OnRequestFailureListener() {
            @Override
            public void onFailure(int statusCode, Throwable throwable) {
                Toast.makeText(mActivity, R.string.server_error_text, Toast.LENGTH_SHORT).show();
            }
        });
        WebApi.getInstance().getLastSoftwareVersion(mActivity);
    }

    private void showNoNetworkDialog() {
        if (noNetworkDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.CustomDialogStyle)
                    .setTitle(R.string.no_network_title)
                    .setMessage(R.string.setting_network_message)
                    .setNegativeButton(R.string.exit_application_text, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            WebApi.getInstance().removeListener();
                            BluetoothTool.getInstance().kill();
                            if (mActivity != null) {
                                mActivity.finish();
                            }
                        }
                    })
                    .setPositiveButton(R.string.setting_network_text, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(Settings.ACTION_SETTINGS);
                            mActivity.startActivity(intent);
                        }
                    });
            noNetworkDialog = builder.create();
            noNetworkDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    isDialogShowing = true;
                }
            });
            noNetworkDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    isDialogShowing = false;
                }
            });
            noNetworkDialog.show();
            noNetworkDialog.setCancelable(false);
            noNetworkDialog.setCanceledOnTouchOutside(false);
        } else {
            if (noNetworkDialog != null) {
                if (!isDialogShowing) {
                    noNetworkDialog.show();
                }
            }
        }
    }

    /**
     * 弹出确认框确认更新
     */
    public void confirmUpdateApplication() {
        if (mActivity != null) {
            View dialogView = mActivity.getLayoutInflater().inflate(R.layout.update_application_dialog, null);
            confirmTextView = (TextView) dialogView.findViewById(R.id.confirm_text);
            confirmTextView.setText(mActivity.getResources().getString(R.string.last_version_text) + lastVersionName);
            progressView = (LinearLayout) dialogView.findViewById(R.id.progress_view);
            downloadProgressBar = (ProgressBar) dialogView.findViewById(R.id.download_progress);
            currentLengthTextView = (TextView) dialogView.findViewById(R.id.current_length);
            totalLengthTextView = (TextView) dialogView.findViewById(R.id.total_length);
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.CustomDialogStyle)
                    .setView(dialogView)
                    .setTitle(R.string.confirm_update_title)
                    .setNegativeButton(R.string.exit_application_text, null)
                    .setPositiveButton(R.string.update_application_text, null);
            AlertDialog dialog = builder.create();
            dialog.show();
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    WebApi.getInstance().removeListener();
                    BluetoothTool.getInstance().kill();
                    if (mActivity != null) {
                        mActivity.finish();
                    }
                }
            });
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    confirmTextView.setVisibility(View.GONE);
                    progressView.setVisibility(View.VISIBLE);
                    new DownloadTask().execute(ApplicationConfig.APIUri + ApplicationConfig.DownloadApplicationFile);
                }
            });
        }
    }

    private static final int GetContentLength = 1;

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GetContentLength: {
                    if (msg.obj instanceof Integer) {
                        int length = (Integer) msg.obj;
                        downloadProgressBar.setMax(length);
                        totalLengthTextView.setText(ParseSerialsUtils.humanReadableByteCount(length));
                    }
                }
                break;
            }
        }

    };

    /**
     * 下载安装包
     */
    private class DownloadTask extends AsyncTask<String, Long, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            int count;
            try {
                URL url = new URL(params[0]);
                URLConnection connection = url.openConnection();
                connection.connect();
                int contentLength = connection.getContentLength();
                Message message = new Message();
                message.what = GetContentLength;
                message.obj = contentLength;
                handler.sendMessage(message);
                InputStream inputStream = new BufferedInputStream(url.openStream());
                File fileName = new File(mActivity.getExternalCacheDir().getPath() + "/update.apk");
                if (!fileName.exists()) {
                    fileName.createNewFile();
                }
                OutputStream output = new FileOutputStream(fileName);
                byte data[] = new byte[1024];
                long total = 0;
                while ((count = inputStream.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress(total);
                    // writing data to file
                    output.write(data, 0, count);
                }
                // flushing output
                output.flush();
                // closing streams
                output.close();
                inputStream.close();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            downloadProgressBar.setProgress(values[0].intValue());
            currentLengthTextView.setText(ParseSerialsUtils.humanReadableByteCount(values[0]));
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                File file = new File(mActivity.getExternalCacheDir().getPath() + "/update.apk");
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mActivity.startActivity(intent);
            }
        }
    }

    /**
     * Network state change BroadcastReceiver
     *
     * @return BroadcastReceiver
     */
    private BroadcastReceiver getBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isNetworkAvailable()) {
                    if (noNetworkDialog != null && isDialogShowing) {
                        noNetworkDialog.dismiss();
                        checkUpdate();
                    }
                } else {
                    if (noNetworkDialog != null && !isDialogShowing) {
                        noNetworkDialog.show();
                    }
                }
            }
        };
    }

    private IntentFilter getIntentFilter() {
        return new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
    }
}
