package com.laserfountain.webhawk;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class WebsiteAdapter extends ArrayAdapter<Website> {
    public WebsiteAdapter(Context context, ArrayList<Website> websites) {
        super(context, 0, websites);
    }

    @Override
    public View getView(int position, View websiteView, ViewGroup parent) {
        // Get the data item for this position
        Website website = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (websiteView == null) {
            websiteView = LayoutInflater.from(getContext()).inflate(R.layout.website_item, parent, false);
        }
        // Lookup view for data population
        TextView url = (TextView) websiteView.findViewById(R.id.url);
        TextView date = (TextView) websiteView.findViewById(R.id.date_checked);
        ImageView up = (ImageView) websiteView.findViewById(R.id.up);
        ImageView error = (ImageView) websiteView.findViewById(R.id.error);
        // Populate the data into the template view using the data object
        url.setText(website.getURL());
        if (website.isUp()) {
            up.setVisibility(View.VISIBLE);
            error.setVisibility(View.GONE);
        } else {
            up.setVisibility(View.GONE);
            error.setVisibility(View.VISIBLE);
        }
        date.setText(website.getChecked().toString());
        // Return the completed view to render on screen
        return websiteView;
    }
}