package com.laserfountain.webhawk;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class WebsiteAdapter extends ArrayAdapter<Website> {
    private ArrayList<Website> websites;
    private final Context context;
    LayoutInflater inflater;
    private SparseBooleanArray selectedIds;

    public static class ViewHolder {
        public TextView url;
        public TextView date;
        public ImageView up;
        public ImageView error;
    }

    public WebsiteAdapter(Context context, ArrayList<Website> websites) {
        super(context, -1, websites);
        this.context = context;
        this.websites = websites;
        this.selectedIds = new SparseBooleanArray();
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;

        Website website = websites.get(position);
        if (view == null) {
            view = inflater.inflate(R.layout.website_item, parent, false);

            holder = new ViewHolder();
            holder.url = (TextView) view.findViewById(R.id.url);
            holder.date = (TextView) view.findViewById(R.id.date_checked);
            holder.up = (ImageView) view.findViewById(R.id.up);
            holder.error = (ImageView) view.findViewById(R.id.error);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        holder.url.setText(website.getURL());

        if (website.isAlive()) {
            holder.up.setVisibility(View.VISIBLE);
            holder.error.setVisibility(View.GONE);
        } else {
            holder.up.setVisibility(View.GONE);
            holder.error.setVisibility(View.VISIBLE);
        }
        holder.date.setText(website.getHumanTimeChecked());
        return view;
    }

    public void toggleSelection(int position) {
        selectView(position, !selectedIds.get(position));
    }

    public void removeSelection() {
        selectedIds = new SparseBooleanArray();
        notifyDataSetChanged();
    }

    public void selectView(int position, boolean value) {
        if (value)
            selectedIds.put(position, value);
        else
            selectedIds.delete(position);
        notifyDataSetChanged();
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    public SparseBooleanArray getSelectedIds() {
        return selectedIds;
    }
}

