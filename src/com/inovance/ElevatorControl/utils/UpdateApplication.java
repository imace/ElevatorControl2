package com.inovance.ElevatorControl.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.bluetoothtool.BluetoothTool;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.web.WebApi;
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

    private static UpdateApplication instance = new UpdateApplication();

    private Activity activity;

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

    private LinearLayout progressView;

    public static UpdateApplication getInstance() {
        return instance;
    }

    private UpdateApplication() {

    }

    public void init(Activity activity) {
        this.activity = activity;
    }

    /**
     * 检查网络连接是否可用
     *
     * @param context Context
     * @return Available Status
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager connectivity = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
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
        if (activity != null) {
            if (isNetworkAvailable()) {
                WebApi.getInstance().setOnResultListener(new WebApi.OnGetResultListener() {
                    @Override
                    public void onResult(String tag, String responseString) {
                        // TODO Check Update
                        if (true) {
                            confirmUpdateApplication("");
                        } else {
                            if (mListener != null) {
                                mListener.onNoUpdate();
                            }
                        }
                    }
                });
                WebApi.getInstance().getLastSoftwareVersion(activity);
            } else {
                new AlertDialog.Builder(activity, R.style.CustomDialogStyle)
                        .setTitle(R.string.no_network_title)
                        .setMessage(R.string.setting_network_message)
                        .setNegativeButton(R.string.exit_application_text, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                WebApi.getInstance().removeListener();
                                BluetoothTool.getInstance(activity).kill();
                                activity.finish();
                            }
                        })
                        .setNeutralButton(R.string.setting_wireless_text, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
                                activity.startActivity(intent);
                            }
                        })
                        .setPositiveButton(R.string.setting_wifi_text, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                                activity.startActivity(intent);
                            }
                        })
                        .create()
                        .show();
            }
        }
    }

    /**
     * 弹出确认框确认更新
     */
    public void confirmUpdateApplication(final String url) {
        if (activity != null) {
            View dialogView = activity.getLayoutInflater().inflate(R.layout.update_application_dialog, null);
            confirmTextView = (TextView) dialogView.findViewById(R.id.confirm_text);
            progressView = (LinearLayout) dialogView.findViewById(R.id.progress_view);
            downloadProgressBar = (ProgressBar) dialogView.findViewById(R.id.download_progress);
            currentLengthTextView = (TextView) dialogView.findViewById(R.id.current_length);
            totalLengthTextView = (TextView) dialogView.findViewById(R.id.total_length);
            AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.CustomDialogStyle)
                    .setView(dialogView)
                    .setTitle(R.string.confirm_update_title)
                    .setNegativeButton(R.string.exit_application_text, null)
                    .setPositiveButton(R.string.update_application_text, null);
            AlertDialog dialog = builder.create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    WebApi.getInstance().removeListener();
                    BluetoothTool.getInstance(activity).kill();
                    activity.finish();
                }
            });
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    confirmTextView.setVisibility(View.GONE);
                    progressView.setVisibility(View.VISIBLE);
                    new DownloadTask().execute(url);
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
                        totalLengthTextView.setText(String.valueOf(length));
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
                File fileName = new File(activity.getExternalCacheDir().getPath() + "/update.apk");
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
            currentLengthTextView.setText(String.valueOf(values[0]));
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                File file = new File(activity.getExternalCacheDir().getPath() + "/update.apk");
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            }
        }
    }
}
