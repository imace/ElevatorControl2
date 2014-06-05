package com.inovance.ElevatorControl.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import butterknife.InjectView;
import butterknife.Views;
import com.inovance.ElevatorControl.R;
import com.inovance.ElevatorControl.adapters.ShortcutListViewAdapter;
import com.inovance.ElevatorControl.daos.ShortcutDao;
import com.inovance.ElevatorControl.models.Shortcut;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.ListView;
import org.holoeverywhere.widget.Spinner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * 快捷菜单设置
 * User: keith.
 * Date: 14-4-3.
 * Time: 11:28.
 */
public class ShortcutSettingActivity extends Activity {

    @InjectView(R.id.list_view)
    ListView listView;

    private boolean isDialogShown = false;

    private List<Shortcut> shortcutList;

    private String[] troubleAnalyzeTabArray;

    private String[] configurationTabArray;

    private String[] firmwareManageTabArray;

    private ShortcutListViewAdapter listViewAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_open_animation, R.anim.activity_close_animation);
        setContentView(R.layout.activity_shortcut_setting_layout);
        setTitle(R.string.shortcut_setting_text);
        Views.inject(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        troubleAnalyzeTabArray = getResources().getStringArray(R.array.trouble_analyze_tab_text);
        configurationTabArray = getResources().getStringArray(R.array.configuration_tab_text);
        firmwareManageTabArray = getResources().getStringArray(R.array.firmware_manage_tab_text);
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
        final Spinner actionSpinner = (Spinner) dialogView.findViewById(R.id.action_spinner);
        ArrayList<String> tabs = new ArrayList<String>();
        Collections.addAll(tabs, troubleAnalyzeTabArray);
        Collections.addAll(tabs, configurationTabArray);
        Collections.addAll(tabs, firmwareManageTabArray);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item,
                tabs.toArray(new String[tabs.size()]));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actionSpinner.setAdapter(adapter);
        if (edit) {
            Shortcut shortcut = shortcutList.get(index);
            shortcutName.setText(shortcut.getName());
            int spinnerSelectIndex = Integer.parseInt(shortcut.getCommand().split(":")[2]);
            if (spinnerSelectIndex < tabs.size()) {
                actionSpinner.setSelection(spinnerSelectIndex);
            }
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
                int actionIndex = actionSpinner.getSelectedItemPosition();
                String actionRecord = "";
                if (actionIndex >= 0 && actionIndex < troubleAnalyzeTabArray.length) {
                    actionRecord = "0:" + actionIndex + ":" + actionIndex;
                } else if (actionIndex >= troubleAnalyzeTabArray.length
                        && actionIndex < troubleAnalyzeTabArray.length + configurationTabArray.length) {
                    actionRecord = "1:" + (actionIndex - troubleAnalyzeTabArray.length)
                            + ":" + actionIndex;
                } else {
                    actionRecord = "3:"
                            + (actionIndex - troubleAnalyzeTabArray.length - configurationTabArray.length)
                            + ":" + actionIndex;
                }
                shortcut.setCommand(actionRecord);
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