package com.kio.ElevatorControl.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import butterknife.InjectView;
import butterknife.Views;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.models.Profile;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.widget.Button;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.ProgressBar;
import org.holoeverywhere.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-10.
 * Time: 11:35.
 */
public class ParameterUploadActivity extends Activity {

    private final static String DIRECTORY_NAME = "Profile";

    private static final String TAG = ParameterUploadActivity.class.getSimpleName();

    @InjectView(R.id.upload_list)
    ListView listView;

    private LocalProfileAdapter adapter;

    private ProgressBar uploadProgressBar;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.parameter_upload_text);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_parameter_upload);
        Views.inject(this);
        List<Profile> lists = new ArrayList<Profile>();
        Profile profile = new Profile();
        profile.setVersion("1.2");
        profile.setUpdateDate("2014-5-12");
        lists.add(profile);
        adapter = new LocalProfileAdapter(lists);
        listView.setAdapter(adapter);
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
     * 上传参数
     *
     * @param position ListView index
     */
    private void onUploadButtonClick(int position) {
        Profile profile = adapter.getItem(position);
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File directory = new File(getApplicationContext().getExternalCacheDir().getPath()
                    + "/"
                    + DIRECTORY_NAME);
            File file = new File(directory, "Profile_001.json");
            try {
                InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file));
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();
                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }
                bufferedReader.close();
                inputStreamReader.close();
                JSONArray groups = new JSONArray(stringBuilder.toString());
                // 开始上传参数
                ParameterUploadActivity.this.startUploadProfile(groups);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(ParameterUploadActivity.this);
        org.holoeverywhere.LayoutInflater inflater = ParameterUploadActivity.this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.parameters_duplicate_dialog, null);
        uploadProgressBar = (ProgressBar) dialogView.findViewById(R.id.progress_bar);
        uploadProgressBar.setVisibility(View.VISIBLE);
        dialogView.findViewById(R.id.file_name).setVisibility(View.GONE);
        builder.setTitle(R.string.uploading_profile_text);
        builder.setView(dialogView);
        builder.setNegativeButton(R.string.dialog_btn_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    /**
     * 开始写入参数
     *
     * @param groups JSONArray
     */
    private void startUploadProfile(JSONArray groups) {
        int size = groups.length();
        for (int i = 0; i < size; i++) {
            try {
                JSONObject groupsJSONObject = groups.getJSONObject(i);
                JSONArray detailParameters = groupsJSONObject.getJSONArray("parameterSettings");
                int length = detailParameters.length();
                for (int j = 0; j < length; j++) {
                    JSONObject jsonObject = detailParameters.getJSONObject(j);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // ========================= Local Profile Adapter =====================================

    /**
     * 本地配置文件Adapter
     */
    private class LocalProfileAdapter extends BaseAdapter {

        private List<Profile> profileLists;

        public LocalProfileAdapter(List<Profile> lists) {
            profileLists = lists;
        }

        @Override
        public int getCount() {
            return profileLists.size();
        }

        @Override
        public Profile getItem(int i) {
            return profileLists.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            ViewHolder holder = null;
            LayoutInflater mInflater = LayoutInflater.from(ParameterUploadActivity.this);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.parameter_upload_item, null);
                holder = new ViewHolder();
                holder.profileVersion = (TextView) convertView.findViewById(R.id.profile_version);
                holder.profileUpdateDate = (TextView) convertView.findViewById(R.id.profile_update_date);
                holder.uploadButton = (Button) convertView.findViewById(R.id.upload_button);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Profile profile = getItem(position);
            holder.profileVersion.setText(profile.getVersion());
            holder.profileUpdateDate.setText(profile.getUpdateDate());
            final int index = position;
            holder.uploadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 上传对应的参数
                    ParameterUploadActivity.this.onUploadButtonClick(index);
                }
            });
            return convertView;
        }

        /**
         * View Holder
         */
        private class ViewHolder {
            TextView profileVersion;
            TextView profileUpdateDate;
            Button uploadButton;
        }

    }

}