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
package com.luxlunae.fabularium.create;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.luxlunae.fabularium.MainActivity;
import com.luxlunae.fabularium.R;
import com.luxlunae.fabularium.create.ProjectDbHelper.ProjectRecord;

/**
 * A dialog that allows the user to edit metadata pertaining to a specific
 * project - including which compiler to use, compiler arguments, output file path
 * and output file format.
 */
public class ProjectDialogFragment extends DialogFragment {
    @Nullable
    private ProjectRecord mPR;

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

        final ProjectSelectorFragment f = (ProjectSelectorFragment) getTargetFragment();
        final MainActivity context = (MainActivity) f.getActivity();

        setRetainInstance(true);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        @SuppressLint("InflateParams") View dialogView = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_edit_project_meta, null);
        builder.setView(dialogView);

        final TextView tvProjName = dialogView.findViewById(R.id.tvProjName);
        final Spinner spinCompiler = dialogView.findViewById(R.id.editCompiler);
        final EditText etCompilerArgs = dialogView.findViewById(R.id.editCompilerArgs);
        final EditText etOutputFile = dialogView.findViewById(R.id.editOutputFile);
        final Spinner spinOutputFormat = dialogView.findViewById(R.id.editOutputFormat);

        // Set initial values for the widgets
        tvProjName.setText(mPR.title);
        ArrayAdapter<CharSequence> compilers = ArrayAdapter.createFromResource(getContext(),
                R.array.validCompilers, android.R.layout.simple_spinner_item);
        compilers.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinCompiler.setAdapter(compilers);
        String[] items = getResources().getStringArray(R.array.validCompilers);
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(mPR.compiler)) {
                spinCompiler.setSelection(i);
                break;
            }
        }
        etCompilerArgs.setText(mPR.compilerArgs);
        etOutputFile.setText(mPR.outputFile);
        ArrayAdapter<CharSequence> adapterFormats = ArrayAdapter.createFromResource(getContext(),
                R.array.validCompilerOutputFormats, android.R.layout.simple_spinner_item);
        adapterFormats.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinOutputFormat.setAdapter(adapterFormats);
        items = getResources().getStringArray(R.array.validCompilerOutputFormats);
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(mPR.outputFormat)) {
                spinOutputFormat.setSelection(i);
                break;
            }
        }

        // Set actions for save and close
        builder.setPositiveButton(R.string.menu_save_text_file, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // N.B. it's important to remove any leading and/or trailing
                // whitespace because, e.g. the Inform compiler doesn't seem
                // to be able to handle more than one space between terminal
                // arguments
                mPR.compiler = spinCompiler.getSelectedItem().toString().trim();
                mPR.compilerArgs = etCompilerArgs.getText().toString().trim();
                mPR.outputFile = etOutputFile.getText().toString().trim();
                mPR.outputFormat = spinOutputFormat.getSelectedItem().toString().trim();

                ProjectDbHelper mDbHelper = new ProjectDbHelper(context);
                SQLiteDatabase dbWrite = mDbHelper.getWritableDatabase();
                if (ProjectDbHelper.put(mPR, dbWrite)) {
                    Toast.makeText(context, R.string.dialog_project_metadata_saved, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, R.string.dialog_project_metadata_not_saved, Toast.LENGTH_SHORT).show();
                }
                dbWrite.close();

                // Restart the loader to pull in the updated data
                context.getSupportLoaderManager().restartLoader(MainActivity.DB_LOADER_PROJECTS, null, f);

            }
        }).setNegativeButton(R.string.menu_close_text_file, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
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

    public void setProjectRecord(ProjectRecord r) {
        mPR = r;
    }
}
