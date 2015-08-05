package com.laserfountain.webhawk;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.List;

public class CheckableRelativeLayout extends RelativeLayout implements Checkable {

    private boolean isChecked;

    public CheckableRelativeLayout(Context context) {
        super(context);
    }

    public CheckableRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setChecked(boolean checked) {
        isChecked = checked;
        ImageView status = (ImageView) findViewById(R.id.status);
        ImageView selected = (ImageView) findViewById(R.id.selected);
        if (checked) {
            selected.setVisibility(VISIBLE);
            status.setVisibility(GONE);
        } else {
            selected.setVisibility(GONE);
            status.setVisibility(VISIBLE);
        }
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public void toggle() {
        setChecked(!this.isChecked);
    }
}