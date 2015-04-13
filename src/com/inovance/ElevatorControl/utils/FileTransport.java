package com.inovance.elevatorcontrol.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.config.ApplicationConfig;

import net.tsz.afinal.core.AsyncTask;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-6-12.
 * Time: 15:45.
 */
public class FileTransport implements Runnable {

    private static final String TAG = FileTransport.class.getSimpleName();

    private static FileTransport instance = new FileTransport();

    private ProgressDialog transportDialog;

    private Context context;

    private OnFileUploadComplete mUploadListener;

    private OnFileDownloadComplete mDownloadListener;

    private List<String> chatContent = new ArrayList<String>();

    private InputStream uploadInputStream;

    private static final int UpdateProgressMessage = 1;

    private static final int TransportComplete = 2;

    private static final int TransportFailed = 3;

    private static final int ConnectTimeOut = 4;

    private boolean isCanceled = false;

    private static class TransportHandler extends Handler {

        private final WeakReference<FileTransport> mParentClass;

        public TransportHandler(FileTransport parentClass) {
            mParentClass = new WeakReference<FileTransport>(parentClass);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            FileTransport parentClass = mParentClass.get();
            if (parentClass != null) {
                switch (msg.what) {
                    case UpdateProgressMessage:
                        parentClass.transportDialog.setProgress((Integer) msg.obj);
                        break;
                    case TransportComplete:
                        if (parentClass.transportDialog.isShowing()) {
                            parentClass.transportDialog.dismiss();
                        }
                        break;
                    case ConnectTimeOut:
                        if (parentClass.transportDialog.isShowing()) {
                            parentClass.transportDialog.dismiss();
                        }
                        Toast.makeText(parentClass.context, R.string.connect_server_timeout_text, Toast.LENGTH_SHORT).show();
                        break;
                    case TransportFailed:
                        if (parentClass.transportDialog.isShowing()) {
                            parentClass.transportDialog.dismiss();
                        }
                        Toast.makeText(parentClass.context, R.string.chat_message_send_failed_text, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    }

    private TransportHandler transportHandler;

    public interface OnFileUploadComplete {
        void onUploadComplete(String fileName);
    }

    public interface OnFileDownloadComplete {
        void onDownloadComplete(File file, String fileName, String contentType);
    }

    public void setOnFileUploadComplete(OnFileUploadComplete listener) {
        mUploadListener = listener;
    }

    public void setOnFileDownloadComplete(OnFileDownloadComplete listener) {
        mDownloadListener = listener;
    }

    public void removeListener() {

    }

    public static FileTransport getInstance() {
        instance.transportHandler = new TransportHandler(instance);
        return instance;
    }

    private FileTransport() {

    }

    /**
     * 下载文件
     *
     * @param context Context
     * @param url     URL
     */
    public void downloadFile(Context context, String url) {
        this.context = context;
        showTransportDialog();
        isCanceled = false;
        new AsyncDownloadTask().execute(url);
    }

    private void showTransportDialog() {
        transportDialog = new ProgressDialog(context);
        transportDialog.setIndeterminate(false);
        transportDialog.setMax(100);
        transportDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        transportDialog.setProgress(0);
        transportDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                context.getString(R.string.dialog_btn_cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isCanceled = true;
                        dialog.dismiss();
                    }
                });
        transportDialog.show();
        transportDialog.setCancelable(false);
        transportDialog.setCanceledOnTouchOutside(false);

    }

    /**
     * 上传文件
     *
     * @param context     Context
     * @param from        From
     * @param to          To
     * @param type        Type
     * @param title       Title
     * @param extension   Extension
     * @param fileName    File name
     * @param inputStream InputStream
     */
    public void uploadFile(Context context, String from, String to,
                           int type, String title, String extension,
                           String fileName, InputStream inputStream) {
        chatContent.clear();
        chatContent.add(from);
        chatContent.add(to);
        chatContent.add(String.valueOf(type));
        chatContent.add(title);
        chatContent.add(extension);
        chatContent.add(fileName);
        uploadInputStream = inputStream;
        this.context = context;
        showTransportDialog();
        isCanceled = false;
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            String end = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****++++++************++++++++++++";
            URL url = new URL(ApplicationConfig.UploadMessageAttachments);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(2000);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());

            dataOutputStream.writeBytes(twoHyphens + boundary + end);
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"txtFrom\"" + end + end + chatContent.get(0) + end);
            dataOutputStream.writeBytes(twoHyphens + boundary + end);
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"txtTo\"" + end + end + chatContent.get(1) + end);
            dataOutputStream.writeBytes(twoHyphens + boundary + end);
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"txtType\"" + end + end + chatContent.get(2) + end);
            dataOutputStream.writeBytes(twoHyphens + boundary + end);
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"txtTile\"" + end + end + URLEncoder.encode(chatContent.get(3), "UTF-8") + end);
            dataOutputStream.writeBytes(twoHyphens + boundary + end);
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"extension\"" + end + end + chatContent.get(4) + end);
            dataOutputStream.writeBytes(twoHyphens + boundary + end);
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"FileUpload1\";filename=\"" + URLEncoder.encode(chatContent.get(5), "UTF-8") + "\"" + end);
            dataOutputStream.writeBytes(end);

            int totalLength = uploadInputStream.available();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int uploaded = 0;
            int length = -1;
            while ((length = uploadInputStream.read(buffer)) != -1) {
                if (isCanceled) {
                    dataOutputStream.flush();
                    dataOutputStream.close();
                    break;
                }
                dataOutputStream.write(buffer, 0, length);
                uploaded += length;
                Message message = new Message();
                message.what = UpdateProgressMessage;
                int percent = (uploaded * 100) / totalLength;
                // Wait server process file
                if (percent == 100) {
                    percent = 99;
                }
                message.obj = percent;
                transportHandler.sendMessage(message);
            }
            if (!isCanceled) {
                dataOutputStream.writeBytes(end);
                dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + end);
                uploadInputStream.close();
                dataOutputStream.flush();
                dataOutputStream.close();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    Message message = new Message();
                    message.what = UpdateProgressMessage;
                    message.obj = 100;
                    transportHandler.sendMessage(message);
                    if (mUploadListener != null) {
                        String fileName = chatContent.get(0)
                                + "-" + chatContent.get(1)
                                + "-" + chatContent.get(2)
                                + "-" + chatContent.get(3)
                                + "." + chatContent.get(4);
                        mUploadListener.onUploadComplete(fileName);
                    }
                    transportHandler.sendEmptyMessage(TransportComplete);
                } else {
                    transportHandler.sendEmptyMessage(TransportFailed);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (SocketTimeoutException e) {
            transportHandler.sendEmptyMessage(ConnectTimeOut);
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 异步下载文件
     */
    private class AsyncDownloadTask extends AsyncTask<String, Integer, String[]> {

        @Override
        protected String[] doInBackground(String... params) {
            int count;
            try {
                URL url = new URL(params[0]);
                URLConnection connection = url.openConnection();
                connection.connect();
                String raw = connection.getHeaderField("Content-Disposition");
                String contentType = connection.getContentType();
                String fileName;
                if (raw != null && raw.contains("=")) {
                    fileName = raw.split("=")[1];
                } else {
                    String fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType);
                    fileName = System.currentTimeMillis() + fileExtension;
                }
                int contentLength = connection.getContentLength();
                InputStream inputStream = new BufferedInputStream(url.openStream());
                File directory = new File(context.getFilesDir().getPath()
                        + "/"
                        + ApplicationConfig.ReceiveFileFolder);
                if (!directory.exists()) {
                    directory.mkdir();
                }
                File filePath = new File(context.getFilesDir().getPath()
                        + "/"
                        + ApplicationConfig.ReceiveFileFolder
                        + "/" + fileName);
                if (!filePath.exists()) {
                    filePath.createNewFile();
                }
                OutputStream output = new FileOutputStream(filePath);
                byte data[] = new byte[1024];
                long total = 0;
                while ((count = inputStream.read(data)) != -1) {
                    if (isCanceled) {
                        output.flush();
                        output.close();
                        break;
                    }
                    total += count;
                    int progress = (int) (total * 100) / contentLength;
                    publishProgress(progress);
                    output.write(data, 0, count);
                }
                if (!isCanceled) {
                    publishProgress(100);
                    output.flush();
                    output.close();
                    inputStream.close();
                    transportHandler.sendEmptyMessage(TransportComplete);
                }
                if (isCanceled) {
                    return null;
                } else {
                    return new String[]{filePath.getAbsolutePath(), fileName, contentType};
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            transportDialog.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                if (mDownloadListener != null) {
                    mDownloadListener.onDownloadComplete(new File(result[0]), result[1], result[2]);
                }
            }
        }
    }

}
