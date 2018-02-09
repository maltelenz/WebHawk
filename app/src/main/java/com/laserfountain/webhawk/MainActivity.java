package com.laserfountain.webhawk;

import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements AddWebsite.NoticeDialogListener{

    private static final int PREFS_UPDATED = 1;
    SwipeRefreshLayout swipeRefreshLayout;

    ArrayList<Website> websites;
    private ListView listView;
    WebsiteAdapter arrayAdapter;

    UpdateService mService;
    private boolean mBound;

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to UpdateService, cast the IBinder and get UpdateService instance
            UpdateService.LocalBinder binder = (UpdateService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            // Load all the websites
            reloadWebsites();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    // Broadcast receiver that gets called whenever there is new data from the service
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Update the list
            reloadWebsites();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView) findViewById(R.id.listView);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setItemsCanFocus(false);
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            ArrayList<Website> removedSites;

            @Override
            public void onItemCheckedStateChanged(ActionMode mode,
                                                  int position, long id, boolean checked) {
                // Capture total checked items
                final int checkedCount = listView.getCheckedItemCount();
                // Set the CAB title according to total checked items
                mode.setTitle(checkedCount + " Selected");
                // Calls toggleSelection method from ListViewAdapter Class
                arrayAdapter.toggleSelection(position);
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                SparseBooleanArray selected = arrayAdapter.getSelectedIds();
                switch (item.getItemId()) {
                    case R.id.action_refresh:
                        for (int i = (selected.size() - 1); i >= 0; i--) {
                            if (selected.valueAt(i)) {
                                Website selectedItem = arrayAdapter.getItem(selected.keyAt(i));
                                // Refresh selected items
                                mService.check(selectedItem);
                            }
                        }
                        // Close CAB
                        mode.finish();
                        return true;
                    case R.id.action_delete:
                        removedSites = new ArrayList<>();
                        for (int i = (selected.size() - 1); i >= 0; i--) {
                            if (selected.valueAt(i)) {
                                final Website selectedItem = arrayAdapter.getItem(selected.keyAt(i));
                                // Refresh selected items
                                mService.deleteWebsite(selectedItem);
                                removedSites.add(selectedItem);
                            }
                        }
                        String removedText;
                        if (removedSites.size() > 1) {
                            removedText = removedSites.size() + " websites removed";
                        } else {
                            removedText = "Website removed";
                        }
                        Snackbar snackbar = Snackbar.make(listView, removedText, Snackbar.LENGTH_LONG);
                        snackbar.setAction("Undo", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                for (int i = (removedSites.size() - 1); i >= 0; i--) {
                                    mService.addWebsite(removedSites.get(i));
                                }

                            }
                        });
                        snackbar.show();
                        // Close CAB
                        mode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_selection, menu);
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                arrayAdapter.removeSelection();
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }
        });

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.activity_main_swipe_refresh_layout);

        websites = new ArrayList<>();
        arrayAdapter = new WebsiteAdapter(this, websites);

        // Hide the empty default view if there are items
        if (!websites.isEmpty()) {
            findViewById(R.id.empty).setVisibility(View.GONE);
        } else {
            findViewById(R.id.empty).setVisibility(View.VISIBLE);
        }

        // Set up the view
        listView.setAdapter(arrayAdapter);

        // Set up swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                checkAll();
            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.primary, R.color.primary_dark);

        // Set up a timer to update the view every minute, needed for timestamps to update.
        Thread timer = new Thread() {
            public void run () {
                //noinspection InfiniteLoopStatement
                while (true) {
                    uiCallback.sendEmptyMessage(0);
                    try {
                        Thread.sleep(60000);    // sleep for 60 seconds
                    } catch (InterruptedException e) {
                        // Do nothing
                        Log.e("WebHawk", "InterruptedException in update thread");
                    }
                }
            }
        };
        timer.start();

        // Receive messages from service so we know when to update
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("WEBSITE_DATA_UPDATED"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindToService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindFromService();
    }

    @Override
    protected void onDestroy() {
        // Unregister from the broadcast from service
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    private Handler uiCallback = new Handler () {
        public void handleMessage (Message msg) {
            arrayAdapter.notifyDataSetChanged();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, PREFS_UPDATED);
            return true;
        } else if (id == R.id.action_refresh) {
            checkAll();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void bindToService() {
        Log.w("WebHawk", "Binding service");
        // Bind to UpdateService
        Intent intent = new Intent(this, UpdateService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindFromService() {
        Log.w("WebHawk", "Unbinding service");
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @SuppressWarnings("unused")
    public void addClicked(View v) {
        showAddDialog("");
    }

    private void showAddDialog(String url) {
        AddWebsite dialog = AddWebsite.newInstance(url);
        dialog.show(getFragmentManager(), "AddWebsite");
    }

    private void checkAll() {
        if (mBound) {
            mService.checkAll();
        }
        swipeRefreshLayout.setRefreshing(false);
    }

    void addWebsite(String url) {
        Website newWebsite = new Website(url);
        if (newWebsite.isMalformedURL()) {
            showAddDialog(url);
            return;
        }
        mService.addWebsite(newWebsite);

        reloadWebsites();
        checkAll();
    }

    public void reloadWebsites() {
        if (mBound) {
            ArrayList<Website> items = mService.getWebsites();
            // Replace the list of websites in the class
            websites.clear();
            websites.addAll(items);

            // Refresh the listing
            arrayAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        EditText urlField = (EditText) dialog.getDialog().findViewById(R.id.add_url);
        addWebsite(urlField.getText().toString());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case PREFS_UPDATED:
                // restart service
                unbindFromService();
                Log.w("WebHawk", "Stopping service");
                stopService(new Intent(this, UpdateService.class));
                bindToService();
                break;
        }
    }
}
