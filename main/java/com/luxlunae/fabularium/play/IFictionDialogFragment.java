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
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.luxlunae.fabularium.AsyncLoaders;
import com.luxlunae.fabularium.MainActivity;
import com.luxlunae.fabularium.R;
import com.luxlunae.glk.GLKUtils;

/**
 * A dialog that displays metadata about a specific game; it is shown when the
 * user long-presses a game in the PLAY tab.
 */
public class IFictionDialogFragment extends DialogFragment {
    private static final int PREVIEW_IMAGE_SIZE_DP = 250;

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

        final Context context = getContext();
        setRetainInstance(true);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        @SuppressLint("InflateParams") View dialogView = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_view_game_meta, null);
        TextView tv = dialogView.findViewById(R.id.tvGameInfo);
        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append("\n");
        IFictionRecord.createFormattedString(mIfr, sb);
        sb.append("\n\n");
        tv.setText(sb);

        Bundle b = getArguments();
        Bitmap default_image = b.getParcelable(MainActivity.DEFAULT_BITMAP);
        assert default_image != null;
        int px = GLKUtils.dpToPx(PREVIEW_IMAGE_SIZE_DP, getResources().getDisplayMetrics());
        AsyncLoaders.loadBitmap(mIfr.ifids[0], tv, context, default_image, px, px);

        builder.setView(dialogView);
        builder.setPositiveButton(R.string.dialog_close, new DialogInterface.OnClickListener() {
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

    public void setIFictionRecord(@NonNull IFictionRecord r) {
        mIfr = r;
    }
}
