package com.laserfountain.webhawk;

import android.os.AsyncTask;
import android.util.Log;

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
        try {
            url = new URL(uriString);
        } catch (MalformedURLException mue) {
            malformedURL = true;
        }
        checked = new Date(0);
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
}