package com.inovance.elevatorcontrol.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import com.inovance.bluetoothtool.BluetoothTool;
import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.config.ApplicationConfig;
import com.inovance.elevatorcontrol.web.WebInterface;

import net.tsz.afinal.core.AsyncTask;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;

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

    private NotificationManager mNotificationManager;

    private Notification.Builder mBuilder;

    int notificationID = 1;

    public static interface OnNoUpdateFoundListener {
        void onNoUpdate();
    }

    private OnNoUpdateFoundListener mListener;

    public void setOnNoUpdateFoundListener(OnNoUpdateFoundListener listener) {
        this.mListener = listener;
    }

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
     * 更新提示对话框
     */
    private AlertDialog updateDialog;

    /**
     * Is no network dialog showing
     */
    private boolean isDialogShowing = false;

    private boolean isDownloading = false;

    private int currentProgress;

    public static UpdateApplication getInstance() {
        return instance;
    }

    private UpdateApplication() {

    }

    public void init(Context context) {
        this.mContext = context;
        mContext.registerReceiver(getBroadcastReceiver(), getIntentFilter());
        try {
            currentVersionName = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setCurrentActivity(Activity activity) {
        this.mActivity = activity;
    }


    /**
     * 检查是否有新版本
     */
    public void checkUpdate() {
        if (mActivity != null) {
            if (WebInterface.isNetworkAvailable(mContext)) {
                startCheckRemoteLastVersion();
            } else {
                showNoNetworkDialog();
            }
        }
    }

    private void startCheckRemoteLastVersion() {
        WebInterface.getInstance().setOnRequestListener(new WebInterface.OnRequestListener() {
            @Override
            public void onResult(String tag, String responseString) {
                boolean needUpdate = false;
                if (responseString != null && responseString.length() > 0) {
                    String[] lastVersion = responseString.split(Pattern.quote("."));
                    String[] currentVersion = currentVersionName.split(Pattern.quote("."));
                    if (lastVersion.length == 3 && currentVersion.length == 3) {
                        int lastMainVersion = Integer.parseInt(lastVersion[0]);
                        int lastSecondVersion = Integer.parseInt(lastVersion[1]);
                        int lastBuildVersion = Integer.parseInt(lastVersion[2]);

                        int currentMainVersion = Integer.parseInt(currentVersion[0]);
                        int currentSecondVersion = Integer.parseInt(currentVersion[1]);
                        int currentBuildVersion = Integer.parseInt(currentVersion[2]);

                        if (lastMainVersion > currentMainVersion) {
                            needUpdate = true;
                        }
                        if (lastSecondVersion > currentSecondVersion) {
                            needUpdate = true;
                        }
                        if (lastBuildVersion > currentBuildVersion) {
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

            @Override
            public void onFailure(int statusCode, Throwable throwable) {
                Toast.makeText(mActivity, R.string.server_error_text, Toast.LENGTH_SHORT).show();
            }
        });
        WebInterface.getInstance().getLastSoftwareVersion(mActivity);
    }

    private void showNoNetworkDialog() {
        if (noNetworkDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.GlobalDialogStyle)
                    .setTitle(R.string.no_network_title)
                    .setMessage(R.string.setting_network_message)
                    .setNegativeButton(R.string.exit_application_text, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            WebInterface.getInstance().removeListener();
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
        if (updateDialog == null && !isDownloading) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                    .setTitle(R.string.confirm_update_title)
                    .setMessage(mActivity.getResources().getString(R.string.last_version_text) + lastVersionName)
                    .setNegativeButton(R.string.exit_application_text, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int id) {
                            WebInterface.getInstance().removeListener();
                            BluetoothTool.getInstance().kill();
                            if (mActivity != null) {
                                mActivity.finish();
                            }
                        }
                    })
                    .setPositiveButton(R.string.update_application_text, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int id) {
                            mNotificationManager = (NotificationManager) mContext.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                            mBuilder = new Notification.Builder(mContext.getApplicationContext());
                            mBuilder.setContentTitle(mContext.getResources().getString(R.string.download_application_package_title));
                            mBuilder.setContentText(mContext.getResources().getString(R.string.download_application_package_message));
                            mBuilder.setSmallIcon(android.R.drawable.stat_sys_download);
                            mBuilder.setAutoCancel(true);
                            isDownloading = true;
                            currentProgress = 0;
                            new DownloadTask().execute(ApplicationConfig.APIUri + ApplicationConfig.DownloadApplicationFile);
                        }
                    });
            updateDialog = builder.create();
            updateDialog.show();
            updateDialog.setCancelable(false);
            updateDialog.setCanceledOnTouchOutside(false);
            updateDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    updateDialog = null;
                }
            });
        }
    }

    private void updateProgress(int total, int progress) {
        mBuilder.setProgress(total, progress, false);
        if (Build.VERSION.SDK_INT < 16) {
            mNotificationManager.notify(notificationID, mBuilder.getNotification());
        } else {
            mNotificationManager.notify(notificationID, mBuilder.build());
        }
    }

    /**
     * 下载安装包
     */
    private class DownloadTask extends AsyncTask<String, Long, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            int count;
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                try {
                    URL url = new URL(params[0]);
                    URLConnection connection = url.openConnection();
                    connection.connect();
                    long contentLength = connection.getContentLength();
                    updateProgress(100, 0);
                    InputStream inputStream = new BufferedInputStream(url.openStream());
                    // Download apk file to external storage to avoid permission problem
                    File fileName = new File(mActivity.getExternalCacheDir().getPath() + "/package.apk");
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
                        publishProgress(contentLength, total);
                        // writing data to file
                        output.write(data, 0, count);
                    }
                    // flushing output
                    output.flush();
                    // closing streams
                    output.close();
                    inputStream.close();
                    return true;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return false;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            return false;
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            float progress = values[1].floatValue() / values[0].floatValue();
            if (progress < 0.2 && progress > 0) {
                if (currentProgress != 20) {
                    currentProgress = 20;
                    updateProgress(100, currentProgress);
                }
            }
            if (progress < 0.4 && progress >= 0.2) {
                if (currentProgress != 40) {
                    currentProgress = 40;
                    updateProgress(100, currentProgress);
                }
            }
            if (progress < 0.8 && progress >= 0.4) {
                if (currentProgress != 80) {
                    currentProgress = 80;
                    updateProgress(100, currentProgress);
                }
            }
            if (progress < 1.0 && progress >= 0.8) {
                if (currentProgress != 90) {
                    currentProgress = 90;
                    updateProgress(100, currentProgress);
                }
            }
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                updateProgress(0, 0);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                File file = new File(mActivity.getExternalCacheDir().getPath() + "/package.apk");
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mActivity.startActivity(intent);
                isDownloading = false;
                currentProgress = 0;
                mNotificationManager.cancel(notificationID);
            } else {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mActivity, R.string.update_application_failed, Toast.LENGTH_SHORT).show();
                    }
                });
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
                if (WebInterface.isNetworkAvailable(context)) {
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
