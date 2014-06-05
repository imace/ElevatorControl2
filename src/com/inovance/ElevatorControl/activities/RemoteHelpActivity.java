package com.inovance.ElevatorControl.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import butterknife.InjectView;
import butterknife.Views;
import com.inovance.ElevatorControl.R;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.LinearLayout;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.ReportedData;
import org.jivesoftware.smackx.ReportedData.Row;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.packet.*;
import org.jivesoftware.smackx.provider.*;
import org.jivesoftware.smackx.search.UserSearch;
import org.jivesoftware.smackx.search.UserSearchManager;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-4.
 * Time: 10:29.
 */

public class RemoteHelpActivity extends Activity implements Runnable {

    private static final String TAG = RemoteHelpActivity.class.getSimpleName();

    private static final String serverHost = "192.168.5.64";

    private static final int serverPort = 5222;

    private XMPPConnection connection;

    @InjectView(R.id.login_view)
    LinearLayout loginView;

    private static final int WILL_CONNECT = 1;

    private static final int CONNECTED = 2;

    private static final int CONNECT_FAILED = 3;

    private static final int WILL_CHECK_USER_EXIST = 4;

    private static final int WILL_CREATE_ACCOUNT = 5;

    private int currentTask = -1;

    private ExecutorService pool = Executors.newSingleThreadExecutor();

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
        configure(ProviderManager.getInstance());
        currentTask = WILL_CONNECT;
        pool.execute(RemoteHelpActivity.this);
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

    @Override
    public void run() {
        switch (currentTask) {
            case WILL_CONNECT:
                createConnection();
                break;
            case WILL_CHECK_USER_EXIST:
                if (checkUserExist("Demo")) {
                    Log.v(TAG, "TRUE");
                } else {
                    currentTask = WILL_CREATE_ACCOUNT;
                    pool.execute(RemoteHelpActivity.this);
                }
                break;
            case WILL_CREATE_ACCOUNT:
                tryToCreateUser();
                break;
        }
    }

    private void createConnection() {
        ConnectionConfiguration config = new ConnectionConfiguration(
                serverHost, serverPort);
        config.setDebuggerEnabled(true);
        try {
            connection = new XMPPConnection(config);
            connection.connect();
            handler.sendEmptyMessage(CONNECTED);
            currentTask = WILL_CHECK_USER_EXIST;
            pool.execute(RemoteHelpActivity.this);
        } catch (XMPPException e) {
            handler.sendEmptyMessage(CONNECT_FAILED);
        }
    }

