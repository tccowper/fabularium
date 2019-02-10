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

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.Layout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

public class TextEditorView extends ScrollView {
    private final TextPaint mLineNumPaint = new TextPaint();
    private EditText mEditText;
    private int mLineNumMarginWidth = 0;
    private boolean mShowLineNumbers = false;

    public TextEditorView(Context context) {
        super(context, null);
        init(context);
    }

    public TextEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private static int spToPx(float sp, @NonNull Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.text_editor, this);
        mEditText = findViewById(R.id.textEditor);
        mLineNumPaint.set(mEditText.getPaint());
        mLineNumPaint.setColor(Color.LTGRAY);
        mLineNumPaint.setTextSize(spToPx(14f, getContext()));
    }

    public Editable getText() {
        return mEditText.getText();
    }

    public void setText(@NonNull final CharSequence text) {
        // See https://issuetracker.google.com/issues/66394813
        // Google has confirmed that there is a bug in Android 8 (O), which
        // doesn't seem to affect Android 4, 5, 6 or 7. The bug is nasty:
        // if setText is called on an EditText widget using a string that
        // has >= 300,000 characters, the app crashes with a SIGABRT in native code.
        // Google says they will not fix this in Android 8.0 as it is already fixed
        // in their internal development build (presumably that means it will be fixed
        // from Android 9 onwards).
        //
        // In our testing, the crash occurs even with strings that have 180,000 characters.
        // 170,000 chars appears to be safe.
        //
        // So we need to workaround the problem if this app is running on Android 8.0, but
        // I haven't yet been able to find any solution other than truncating the text :-(
        int len = text.length();

        try {
            if (Build.VERSION.SDK_INT == 26 && len > 170000) {
                // Android O and string may be large enough to cause problems
                // Alert user and truncate string to safe limit
                // TODO: Is there a way to workaround this bug and actually show the full file?
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Android O bug")
                        .setMessage(R.string.ANDROID_SET_TEXT_BUG_EXPLANATION)
                        .setPositiveButton("TRUNCATE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mEditText.setText(text.subSequence(0, 170000));
                            }
                        })
                        .setNegativeButton("DON'T TRUNCATE (MIGHT CRASH)", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mEditText.setText(text);
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();

            } else {
                // setText should work OK irrespective of string size
                mEditText.setText(text);
            }
        } catch (RuntimeException e) {
            // We've been occasionally receiving TransactionTooLargeException crash reports when
            // setText gets called above, so try to catch those gracefully here and tell the user what's happening
            Toast.makeText(getContext(),
                    "I can't load the file into the text editor as it is too large. Sorry!",
                    Toast.LENGTH_LONG).show();
        } catch (OutOfMemoryError e) {
            // We've also been occasionally getting OutOfMemoryError crash reports...
            Toast.makeText(getContext(),
                    "You don't have sufficient free memory available to load this file into the text editor. Sorry!",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void toggleLineNumbers() {
        mShowLineNumbers = !mShowLineNumbers;
        if (mShowLineNumbers) {
            // Make the line number margin width sufficient to go up to 99,999 lines (in base font) plus a few pixels of padding
            mLineNumMarginWidth = (int) (mEditText.getPaint().measureText("0") * 6f);
            setPadding(mLineNumMarginWidth, 10, 10, 10);
        } else {
            mLineNumMarginWidth = 0;
            setPadding(0, 0, 0, 0);
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        final int scrollX = getScrollX();
        final int scrollY = getScrollY();

        // render text
        super.onDraw(canvas);

        // render line numbers
        if (mShowLineNumbers) {
            Layout l = mEditText.getLayout();
            if (l != null) {
                int line = l.getLineForVertical(scrollY);
                int voffset = getPaddingTop() + mEditText.getTotalPaddingTop();
                int startY = l.getLineBaseline(line) + voffset;
                int endY = startY + getHeight();
                int endI = l.getLineCount();

                for (int i = line, y = startY; y < endY && i < endI; i++) {
                    canvas.drawText(String.valueOf(i + 1), scrollX + 5f, (float) y, mLineNumPaint);
                    if (i < endI - 1) {
                        y = l.getLineBaseline(i + 1) + voffset;
                    }
                }

                int startX = (mLineNumMarginWidth * 5 / 6 + 1);

                canvas.drawLine(scrollX + startX, l.getLineTop(line), scrollX + startX, endY, mLineNumPaint);
            }
        }
    }
}
