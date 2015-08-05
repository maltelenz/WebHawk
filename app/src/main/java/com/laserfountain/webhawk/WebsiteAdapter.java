package com.laserfountain.webhawk;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
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
        public ImageView status;
        public ImageView open;
        public FrameLayout icon;
    }

    public WebsiteAdapter(Context context, ArrayList<Website> websites) {
        super(context, -1, websites);
        this.context = context;
        this.websites = websites;
        this.selectedIds = new SparseBooleanArray();
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View view, final ViewGroup parent) {
        final ViewHolder holder;

        final Website website = websites.get(position);
        if (view == null) {
            view = inflater.inflate(R.layout.website_item, parent, false);

            holder = new ViewHolder();
            holder.url = (TextView) view.findViewById(R.id.url);
            holder.date = (TextView) view.findViewById(R.id.date_checked);
            holder.status = (ImageView) view.findViewById(R.id.status);
            holder.open = (ImageView) view.findViewById(R.id.open);
            holder.icon = (FrameLayout) view.findViewById(R.id.icon);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        holder.url.setText(website.getURL());

        if (website.isAlive()) {
            holder.status.setImageResource(R.drawable.ic_lens_black_48dp);
            holder.status.setColorFilter(context.getResources().getColor(R.color.success));
        } else {
            holder.status.setImageResource(R.drawable.ic_error_black_48dp);
            holder.status.setColorFilter(context.getResources().getColor(R.color.error));
        }
        holder.date.setText(website.getHumanTimeChecked());

        holder.open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Webhawk", "Clicked open on: " + website.getURL());
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(website.getURL()));
                context.startActivity(intent);
            }
        });
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

