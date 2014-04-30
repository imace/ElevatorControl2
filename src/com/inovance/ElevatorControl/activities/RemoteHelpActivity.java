package com.inovance.ElevatorControl.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.view.MenuItem;
import android.view.View;
import butterknife.InjectView;
import butterknife.Views;
import com.inovance.ElevatorControl.R;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.LinearLayout;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-4.
 * Time: 10:29.
 */

public class RemoteHelpActivity extends Activity {

    private static final String serverHost = "192.168.5.64";

    private static final int serverPort = 5222;

    private XMPPConnection connection;

    @InjectView(R.id.login_view)
    LinearLayout loginView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setTitle(R.string.remote_help_text);
        setContentView(R.layout.activity_remote_help);
        Views.inject(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        new Thread(new XMPPThread()).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (connection != null) {
                    connection.disconnect();
                }
                setResult(RESULT_OK);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Chat Server Message Handler
     */
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    break;
                case 2: {
                    loginView.setVisibility(View.GONE);
                }
                break;
            }
        }

    };

    /**
     * XMPP Server Client Thread
     */
    public class XMPPThread implements Runnable {

        @Override
        public void run() {
            ConnectionConfiguration config = new ConnectionConfiguration(
                    serverHost, serverPort);
            config.setDebuggerEnabled(true);
            try {
                connection = new XMPPConnection(config);
                connection.connect();
                connection.loginAnonymously();
                handler.sendEmptyMessage(2);
            } catch (XMPPException e) {
                handler.sendEmptyMessage(1);
            }
        }

    }

}