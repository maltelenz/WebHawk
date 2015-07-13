package com.laserfountain.webhawk;

import android.os.AsyncTask;
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
    private URL url;
    private Date checked;

    private boolean malformedURL;
    private boolean alive;

    public Website(String uriString, WebsiteAdapter adapterIn) {
        this(uriString, new Date(0), adapterIn);
    }

    public Website(String uriString, Date checkedIn, WebsiteAdapter adapterIn) {
        try {
            url = new URL(uriString);
        } catch (MalformedURLException mue) {
            malformedURL = true;
        }
        checked = checkedIn;
        adapter = adapterIn;
    }

    private class DownloadWebsiteTask extends AsyncTask<URL, Void, Boolean> {
        // Do the long-running work in here
        protected Boolean doInBackground(URL... urls) {
            Log.d("WebHawk", "Checking page: " + urls[0]);
            Boolean result = true;
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
                result = false;
            } finally {
                try {
                    if (inputStream != null) inputStream.close();
                } catch (IOException ioe) {
                    // nothing to see here
                }
            }
            Log.d("WebHawk", "Result page: " + urls[0] + " ---> " + result);
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            alive = result;
            checked = new Date();
            // Alert the view that we have changed the result
            adapter.notifyDataSetChanged();

        }
    }
    void check() {
        new DownloadWebsiteTask().execute(url);
    }

    boolean isMalformedURL() {
        return malformedURL;
    }

    boolean isUp() {
        return alive;
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
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }
}
