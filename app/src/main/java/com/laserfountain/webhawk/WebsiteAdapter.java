package com.laserfountain.webhawk;

import android.content.Context;
import android.media.Image;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class WebsiteAdapter extends RecyclerView.Adapter<WebsiteAdapter.ViewHolder> {
    private ArrayList<Website> websites;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView url;
        public TextView date;
        public ImageView up;
        public ImageView error;
        public ViewHolder(View view) {
            super(view);
            url = (TextView) view.findViewById(R.id.url);
            date = (TextView) view.findViewById(R.id.date_checked);
            up = (ImageView) view.findViewById(R.id.up);
            error = (ImageView) view.findViewById(R.id.error);
        }
    }

    public WebsiteAdapter(Context context, ArrayList<Website> websites) {
        this.websites = websites;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public WebsiteAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.website_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        Website website = websites.get(position);
        // - replace the contents of the views with that element
        holder.url.setText(website.getURL());
        if (website.isUp()) {
            holder.up.setVisibility(View.VISIBLE);
            holder.error.setVisibility(View.GONE);
        } else {
            holder.up.setVisibility(View.GONE);
            holder.error.setVisibility(View.VISIBLE);
        }
        holder.date.setText(website.getChecked().toString());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return websites.size();
    }
}