    private boolean checkUserExist(String userName) {
        UserSearchManager userSearchManager = new UserSearchManager(connection);
        try {
            Log.v(TAG, connection.getServiceName());
            Form searchForm = userSearchManager.getSearchForm("search." + connection.getServiceName());
            Log.v(TAG, searchForm.toString());
            Form answerForm = searchForm.createAnswerForm();
            answerForm.setAnswer("Username", true);
            answerForm.setAnswer("search", userName);
            ReportedData reportedData = userSearchManager.getSearchResults(answerForm, "search."
                    + connection.getServiceName());
            Log.v(TAG, "0001");
            if (reportedData.getRows() != null) {
                Log.v(TAG, "0002");
                Iterator<Row> it = reportedData.getRows();
                while (it.hasNext()) {
                    Row row = it.next();
                    Iterator iterator = row.getValues("jid");
                    if (iterator.hasNext()) {
                        String value = iterator.next().toString();
                        if (value.equalsIgnoreCase(userName)) {
                            Log.v(TAG, value);
                            return true;
                        }
                    }
                }
            }
        } catch (XMPPException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void tryToCreateUser() {
        AccountManager accountManager = new AccountManager(connection);
        try {
            accountManager.createAccount("Demo", "1234");
        } catch (XMPPException e) {
            Log.v(TAG, e.getMessage());
        }
    }

    private void loginUser() {

    }

    public void configure(ProviderManager pm) {
        //  Private Data Storage
        pm.addIQProvider("query", "jabber:iq:private", new PrivateDataManager.PrivateDataIQProvider());

        //  Time
        try {
            pm.addIQProvider("query", "jabber:iq:time", Class.forName("org.jivesoftware.smackx.packet.Time"));
        } catch (ClassNotFoundException e) {
            Log.w("TestClient", "Can't load class for org.jivesoftware.smackx.packet.Time");
        }

        //  Roster Exchange
        pm.addExtensionProvider("x", "jabber:x:roster", new RosterExchangeProvider());

        //  Message Events
        pm.addExtensionProvider("x", "jabber:x:event", new MessageEventProvider());

        //  Chat State
        pm.addExtensionProvider("active", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("composing", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("paused", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("inactive", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("gone", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());

        //  XHTML
        pm.addExtensionProvider("html", "http://jabber.org/protocol/xhtml-im", new XHTMLExtensionProvider());

        //  Group Chat Invitations
        pm.addExtensionProvider("x", "jabber:x:conference", new GroupChatInvitation.Provider());

        //  Service Discovery # Items
        pm.addIQProvider("query", "http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());

        //  Service Discovery # Info
        pm.addIQProvider("query", "http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());

        //  Data Forms
        pm.addExtensionProvider("x", "jabber:x:data", new DataFormProvider());

        //  MUC User
        pm.addExtensionProvider("x", "http://jabber.org/protocol/muc#user", new MUCUserProvider());

        //  MUC Admin
        pm.addIQProvider("query", "http://jabber.org/protocol/muc#admin", new MUCAdminProvider());

        //  MUC Owner
        pm.addIQProvider("query", "http://jabber.org/protocol/muc#owner", new MUCOwnerProvider());

        //  Delayed Delivery
        pm.addExtensionProvider("x", "jabber:x:delay", new DelayInformationProvider());

        //  Version
        try {
            pm.addIQProvider("query", "jabber:iq:version", Class.forName("org.jivesoftware.smackx.packet.Version"));
        } catch (ClassNotFoundException e) {
            //  Not sure what's happening here.
        }

        //  VCard
        pm.addIQProvider("vCard", "vcard-temp", new VCardProvider());

        //  Offline Message Requests
        pm.addIQProvider("offline", "http://jabber.org/protocol/offline", new OfflineMessageRequest.Provider());

        //  Offline Message Indicator
        pm.addExtensionProvider("offline", "http://jabber.org/protocol/offline", new OfflineMessageInfo.Provider());

        //  Last Activity
        pm.addIQProvider("query", "jabber:iq:last", new LastActivity.Provider());

        //  User Search
        pm.addIQProvider("query", "jabber:iq:search", new UserSearch.Provider());

        //  SharedGroupsInfo
        pm.addIQProvider("sharedgroup", "http://www.jivesoftware.org/protocol/sharedgroup", new SharedGroupsInfo.Provider());

        //  JEP-33: Extended Stanza Addressing
        pm.addExtensionProvider("addresses", "http://jabber.org/protocol/address", new MultipleAddressesProvider());

        //   FileTransfer
        pm.addIQProvider("si", "http://jabber.org/protocol/si", new StreamInitiationProvider());

        pm.addIQProvider("query", "http://jabber.org/protocol/bytestreams", new BytestreamsProvider());

        //  Privacy
        pm.addIQProvider("query", "jabber:iq:privacy", new PrivacyProvider());
        pm.addIQProvider("command", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider());
        pm.addExtensionProvider("malformed-action", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.MalformedActionError());
        pm.addExtensionProvider("bad-locale", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadLocaleError());
        pm.addExtensionProvider("bad-payload", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadPayloadError());
        pm.addExtensionProvider("bad-sessionid", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadSessionIDError());
        pm.addExtensionProvider("session-expired", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.SessionExpiredError());
    }

    /**
     * Chat Server Message Handler
     */
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONNECTED: {
                    loginView.setVisibility(View.GONE);
                }
                break;
                case CONNECT_FAILED:
                    break;
            }
        }

    };

}