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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.luxlunae.fabularium.FabLogger;
import com.luxlunae.fabularium.R;
import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.model.GLKModel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class GLKFileDialog extends DialogFragment {
    @Nullable
    private GLKFileDialogListener mListener;
    @NonNull
    private static final SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", Locale.US);
    @Nullable
    private String mFilePath = null;   // assume failure until proven otherwise (my, aren't we optimistic?)
    private GLKModel mModel;
    private int mMode;
    private int mType;

    @NonNull
    public static GLKFileDialog newInstance(@NonNull GLKModel m, int fileType, int fmode) {
        GLKFileDialog f = new GLKFileDialog();
        f.mType = fileType;
        f.mMode = fmode;
        f.mModel = m;
        return f;
    }

    public void setListener(@Nullable GLKFileDialogListener listener) {
        mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        buildDialog(mModel, builder, mMode, mType);
        return builder.create();
    }

    @Nullable
    private GLKFileListAdapter mAdapter;

    /**
     * This method will be invoked when the dialog is dismissed.
     *
     * @param dialogInterface - the dialog that was dismissed will be passed into the method
     */
    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        // Important note at
        // https://developer.android.com/reference/android/app/DialogFragment.html#onCreateDialog(android.os.Bundle):
        //
        //   "Note: DialogFragment own the Dialog.setOnCancelListener
        //    and Dialog.setOnDismissListener callbacks. You must not set
        //    them yourself. To find out about these events, override
        //    onCancel(DialogInterface) and onDismiss(DialogInterface)."
        //
        // If we do override those setOnXXX callbacks our app just crashes
        // (as I quickly discovered before finding this note).
        super.onDismiss(dialogInterface);
        if (mListener != null) {
            // If the dialog is dismissed and we haven't already alerted the
            // listener (i.e. mListener is not null), treat the action as
            // though the user cancelled and alert the listener now.
            // This is important to avoid a bug whereby user touching outside
            // the dialog led to app hanging (thanks Vsevolod for reporting it)
            mListener.onDialogFinish(null);
            mListener = null;
        }
    }

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface GLKFileDialogListener {
        void onDialogFinish(String selPath);
    }

    private void buildDialog(@NonNull GLKModel m, @NonNull AlertDialog.Builder builder, int fmode, int fType) {
        // Work out what files (if any) already exist for this game, using the ifid
        // then show those in the dialog
        final File f;
        final String dir = m.mGameDataPath;
        final String ext;
        final String dialogTitle;
        switch (fType) {
            case GLKConstants.fileusage_SavedGame:
                ext = GLKConstants.GLK_SAVE_EXT;
                dialogTitle = (fmode == GLKConstants.filemode_Read) ? "Restore game" : "Save game";
                break;
            case GLKConstants.fileusage_Transcript:
                ext = GLKConstants.GLK_TRANSCRIPT_EXT;
                dialogTitle = (fmode == GLKConstants.filemode_Read) ? "Load transcript" : "Save transcript";
                break;
            case GLKConstants.fileusage_InputRecord:
                ext = GLKConstants.GLK_INPUT_RECORD_EXT;
                dialogTitle = (fmode == GLKConstants.filemode_Read) ? "Load input record" : "Save input record";
                break;
            case GLKConstants.fileusage_Data:
                ext = GLKConstants.GLK_DATA_EXT;
                dialogTitle = (fmode == GLKConstants.filemode_Read) ? "Load data" : "Save data";
                break;
            default:
                GLKLogger.error("GLKFileDialog: called with unrecognised file type.");
                if (mListener != null) {
                    mListener.onDialogFinish(null);
                    mListener = null;
                }
                return;
        }

        if (dir == null) {
            // We can't access the required folder for some reason
            // (almost certainly related to user overriding default path
            // with something that doesn't exist or that Fabularium can't
            // access).
            //
            // In this case, don't display the dialog, just treat like
            // user has clicked CANCEL.
            if (mListener != null) {
                mListener.onDialogFinish(null);
            }
            mListener = null;
            return;
        }

        builder.setTitle(dialogTitle);

        @SuppressLint("InflateParams") View dialogView = LayoutInflater.from(builder.getContext()).inflate(R.layout.glk_file_selector, null);
        final ListView lv = dialogView.findViewById(R.id.lvGlkFiles);

        // Look for any existing files of the given type for this IFID
        // If / when they are found, add to the list:
        //   File name format is "[1|2|3...].<ext>"
        // Then sort by descending order of time stamp (i.e. newest at the top),
        // Finally set the alert dialog to display the list later
        f = new File(dir);
        final ArrayList<LIST_ITEM> tmp = new ArrayList<>();
        String fpath;
        final int lenExt = ext.length();
        File[] files = f.listFiles();
        if (files != null) {
            for (File file : files) {
                fpath = file.getName();
                if (fpath.endsWith(ext)) {
                    // We have a winner
                    tmp.add(new LIST_ITEM(file, lenExt));
                }
            }
        }

        // Find the next available file name
        int nNextFreeSlot = 0;
        String tmpName;
        File file;
        do {
            nNextFreeSlot++;
            tmpName = nNextFreeSlot + ext;
            file = new File(dir, tmpName);
        } while (file.exists());

        final String newName = tmpName;

        if (tmp.size() > 0) {
            // Sort the items
            Collections.sort(tmp);

            // Show the items in a list with their time stamps
            mAdapter = new GLKFileListAdapter(builder.getContext(), tmp);
            lv.setAdapter(mAdapter);
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapter, View v, final int position, long id) {
                    long viewId = v.getId();

                    final Activity act = getActivity();
                    if (act == null || act.isFinishing()) {
                        FabLogger.error("GLKFileDialog: cannot retrieve parent activity or activity is finishing, rename command aborted.");
                        return;
                    }

                    final LIST_ITEM item = mAdapter.getItem(position);

                    if (viewId == R.id.GlkFileRename) {
                        // give user a chance to change name
                        AlertDialog.Builder builder = new AlertDialog.Builder(act);
                        final EditText input = new EditText(act);
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        input.setText(item.mName);
                        builder.setTitle(R.string.dialog_rename_file_title);
                        builder.setView(input);
                        builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String name = input.getText().toString();
                                // remove any symbols that can interfere with paths
                                name = name.replace("/", "").replace(".", "");
                                if (!name.equals("")) {
                                    File src = item.mFile;
                                    File dst = new File(src.getParent(), name + ext);
                                    if (dst.exists()) {
                                        Toast.makeText(act,
                                                "That name is already taken - try something else.",
                                                Toast.LENGTH_LONG).show();
                                    } else {
                                        if (src.renameTo(dst)) {
                                            tmp.set(position, new LIST_ITEM(dst, lenExt));
                                            mAdapter.notifyDataSetInvalidated();
                                        } else {
                                            Toast.makeText(act,
                                                    "Failed to rename file.",
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }
                            }
                        }).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        });
                        builder.show();
                    } else if (viewId == R.id.GlkFileDelete) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(act);
                        builder.setTitle(R.string.dialog_delete_file_title);
                        builder.setMessage("Are you sure you want to delete '" + item.mName + "'?");
                        builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (item.mFile.delete()) {
                                    tmp.remove(position);
                                    mAdapter.notifyDataSetInvalidated();
                                } else {
                                    Toast.makeText(act,
                                            "Failed to delete file.",
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        }).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // do nothing
                            }
                        });
                        builder.setIcon(android.R.drawable.ic_dialog_alert);
                        builder.show();
                    } else {
                        if (mListener != null) {
                            mListener.onDialogFinish(item.mFile.getAbsolutePath());
                            mListener = null;
                            getDialog().dismiss();
                        }
                    }
                }
            });
        }

        builder.setView(dialogView);

        // Only add the NEW button if the file will have write access
        if (fmode != GLKConstants.filemode_Read) {
            builder.setPositiveButton("NEW", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked OK button
                    mFilePath = dir + newName;
                    if (mListener != null) {
                        mListener.onDialogFinish(mFilePath);
                        mListener = null;
                    }
                }
            });
        }

        // Always add a CANCEL button
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                if (mListener != null) {
                    mListener.onDialogFinish(null);
                    mListener = null;
                }
            }
        });
    }

    public class LIST_ITEM implements Comparable<LIST_ITEM> {
        @NonNull
        final String mName;
        @NonNull
        final Date mTimeStamp;
        @NonNull
        final String mTimeStampText;
        @NonNull
        final File mFile;

        LIST_ITEM(@NonNull File f, int lenExt) {
            mFile = f;
            String fpath = f.getName();
            mName = fpath.substring(0, fpath.length() - lenExt);
            mTimeStamp = new Date(f.lastModified());
            mTimeStampText = formatter.format(mTimeStamp);
        }

        @Override
        public int compareTo(@NonNull LIST_ITEM other) {
            // sort so newer appears earlier in the list
            if (mTimeStamp.after(other.mTimeStamp))
                return -1;
            else if (mTimeStamp.before(other.mTimeStamp))
                return 1;
            return 0;
        }
    }
}
