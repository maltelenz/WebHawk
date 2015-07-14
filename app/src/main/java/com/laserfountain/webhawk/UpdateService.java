package com.laserfountain.webhawk;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class UpdateService extends Service {
    private ServiceHandler mServiceHandler;
    private final IBinder mBinder = new LocalBinder();
    private ArrayList<Website> websites;

    public class LocalBinder extends Binder {
        UpdateService getService() {
            // Return this instance of LocalService so clients can call public methods
            return UpdateService.this;
        }
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            Log.d("WebHawk", "Checking websites");
            for (Website website : websites) {
                website.check();
                sendUpdateMessage();
                saveToStorage();
            }
            Log.d("WebHawk", "Saving websites");
        }
    }

    @SuppressLint("CommitPrefEdits")
    private void saveToStorage() {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("MainActivity", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        Set<String> set= new HashSet<>();
        for (int i = 0; i < websites.size(); i++) {
            set.add(websites.get(i).getJSONObject().toString());
        }

        editor.putStringSet("allWebsites", set);
        editor.commit();
    }

    private void fetchStorage() {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("MainActivity", Context.MODE_PRIVATE);

        ArrayList<Website> items = new ArrayList<>();

        Set<String> set = preferences.getStringSet("allWebsites", new HashSet<String>());
        for (String s : set) {
            try {
                JSONObject jsonObject = new JSONObject(s);
                String url = jsonObject.getString("url");
                Date checked;
                try {
                    checked = new Date(jsonObject.getLong("checked"));
                } catch (JSONException e) {
                    checked = new Date(0);
                }
                Boolean alive;
                try {
                    alive = jsonObject.getBoolean("alive");
                } catch (JSONException e) {
                    alive = false;
                }
                Website website = new Website(url, checked);
                website.setAlive(alive);

                items.add(website);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        websites = items;
    }

    @Override
    public void onCreate() {
        // Load the websites from storage
        fetchStorage();

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        Looper mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        // Check all websites
        checkAll();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void sendUpdateMessage() {
        Intent intent = new Intent("WEBSITE_DATA_UPDATED");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Public functions intended to be called (among other places)
    // from the controlling activity

    public void checkAll() {
        Message msg = mServiceHandler.obtainMessage();
        mServiceHandler.sendMessage(msg);
    }

    public void addWebsite(Website site) {
        websites.add(site);
        sendUpdateMessage();
        saveToStorage();
    }

    public ArrayList<Website> getWebsites() {
        return websites;
    }
}

