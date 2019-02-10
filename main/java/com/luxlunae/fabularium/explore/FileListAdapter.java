/*
 * Copyright (C) 2018 Tim Cadogan-Cowper.
 *
 * This file is part of Fabularium.
 *
 * Fabularium is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Fabularium; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.luxlunae.fabularium.explore;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.luxlunae.fabularium.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The FileListAdapter acts as a bridge between a file browser ListView and the underlying data for that view. The Adapter
 * provides access to the data items. It is also responsible for making a View for each item in the data set.
 */
public class FileListAdapter extends BaseAdapter implements OnCheckedChangeListener {
    @NonNull
    private final List<File> mValues;
    @NonNull
    private final boolean[] mChecked;
    @Nullable
    private final Drawable mFolderDrawable;
    @Nullable
    private final Drawable mFileDrawable;
    @Nullable
    private final Drawable mZippedDrawable;
    @Nullable
    private final LayoutInflater mInflater;
    @Nullable
    private final ToolbarListener mToolbarListener;
    private boolean mShowCheckBoxes;

    FileListAdapter(@NonNull Context context, @NonNull List<File> values, @Nullable ToolbarListener toolbarListener) {
        mShowCheckBoxes = true;
        mValues = values;
        mChecked = new boolean[mValues.size()];
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mToolbarListener = toolbarListener;

        // Load the icons we'll use
        Resources r = context.getResources();
        mFolderDrawable = ResourcesCompat.getDrawable(r, R.drawable.ic_folder_white_24dp, null);
        mFileDrawable = ResourcesCompat.getDrawable(r, R.drawable.ic_insert_drive_file_white_24dp, null);
        mZippedDrawable = ResourcesCompat.getDrawable(r, R.drawable.ic_zipped_white_24dp, null);
    }

    public void setAll(boolean checked) {
        int sz = mChecked.length;
        for (int i = 0; i < sz; i++) {
            mChecked[i] = checked;
        }
        notifyDataSetInvalidated();
        if (mToolbarListener != null) {
            mToolbarListener.setToolbarSelected(checked ? sz : 0, sz);
        }
    }

    void toggleCheckBoxes(boolean on) {
        if (mShowCheckBoxes != on) {
            mShowCheckBoxes = on;
            setAll(false);
        }
    }

    /**
     * How many items are in the data set represented by this Adapter.
     *
     * @return Count of items.
     */
    @Override
    public int getCount() {
        return mValues.size();
    }

    /**
     * Get the data item associated with the specified position in the data set.
     *
     * @param position - Position of the item whose data we want within the adapter's data set.
     * @return The data at the specified position.
     */
    @Override
    public Object getItem(int position) {
        return mValues.get(position);
    }

    /**
     * Get the row id associated with the specified position in the list.
     *
     * @param position - The position of the item within the adapter's data set whose row id we want.
     *
     * @return The id of the item at the specified position.
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Get a View that displays the data at the specified position in the data set. You can
     * either create a View manually or inflate it from an XML layout file. When the View is
     * inflated, the parent View (GridView, ListView...) will apply default layout parameters
     * unless you use inflate(int, android.view.ViewGroup, boolean) to specify a root view
     * and to prevent attachment to the root.
     *
     * @param position - The position of the item within the adapter's data set of the item whose view we want.
     * @param convertView - The old view to reuse, if possible. Note: You should check that this view is
     *                    non-null and of an appropriate type before using. If it is not possible
     *                    to convert this view to display the correct data, this method can create
     *                    a new view. Heterogeneous lists can specify their number of view types,
     *                    so that this View is always of the right type (see getViewTypeCount() and getItemViewType(int)).
     * @param parent - The parent that this view will eventually be attached to
     *
     * @return A View corresponding to the data at the specified position.
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            assert mInflater != null;
            view = mInflater.inflate(R.layout.file_selector_cell, parent, false);
        }

        // OK, create a text view with compound drawable
        File f = mValues.get(position);

        ImageView imgv = view.findViewById(R.id.imgFile);
        if (f.getName().toLowerCase().endsWith(".zip")) {
            imgv.setImageDrawable(mZippedDrawable);
        } else {
            imgv.setImageDrawable((f instanceof Shortcut || f.isDirectory()) ? mFolderDrawable : mFileDrawable);
        }

        TextView tv = view.findViewById(R.id.tvFile);
        tv.setText(f instanceof Shortcut ? ((Shortcut) f).getDisplayText() : f.getName());

        CheckBox cb = view.findViewById(R.id.cbFile);
        TextView tv2 = view.findViewById(R.id.tvFileDetails);
        StringBuilder sb = new StringBuilder();
        if (f instanceof Shortcut) {
            sb.append("=> ").append(f.getAbsolutePath());
        } else {
            if (f.isDirectory()) {
                String[] lst = f.list();
                if (lst != null) {
                    int n = lst.length;
                    sb.append(String.valueOf(n)).append((n != 1) ? " items" : " item");
                } else {
                    sb.append("0 items");
                }
                String s = "folder " + f.getName();
                view.setContentDescription(s);
                cb.setContentDescription(s);
            } else {
                float sz = f.length();
                if (sz < 1024) {
                    // Less than 1 KB
                    sb.append(String.valueOf(f.length())).append(" B");
                } else if (sz < 1048576) {
                    // Less than 1 MB
                    sb.append(String.valueOf((f.length() / 1024))).append(" KB");
                } else if (sz < 1073741824) {
                    // Less than 1 GB
                    sb.append(String.valueOf((f.length() / 1048576))).append(" MB");
                } else {
                    // Greater than 1 GB
                    sb.append(String.valueOf((f.length() / 1073741824))).append(" GB");
                }
                String s = "file " + f.getName();
                view.setContentDescription(s);
                cb.setContentDescription(s);
            }
            sb.append("  |  ");
            SimpleDateFormat formatter = new SimpleDateFormat("d/M/yyyy HH:mm:ss", Locale.US);
            sb.append(formatter.format(f.lastModified()));
        }
        tv2.setText(sb);

        if (mShowCheckBoxes) {
            cb.setVisibility(View.VISIBLE);
            cb.setTag(position);
            cb.setChecked(mChecked[position]);
            cb.setOnCheckedChangeListener(this);
        } else {
            cb.setVisibility(View.GONE);
        }

        return view;
    }

    /**
     * Called when the checked state of a compound button has changed.
     *
     * @param buttonView - The compound button view whose state has changed.
     * @param isChecked  - The new checked state of buttonView.
     */
    @Override
    public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
        mChecked[(int) buttonView.getTag()] = isChecked;

        int nSelected = 0;
        for (boolean aMChecked : mChecked) {
            if (aMChecked) {
                nSelected++;
            }
        }
        if (mToolbarListener != null) {
            mToolbarListener.setToolbarSelected(nSelected, mChecked.length);
        }
    }

    @NonNull
    public ArrayList<File> getSelected() {
        ArrayList<File> selected = new ArrayList<>();
        for (int i = 0, sz = mChecked.length; i < sz; i++) {
            if (mChecked[i]) {
                selected.add(mValues.get(i));
            }
        }
        return selected;
    }

    public interface ToolbarListener {
        void setToolbarSelected(int numSelected, int maxItems);
    }
}
