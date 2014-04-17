package com.kio.ElevatorControl.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import butterknife.InjectView;
import butterknife.Views;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.adapters.ShortcutListViewAdapter;
import com.kio.ElevatorControl.daos.ShortcutDao;
import com.kio.ElevatorControl.models.Shortcut;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.ListView;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-4-3.
 * Time: 11:28.
 */
public class ShortcutSettingActivity extends Activity {

    @InjectView(R.id.list_view)
    ListView listView;

    private boolean isDialogShown = false;

    private List<Shortcut> shortcutList;

    private ShortcutListViewAdapter listViewAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setContentView(R.layout.activity_shortcut_setting_layout);
        setTitle(R.string.shortcut_setting_text);
        Views.inject(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        shortcutList = ShortcutDao.findAll(this);
        listViewAdapter = new ShortcutListViewAdapter(this, shortcutList);
        listView.setAdapter(listViewAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                final int index = position;
                AlertDialog.Builder builder = new AlertDialog.Builder(ShortcutSettingActivity.this,
                        R.style.CustomDialogStyle)
                        .setTitle(R.string.shortcut_item_operation_title_text)
                        .setItems(R.array.operation_item, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                switch (i) {
                                    case 0:
                                        showAddShortcutDialog(true, index);
                                        break;
                                    case 1: {
                                        showDeleteItemConfirmDialog(index);
                                    }
                                    break;
                                }
                            }
                        });
                builder.create().show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.shortcut_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                if (!isDialogShown) {
                    showAddShortcutDialog(false, 0);
                }
                return true;
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Show Delete Item Confirm Dialog
     *
     * @param index index
     */
    private void showDeleteItemConfirmDialog(final int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogStyle)
                .setTitle(R.string.confirm_delete_title_text)
                .setMessage(R.string.confirm_delete_message_text);
        builder.setNegativeButton(R.string.dialog_btn_cancel, null);
        builder.setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ShortcutDao.deleteItem(ShortcutSettingActivity.this,
                        shortcutList.get(index));
                shortcutList.remove(index);
                listViewAdapter.setShortcutList(shortcutList);
            }
        });
        builder.create().show();
    }

    private void showAddShortcutDialog(final boolean edit, final int index) {
        View dialogView = getLayoutInflater().inflate(R.layout.add_shortcut_dialog, null);
        final EditText shortcutName = (EditText) dialogView.findViewById(R.id.shortcut_name);
        final EditText shortcutCommand = (EditText) dialogView.findViewById(R.id.shortcut_command);
        if (edit) {
            Shortcut shortcut = shortcutList.get(index);
            shortcutName.setText(shortcut.getName());
            shortcutCommand.setText(shortcut.getCommand());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogStyle)
                .setView(dialogView)
                .setTitle(R.string.add_shortcut_dialog_title_text);
        builder.setNegativeButton(R.string.dialog_btn_cancel, null);
        builder.setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Shortcut shortcut = edit ? shortcutList.get(index) : new Shortcut();
                shortcut.setName(shortcutName.getText().toString());
                shortcut.setCommand(shortcutCommand.getText().toString());
                if (!edit) {
                    ShortcutDao.saveItem(ShortcutSettingActivity.this, shortcut);
                    shortcutList.add(shortcut);
                    listViewAdapter.setShortcutList(shortcutList);
                } else {
                    ShortcutDao.updateItem(ShortcutSettingActivity.this, shortcut);
                    shortcutList.set(index, shortcut);
                    listViewAdapter.setShortcutList(shortcutList);
                }
                isDialogShown = false;
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}