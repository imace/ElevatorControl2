package com.kio.ElevatorControl.activities;

import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import butterknife.InjectView;
import butterknife.Views;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.adapters.ChatMessageAdapter;
import com.kio.ElevatorControl.models.ChatMessage;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.Toast;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Presence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-4.
 * Time: 10:29.
 */

public class RemoteHelpActivity extends Activity {

    private static final String serverHost = "192.168.1.102";

    private static final int serverPort = 5222;

    private XMPPConnection connection;

    @InjectView(R.id.login_view)
    LinearLayout loginView;

    @InjectView(R.id.chat_view)
    LinearLayout chatView;

    @InjectView(R.id.chat_list_view)
    ListView chatListView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setTitle(R.string.remote_help_text);
        setContentView(R.layout.activity_remote_help);
        Views.inject(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        if (connectToServer()) {
            if (userLogin("keith", "010120")) {
                loginView.setVisibility(View.GONE);
                chatView.setVisibility(View.VISIBLE);
                getAllGroupsList();
                //updateChatListViewData();
            }
        } else {
            Toast.makeText(this, "无法连接到服务器", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                connection.disconnect();
                setResult(RESULT_OK);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getAllGroupsList(){
        Roster roster = connection.getRoster();
        Collection entries = roster.getUnfiledEntries();
    }

    private void updateChatListViewData() {
        List<ChatMessage> chatMessageList = new ArrayList<ChatMessage>();
        for (int i = 0; i < 10; i++) {
            ChatMessage item = new ChatMessage();
            item.setMessage(i + "Message");
            item.setSend(i % 2 == 0);
            chatMessageList.add(item);
        }
        chatListView.setAdapter(new ChatMessageAdapter(this, chatMessageList));
    }

    /**
     * 连接到服务器
     *
     * @return 是否连接成功
     */
    private boolean connectToServer() {
        ConnectionConfiguration config = new ConnectionConfiguration(
                serverHost, serverPort);
        config.setDebuggerEnabled(true);
        try {
            connection = new XMPPConnection(config);
            connection.connect();
            return true;
        } catch (XMPPException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 用户登录
     *
     * @param userName 用户名
     * @param password 密码
     * @return 是否登录成功
     */
    public boolean userLogin(String userName, String password) {
        try {
            if (connection == null)
                return false;
            connection.loginAnonymously();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}