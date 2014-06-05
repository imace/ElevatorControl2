package com.inovance.ElevatorControl.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.views.dialogs.CustomDialog;
import com.inovance.ElevatorControl.web.WebApi;
import net.tsz.afinal.core.AsyncTask;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.ProgressBar;

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
     * 检查是否有新版本
     */
    public void checkUpdate() {
        if (activity != null) {
            WebApi.getInstance().setOnResultListener(new WebApi.onGetResultListener() {
                @Override
                public void onResult(String tag, String responseString) {
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
                    .setNegativeButton(R.string.dialog_btn_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            CustomDialog.exitDialog(activity);
                        }
                    })
                    .setPositiveButton(R.string.dialog_btn_ok, null);
            AlertDialog dialog = builder.create();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    confirmTextView.setVisibility(View.GONE);
                    progressView.setVisibility(View.VISIBLE);
                    new DownloadTask().execute(url);
                }
            });
            dialog.show();
        }
    }

    /**
     * 下载安装包
     */
    private class DownloadTask extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            int count;
            try {
                URL url = new URL(params[0]);
                URLConnection connection = url.openConnection();
                connection.connect();
                int contentLength = connection.getContentLength();
                downloadProgressBar.setMax(contentLength);
                totalLengthTextView.setText(String.valueOf(contentLength));
                InputStream inputStream = new BufferedInputStream(url.openStream(), 8192);
                File fileName = new File(activity.getExternalCacheDir().getPath() + "/APK/update.apk");
                OutputStream output = new FileOutputStream(fileName);
                byte data[] = new byte[1024];
                int total = 0;
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
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            downloadProgressBar.setProgress(values[0]);
            currentLengthTextView.setText(String.valueOf(values[0]));
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                File file = new File(activity.getExternalCacheDir().getPath() + "/APK/update.apk");
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            }
        }
    }
}
