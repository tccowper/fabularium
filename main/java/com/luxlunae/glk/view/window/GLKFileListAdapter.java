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

package com.luxlunae.glk.view.window;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.luxlunae.fabularium.R;

import java.util.List;

public class GLKFileListAdapter extends BaseAdapter {

    @NonNull
    private final List<GLKFileDialog.LIST_ITEM> mValues;
    @Nullable
    private final LayoutInflater mInflater;

    GLKFileListAdapter(@NonNull Context context, @NonNull List<GLKFileDialog.LIST_ITEM> values) {
        mValues = values;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mValues.size();
    }

    @Override
    public GLKFileDialog.LIST_ITEM getItem(int position) {
        return mValues.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, final @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            assert mInflater != null;
            view = mInflater.inflate(R.layout.glk_file_selector_cell, parent, false);
        }

        GLKFileDialog.LIST_ITEM f = mValues.get(position);

        TextView tv = view.findViewById(R.id.tvGlkFile);
        tv.setText(f.mName);
        TextView tv2 = view.findViewById(R.id.tvGlkFileDetails);
        tv2.setText(f.mTimeStampText);


        ImageButton rename = view.findViewById(R.id.GlkFileRename);
        rename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ListView) parent).performItemClick(v, position, 0); // Let the event be handled in onItemClick()
            }
        });

        ImageButton delete = view.findViewById(R.id.GlkFileDelete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ListView) parent).performItemClick(v, position, 0); // Let the event be handled in onItemClick()
            }
        });

        return view;
    }
}
