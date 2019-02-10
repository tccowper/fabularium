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
package com.luxlunae.glk.model.html;

import android.content.res.Resources;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout;

import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.model.GLKModel;
import com.luxlunae.glk.model.GLKResourceManager;
import com.luxlunae.glk.model.stream.window.GLKTextWindowM;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public final class State {

    public enum HtmlType {
        HTML_TADS,
        HTML_ADRIFT
    }

    @NonNull
    final HtmlType mHtmlType;
    @NonNull
    final GLKTextWindowM mAttachedWindow;
    @NonNull
    final Point mSizeTracker;
    @NonNull
    final Stack<TableSpan> mTables;
    @NonNull
    final Stack<Layout.Alignment> mDivAligns;
    @NonNull
    final GLKModel mGLKModel;
    @NonNull
    final GLKResourceManager mResMgr;
    @NonNull
    private final StringBuilder mCurTag;
    @NonNull
    private final Map<String, String> mCurAttributes;
    @NonNull
    private final StringBuilder mCurAttrName;
    @NonNull
    private final StringBuilder mCurAttrValue;
    @NonNull
    private final StringBuilder mCharRefBuffer;
    @NonNull
    private final StringBuilder mInputHTML;
    @NonNull
    private final StringBuilder mCharRefSubstitute;
    private final boolean mShowRawHTML;
    int mBaseFontSize = 3;
    int mCurBanIDGLK = GLKConstants.NULL;
    boolean mCurBanResizeToContents = false;
    @NonNull
    WatchableStringBuilder mOutputSpanned;
    @NonNull
    Tokeniser.State mState;
    @Nullable
    Tokeniser.State mLastState;  // currently only need when there is a CR in an attribute value
    int mAdditionalChar;
    @NonNull
    Tokeniser.CRState mCRState;
    private int mRawTextStart;
    private boolean mGetRawText = false;     // if true raw HTML will be appended to mRawText
    private boolean mCollapsingWhitespace = true;   // if false, whitespaces in HTML text won't be collapsed
    private int mMark;
    private int mNextCharIndex;
    private boolean mIsStartTag;

    public State(@NonNull GLKModel m, @NonNull GLKTextWindowM w, @NonNull WatchableStringBuilder output) {
        switch (m.mTerpID) {
            case BEBEK:
                mHtmlType = HtmlType.HTML_ADRIFT;
                // Adrift games assume that whitespace
                // between HTML tags will NOT be collapsed
                // (contrary to the HTML specification)
                mCollapsingWhitespace = false;
                break;
            default:
            case TADS:
                mHtmlType = HtmlType.HTML_TADS;
                break;
        }
        mNextCharIndex = 0;
        mAttachedWindow = w;
        mSizeTracker = new Point(-1, -1);
        mState = Tokeniser.State.Data;
        mCRState = Tokeniser.CRState.CRNone;
        mMark = 0;
        mCurAttributes = new HashMap<>();
        mCurTag = new StringBuilder();
        mCurAttrName = new StringBuilder();
        mCurAttrValue = new StringBuilder();
        mCharRefBuffer = new StringBuilder();
        mCharRefSubstitute = new StringBuilder();
        mInputHTML = new StringBuilder();
        mOutputSpanned = output;
        mTables = new Stack<>();
        mDivAligns = new Stack<>();
        mShowRawHTML = m.mShowRawHTML;
        mGLKModel = m;
        mResMgr = m.mResourceMgr;
    }

    @NonNull
    Resources getResources() {
        return mGLKModel.getApplicationContext().getResources();
    }

    void captureRawText(boolean on) {
        mGetRawText = on;
        mRawTextStart = on ? mNextCharIndex : 0;
    }

    boolean isCapturingRawText() {
        return mGetRawText;
    }

    @NonNull
    String getRawText(int closingTagLen) {
        if (!mGetRawText) {
            return "";
        }
        int end = mNextCharIndex - closingTagLen - 3; // 3 additional characters are the <, / and >
        return (mRawTextStart >= 0 && end > mRawTextStart) ? mInputHTML.substring(mRawTextStart, end) : "";
    }

    void parseError(@NonNull String errMsg) {
        int s = (mNextCharIndex > 30) ? mNextCharIndex - 15 : 0;
        GLKLogger.error(errMsg + ": " + mInputHTML.substring(s, mNextCharIndex));
    }

    void mark() {
        mMark = mNextCharIndex;
    }

    void reset() {
        mNextCharIndex = mMark;
    }

    public void setOutput(@NonNull WatchableStringBuilder output) {
        mOutputSpanned = output;
    }

    public void appendInput(@NonNull String html) {
        if (html.length() > 0) {
            if (mShowRawHTML) {
                mOutputSpanned.append(html);
            } else {
                mInputHTML.append(html);
            }
        }
    }

    public void appendInput(char ch) {
        mInputHTML.append(ch);
    }

    void appendCurTag(char ch) {
        mCurTag.append(ch);
    }

    void appendCurAttrName(char ch) {
        mCurAttrName.append(ch);
    }

    void appendCurAttrValue(char ch) {
        mCurAttrValue.append(ch);
    }

    void appendCurCharRef(char ch) {
        mCharRefBuffer.append(ch);
    }

    void appendText(char ch) {
        if (mGetRawText) {
            return;
        }

        if (mCollapsingWhitespace && Character.isWhitespace(ch)) {
            // ignore whitespace that immediately follows other whitespace;
            // newlines count as spaces
            int len = mOutputSpanned.length();
            if (len != 0) {
                char prev = mOutputSpanned.charAt(len - 1);
                if (!Character.isWhitespace(prev)) {
                    mOutputSpanned.append(' ');
                }
            }
        } else {
            mOutputSpanned.append(ch);
        }
    }

    boolean matchCRSymbol() {
        String ret = CharRef.CREFS.get(mCharRefBuffer.toString());
        if (ret != null) {
            mCharRefSubstitute.setLength(0);
            mCharRefSubstitute.append(ret);
            return true;
        }
        return false;
    }

    boolean matchCRNumber(boolean hex) {
        // TODO: check if it's in the prohibited table, which should return false
        try {
            char c = hex ?
                    (char) Integer.parseInt(mCharRefBuffer.toString(), 16) :
                    (char) Integer.parseInt(mCharRefBuffer.toString());
            mCharRefSubstitute.setLength(0);
            mCharRefSubstitute.append(c);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    void flushCRSubstitute() {
        mCharRefBuffer.setLength(0);

        if (mGetRawText) {
            return;
        }

        if (mState == Tokeniser.State.Data) {
            mOutputSpanned.append(mCharRefSubstitute);
        } else {
            mCurAttrValue.append(mCharRefSubstitute);
        }
    }

    int getCRTextLength() {
        return mCharRefBuffer.length();
    }

    @NonNull
    String getCRText() {
        return mCharRefBuffer.toString();
    }

    void clearCRText() {
        mCharRefBuffer.setLength(0);
    }

    void newStartTag() {
        mCurTag.setLength(0);
        mCurAttrName.setLength(0);
        mCurAttrValue.setLength(0);
        mCurAttributes.clear();
        mIsStartTag = true;
    }

    void newEndTag() {
        mCurTag.setLength(0);
        mCurAttrName.setLength(0);
        mCurAttrValue.setLength(0);
        mCurAttributes.clear();
        mIsStartTag = false;
    }

    void newAttribute() {
        storeCurrentAttribute();
        mCurAttrName.setLength(0);
        mCurAttrValue.setLength(0);
    }

    // The output of the tokenization step is a series of zero or more of the
    // following tokens: DOCTYPE, start tag, end tag, comment, character, end-of-file.
    void emitTag() {
        storeCurrentAttribute();
        String tag = mCurTag.toString();
        if (mIsStartTag) {
            Parser.startElement(tag, mCurAttributes, this);
        } else {
            Parser.endElement(tag, this);
        }
    }

    void emitEOF() {
        // currently we do nothing
    }

    int consumeNextInputChar() {
        if (mNextCharIndex < mInputHTML.length()) {
            return (int) mInputHTML.charAt(mNextCharIndex++);
        }
        return Tokeniser.EOF;
    }

    int peekNextChar() {
        if (mNextCharIndex < mInputHTML.length()) {
            return mInputHTML.charAt(mNextCharIndex);
        }
        return Tokeniser.EOF;
    }

    void unconsume() {
        if (mNextCharIndex > 0) {
            mNextCharIndex--;
        }
    }

    private void storeCurrentAttribute() {
        if (mCurAttrName.length() > 0) {
            if (mCurAttrValue.length() > 0) {
                mCurAttributes.put(mCurAttrName.toString(), mCurAttrValue.toString());
            } else {
                mCurAttributes.put(mCurAttrName.toString(), "");
            }
        }
    }
}
