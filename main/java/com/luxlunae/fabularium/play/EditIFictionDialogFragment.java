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
package com.luxlunae.fabularium.play;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.luxlunae.fabularium.MainActivity;
import com.luxlunae.fabularium.R;

import java.io.File;

/**
 * A dialog that allows the user to edit metadata pertaining to a specific
 * game - including ifid, title and game format.
 */
public class EditIFictionDialogFragment extends DialogFragment {
    @Nullable
    private IFictionRecord mIfr;

    /**
     * Override to build your own custom Dialog container. This is typically
     * used to show an AlertDialog instead of a generic Dialog; when doing so,
     * onCreateView(LayoutInflater, ViewGroup, Bundle) does not need to be
     * implemented since the AlertDialog takes care of its own content.
     * <p>
     * This method will be called after onCreate(Bundle) and before
     * onCreateView(LayoutInflater, ViewGroup, Bundle). The default
     * implementation simply instantiates and returns a Dialog class.
     * <p>
     * Note: DialogFragment own the Dialog.setOnCancelListener and Dialog.setOnDismissListener
     * callbacks. You must not set them yourself. To find out about these events, override
     * onCancel(DialogInterface) and onDismiss(DialogInterface).
     *
     * @param savedInstanceState - The last saved instance state of the Fragment, or
     *                           null if this is a freshly created Fragment.
     * @return Return a new Dialog instance to be displayed by the Fragment.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Fragment f = getTargetFragment();
        final MainActivity context = (MainActivity) f.getActivity();

        setRetainInstance(true);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        @SuppressLint("InflateParams") View dialogView = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_edit_game_meta, null);
        builder.setView(dialogView);

        final TextView tvFileName = dialogView.findViewById(R.id.tvFileName);
        final EditText etTitle = dialogView.findViewById(R.id.meta_title);
        final EditText etIFID = dialogView.findViewById(R.id.meta_ifid);
        final ListView lvFormats = dialogView.findViewById(R.id.meta_formats);

        if (mIfr != null) {
            tvFileName.setText(new File(mIfr.file_path).getName());
        } else {
            tvFileName.setText(R.string.unknown);
        }

        final StringBuilder sb = new StringBuilder();
        final ArrayAdapter lst = ArrayAdapter.createFromResource(builder.getContext(),
                R.array.validGameFormats, android.R.layout.simple_list_item_single_choice);
        lvFormats.setAdapter(lst);
        lvFormats.setChoiceMode(ListView.CHOICE_MODE_SINGLE);      // note: this is necessary to enable the checkboxes
        lvFormats.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull AdapterView<?> adapter, View v, int position, long id) {
                sb.setLength(0);
                sb.append(adapter.getItemAtPosition(position).toString());
            }
        });

        builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String ifid = etIFID.getText().toString();
                if (ifid.equals("")) {
                    Toast.makeText(context, "Could not save metadata - you didn't specify the IFID.", Toast.LENGTH_SHORT).show();
                    return;
                }
                mIfr.ifids = new String[1];
                mIfr.ifids[0] = ifid;

                String title = etTitle.getText().toString();
                if (title.equals("")) {
                    Toast.makeText(context, "Could not save metadata - you didn't specify a title.", Toast.LENGTH_SHORT).show();
                    return;
                }
                mIfr.title = title;

                String format = sb.toString();
                if (format.equals("")) {
                    Toast.makeText(context, "Could not save metadata - you didn't choose a format.", Toast.LENGTH_SHORT).show();
                    return;
                }
                mIfr.format = format;

                // Save it back into the database
                IFictionDbHelper mDbHelper = new IFictionDbHelper(context);
                SQLiteDatabase dbWrite = mDbHelper.getWritableDatabase();
                if (IFictionDbHelper.put(mIfr, dbWrite)) {
                    Toast.makeText(context, "Saved metadata for IFID " + mIfr.ifids[0] + ".", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Could not save metadata - failed to write to database.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
            }
        });

        // Create the AlertDialog object and return it
        return builder.create();
    }

    /**
     * Remove dialog.
     */
    @Override
    public void onDestroyView() {
        // We need to work around an Android bug that causes
        // the dialog to be incorrectly dismissed after screen rotation.
        // See https://code.google.com/p/android/issues/detail?id=17423
        Dialog dialog = getDialog();
        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }
        super.onDestroyView();
    }

    public void setIFictionRecord(@Nullable IFictionRecord r) {
        mIfr = r;
    }
}
