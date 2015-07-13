package com.laserfountain.webhawk;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

public class Website {
    private WebsiteAdapter adapter;

    WebsiteUpdatedListener updateListener;

    private URL url;
    private Date checked;

    private boolean malformedURL;
    private boolean alive;

    public Website(String uriString, Activity activity) {
        this(uriString, new Date(0), activity);
    }

    public Website(String uriString, Date checkedIn, Activity activity) {
        try {
            url = new URL(uriString);
        } catch (MalformedURLException mue) {
            malformedURL = true;
        }
        checked = checkedIn;
        updateListener = (WebsiteUpdatedListener) activity;
    }

    public interface WebsiteUpdatedListener {
        public void onWebsiteUpdated();
    }

    private class DownloadWebsiteTask extends AsyncTask<URL, Void, Bundle> {
        private final String ALIVE_KEY = "alive";
        // Do the long-running work in here
        protected Bundle doInBackground(URL... urls) {
            Log.d("WebHawk", "Checking page: " + urls[0]);
            Boolean alive = true;
            InputStream inputStream = null;
            BufferedReader bufferedReader;
            String line;
            String fullResponse = "";

            try {
                inputStream = urls[0].openStream();
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                while ((line = bufferedReader.readLine()) != null) {
                    fullResponse.concat(line);
                }
            } catch (IOException ioe) {
                alive = false;
            } finally {
                try {
                    if (inputStream != null) inputStream.close();
                } catch (IOException ioe) {
                    // nothing to see here
                }
            }
            Log.d("WebHawk", "Result page: " + urls[0] + " ---> " + alive);
            Bundle result = new Bundle();
            result.putBoolean(ALIVE_KEY, alive);
            return result;
        }

        @Override
        protected void onPostExecute(Bundle result) {
            alive = result.getBoolean(ALIVE_KEY);
            checked = new Date();
            // Alert the view that we have changed the result
            updateListener.onWebsiteUpdated();
        }
    }
    void check() {
        new DownloadWebsiteTask().execute(url);
    }

    boolean isMalformedURL() {
        return malformedURL;
    }

    boolean isAlive() {
        return alive;
    }

    void setAlive(boolean aliveIn) {
        alive = aliveIn;
    }

    Date getChecked() {
        return checked;
    }

    public String getURL() {
        return url.toString();
    }

    public String getHumanTimeChecked()
    {
        long milliSecondsInFourHours = 4*60*60*1000;

        long timeDifferenceInMilliSeconds = new Date().getTime() - checked.getTime();

        if ( timeDifferenceInMilliSeconds < milliSecondsInFourHours)
        {
            return DateUtils.getRelativeTimeSpanString(checked.getTime()).toString();
        }
        else
        {
            return checked.toString();
        }
    }

    public JSONObject getJSONObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("url", this.url.toString());
            obj.put("checked", this.checked.getTime());
            obj.put("alive", this.alive);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }
}
