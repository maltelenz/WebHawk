package com.laserfountain.webhawk;

import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.PendingIntent;
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
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity implements AddWebsite.NoticeDialogListener{

    SwipeRefreshLayout swipeRefreshLayout;

    ArrayList<Website> websites;
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

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.listView);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.activity_main_swipe_refresh_layout);

        // using this to improve performance as changes in content
        // do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        websites = new ArrayList<>();
        arrayAdapter = new WebsiteAdapter(this, websites);

        // Hide the empty default view if there are items
        if (!websites.isEmpty()) {
            findViewById(R.id.empty).setVisibility(View.GONE);
        } else {
            findViewById(R.id.empty).setVisibility(View.VISIBLE);
        }

        // Set up the view
        recyclerView.addItemDecoration(new DividerItemDecoration(this));
        recyclerView.setAdapter(arrayAdapter);

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
        // Bind to UpdateService
        Intent intent = new Intent(this, UpdateService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
}
