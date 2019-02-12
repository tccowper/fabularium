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
package com.luxlunae.fabularium;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * A dialog that can display HTML pages, loading either a page on the local file system
 * or a page on the internet, based on the provided URL.
 */
public class UrlDialogFragment extends DialogFragment {
    private String mUrl;
    private String mTitle;

    public static UrlDialogFragment newInstance() {
        return new UrlDialogFragment();
    }

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
    @SuppressLint("SetJavaScriptEnabled")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        setRetainInstance(true);

        @SuppressLint("InflateParams") WebView view = (WebView) LayoutInflater.from(getActivity()).inflate(R.layout.dialog_licences, null);
        WebSettings webSettings = view.getSettings();
        webSettings.setJavaScriptEnabled(true);
        view.loadUrl(mUrl);

        // N.B. For now we do not set the dialog title for cosmetic reasons
        return new AlertDialog.Builder(getActivity(), R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .create();
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

    @NonNull
    public UrlDialogFragment setUrl(String url) {
        mUrl = url;
        return this;
    }

    @NonNull
    public UrlDialogFragment setTitle(String title) {
        mTitle = title;
        return this;
    }
}
