package com.laserfountain.webhawk;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

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

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        listView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        listView.setLayoutManager(layoutManager);

        websites = new ArrayList<Website>();
        arrayAdapter = new WebsiteAdapter(this, websites);

        websites.add(new Website("http://maltelenz.com", arrayAdapter));
        websites.add(new Website("http://www.wowloot.com", arrayAdapter));

        // Hide the empty default view if there are items
        if (!websites.isEmpty()) {
            findViewById(R.id.empty).setVisibility(View.GONE);
        }

        checkAll();

        listView.addItemDecoration(new DividerItemDecoration(this));

        listView.setAdapter(arrayAdapter);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                checkAll();
            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.primary, R.color.primary_dark);
    }

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

    private void checkAll() {
        for (Website website : websites) {
            website.check();
        }
        swipeRefreshLayout.setRefreshing(false);
    }
}
