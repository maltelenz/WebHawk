package com.laserfountain.webhawk;

import android.text.format.DateUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

public class Website  implements Serializable {
    private URL url;
    private Date checked;

    private boolean malformedURL;
    private boolean alive;

    public Website(String uriString) {
        this(uriString, new Date(0));
    }

    public Website(String uriString, Date checkedIn) {
        try {
            url = new URL(uriString);
        } catch (MalformedURLException mue) {
            malformedURL = true;
        }
        checked = checkedIn;
        alive = false;
    }

    private boolean internetAvailable() {
        try {
            Process ipProcess = Runtime.getRuntime().exec("/system/bin/ping -c 1 8.8.8.8");
            return ipProcess.waitFor() == 0;
        } catch (IOException e) {
            // ignore
        } catch (InterruptedException e) {
            // ignore
        }

        return false;
    }

    void check() {
        Log.d("WebHawk", "Checking page: " + url);
        if (!internetAvailable()) {
            Log.d("WebHawk", "Device offline when checking: " + url);
            return;
        }
        InputStream inputStream = null;
        BufferedReader bufferedReader;
        String line;
        String fullResponse = "";
        try {
            inputStream = url.openStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            while ((line = bufferedReader.readLine()) != null) {
                fullResponse.concat(line);
            }
            alive = true;
        } catch (IOException ioe) {
            alive = false;
            Log.d("WebHawk", "Error: " + url + "  --> " + ioe.getMessage());
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException ioe) {
                // nothing to see here
            }
        }
        checked = new Date();

        Log.d("WebHawk", "Result page: " + url + " ---> " + alive);
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
