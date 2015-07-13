package com.laserfountain.webhawk;

import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;


public class MainActivity extends AppCompatActivity implements AddWebsite.NoticeDialogListener {

    private RecyclerView listView;
    private RecyclerView.LayoutManager layoutManager;
    SwipeRefreshLayout swipeRefreshLayout;

    ArrayList<Website> websites;
    WebsiteAdapter arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (RecyclerView) findViewById(R.id.listView);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.activity_main_swipe_refresh_layout);

        // using this to improve performance as changes in content
        // do not change the layout size of the RecyclerView
        listView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        listView.setLayoutManager(layoutManager);

        websites = new ArrayList<Website>();
        arrayAdapter = new WebsiteAdapter(this, websites);

        // Load all the websites
        loadFromStorage();

        // Hide the empty default view if there are items
        if (!websites.isEmpty()) {
            findViewById(R.id.empty).setVisibility(View.GONE);
        }

        // Set up the view
        listView.addItemDecoration(new DividerItemDecoration(this));
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
                for (;;) {
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

    public void addClicked(View v) {
        showAddDialog(new String());
    }

    private void showAddDialog(String url) {
        AddWebsite dialog = AddWebsite.newInstance(url);
        dialog.show(getFragmentManager(), "AddWebsite");
    }

    private void checkAll() {
        for (Website website : websites) {
            website.check();
        }
        swipeRefreshLayout.setRefreshing(false);
    }

    void addWebsite(String url) {
        Website newWebsite = new Website(url, arrayAdapter);
        if (newWebsite.isMalformedURL()) {
            showAddDialog(url);
            return;
        }
        websites.add(newWebsite);

        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        Set<String> set= new HashSet<String>();
        for (int i = 0; i < websites.size(); i++) {
            set.add(websites.get(i).getJSONObject().toString());
        }

        editor.putStringSet("allWebsites", set);
        editor.commit();

        // Refresh the listing
        arrayAdapter.notifyDataSetChanged();
    }

    public void loadFromStorage() {
        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);

        ArrayList<Website> items = new ArrayList<Website>();

        Set<String> set = preferences.getStringSet("allWebsites", new HashSet<String>());
        for (String s : set) {
            try {
                JSONObject jsonObject = new JSONObject(s);
                String url = jsonObject.getString("url");
                Date checked = new Date(jsonObject.getLong("checked"));
                Website website = new Website(url, checked, arrayAdapter);

                items.add(website);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // Replace the list of websites in the class
        websites.clear();
        websites.addAll(items);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        EditText urlField = (EditText) dialog.getDialog().findViewById(R.id.add_url);
        addWebsite(urlField.getText().toString());
    }
}
