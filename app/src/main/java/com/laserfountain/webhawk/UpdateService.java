package com.laserfountain.webhawk;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class UpdateService extends Service {
    private ServiceHandler mServiceHandler;
    private final IBinder mBinder = new LocalBinder();
    private ArrayList<Website> websites;
    private final int NOTIFICATION_ID = 1;

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
            if (msg.arg2 < 0) {
                Log.d("WebHawk", "Checking all websites");
                for (Website website : websites) {
                    website.check();
                    sendUpdateMessage();
                    saveToStorage();
                }
            } else if (msg.arg2 > -1) {
                Log.d("WebHawk", "Checking one website");
                websites.get(msg.arg2).check();
                sendUpdateMessage();
                saveToStorage();
            }

            updateNotification();

            if (msg.arg2 < -1) {
                // We can stop here.
                Log.w("WebHawk", "Service stopSelf");
                stopSelf(msg.arg1);
            }
        }
    }

    private void updateNotification() {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int nrBroken = 0;
        for (Website website : websites) {
            if (!website.isAlive()) {
                nrBroken++;
            }
        }
        if (nrBroken == 0) {
            // Cancel the notification
            mNotificationManager.cancel(NOTIFICATION_ID);
            return;
        }

        // There are unavailable sites, so show a notification
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.error)
                        .setColor(getResources().getColor(R.color.error))
                        .setContentTitle("Webhawk")
                        .setContentText("Websites unavailable")
                        .setNumber(nrBroken)
                        .setAutoCancel(true)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setOnlyAlertOnce(true);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean vibrate = sharedPref.getBoolean(getResources().getString(R.string.pref_key_vibrate), true);

        if (vibrate) {
            mBuilder.setVibrate(new long[]{100, 100, 75, 100});
        }

        NotificationCompat.InboxStyle inboxStyle =
                new NotificationCompat.InboxStyle();
        // Sets a title for the Inbox in expanded layout
        inboxStyle.setBigContentTitle(Integer.toString(nrBroken) + " websites unavailable:");
        // Moves events into the expanded layout
        for (Website website : websites) {
            if (!website.isAlive()) {
                inboxStyle.addLine(website.getURL());
            }
        }

        // Move the expanded layout object into the notification object.
        mBuilder.setStyle(inboxStyle);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
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
        Log.w("WebHawk", "Service onCreate");
        // Load default values for preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Set up AlarmManager to start the service regularly
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        int intervalInMinutes = Integer.valueOf(sharedPref.getString(getResources().getString(R.string.pref_key_check_interval), "60"));
        int pollIntervalMilliSeconds = 1000 * 60 * intervalInMinutes;

        boolean checkAutomatically = sharedPref.getBoolean(getResources().getString(R.string.pref_key_check), true);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MILLISECOND, pollIntervalMilliSeconds);
        Intent serviceIntent = new Intent(this, UpdateService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, serviceIntent, 0);
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (checkAutomatically) {
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pollIntervalMilliSeconds, pendingIntent);

            Log.d("WebHawk", "Just set an alarm with the interval " + Integer.toString(intervalInMinutes));
        } else {
            alarm.cancel(pendingIntent);
        }

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
        Log.w("WebHawk", "Service onCreate end");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w("WebHawk", "Service onStartCommand");
        // Check all websites
        checkAll(startId);

        Log.w("WebHawk", "Service onStartCommand return");
        // If we get killed, after returning from here, that's ok, an alarm will start us again
        return  START_NOT_STICKY;
    }

    // checkAll that also results in stopSelf()
    private void checkAll(int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.arg2 = -2;
        mServiceHandler.sendMessage(msg);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.w("WebHawk", "Service onBind");
        return mBinder;
    }

    private void sendUpdateMessage() {
        Intent intent = new Intent("WEBSITE_DATA_UPDATED");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Public functions intended to be called (among other places)
    // from the controlling activity

    public void check(Website website) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg2 = websites.indexOf(website);
        mServiceHandler.sendMessage(msg);
    }

    public void checkAll() {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg2 = -1;
        mServiceHandler.sendMessage(msg);
    }

    public void deleteWebsite(Website website) {
        websites.remove(website);
        sendUpdateMessage();
        saveToStorage();
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

