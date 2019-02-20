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
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;

import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.GLKUtils;
import com.luxlunae.glk.controller.GLKController;
import com.luxlunae.glk.controller.GLKEvent;
import com.luxlunae.glk.model.GLKDrawable;
import com.luxlunae.glk.model.GLKModel;
import com.luxlunae.glk.model.stream.GLKOutputStream;
import com.luxlunae.glk.model.stream.window.GLKTextWindowM;
import com.luxlunae.glk.model.style.GLKHyperlinkSpan;
import com.luxlunae.glk.model.style.GLKStyleSpan;

import java.util.EmptyStackException;
import java.util.Map;

import static com.luxlunae.glk.controller.GLKController.glk_request_char_event;
import static com.luxlunae.glk.controller.GLKController.glk_select;
import static com.luxlunae.glk.controller.GLKController.glk_window_clear;
import static com.luxlunae.glk.model.GLKModel.TADS_COLOR_ALINK;
import static com.luxlunae.glk.model.GLKModel.TADS_COLOR_BGCOLOR;
import static com.luxlunae.glk.model.GLKModel.TADS_COLOR_HLINK;
import static com.luxlunae.glk.model.GLKModel.TADS_COLOR_INPUT;
import static com.luxlunae.glk.model.GLKModel.TADS_COLOR_LINK;
import static com.luxlunae.glk.model.GLKModel.TADS_COLOR_STATUSBG;
import static com.luxlunae.glk.model.GLKModel.TADS_COLOR_STATUSTEXT;
import static com.luxlunae.glk.model.GLKModel.TADS_COLOR_TEXT;
import static com.luxlunae.glk.model.GLKModel.TADS_FONT_INPUT;
import static com.luxlunae.glk.model.GLKModel.TADS_FONT_SANS;
import static com.luxlunae.glk.model.GLKModel.TADS_FONT_SCRIPT;
import static com.luxlunae.glk.model.GLKModel.TADS_FONT_SERIF;
import static com.luxlunae.glk.model.GLKModel.TADS_FONT_TYPEWRITER;

/**
 * This class processes HTML strings into displayable styled text.
 * We support all the tags permitted by TADS HTML (which is based on
 * HTML 3.2 - see https://www.w3.org/TR/REC-html32.html).
 * <p/>
 * That is, we support the following HTML 3.2 tags:
 * <p/>
 * TITLE, BODY, H1-H6, ADDRESS, P, UL, OL, LI, DT, DD, PRE, XMP, LISTING, PLAINTEXT,
 * DIV, CENTER, BLOCKQUOTE, HR, TABLE, CAPTION, TR, TD, TT, I, B, U, STRIKE, BIG,
 * SMALL, SUB, SUP, EM, STRONG, DFN, CODE, SAMP, KBD, VAR, CITE, A, IMG, FONT,
 * BASEFONT, BR, MAP
 * <p/>
 * And the following HTML 3.0 additions:
 * <p/>
 * BQ, CREDIT, NOBR, BANNER, TAB, LH
 * <p/>
 * And we support the following TADS-specific tags (see
 * http://www.tads.org/t3doc/doc/htmltads/deviate.htm for more info on the deviations):
 * <p/>
 * SOUND, ABOUTBOX, WRAP, Q
 */
public final class Parser {

    private static final boolean DEBUG_TADS_PARSER = false;

    private static final float[] HEADER_SIZES = {
            1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1f,
    };

    private static final float[] FONT_SIZES = {
            // in %s of the user-selected default font size
            // the third element should always be 100% to ensure that
            // we use the user selected size by default
            0.8f, 0.9f, 1f, 1.1f, 1.2f, 1.3f, 1.4f
    };

    private Parser() {
        // prevent instantiation
    }

    private static void endPara(@NonNull SpannableStringBuilder text) {
        Object obj = GLKStyleSpan.getLast(text, Para.class);
        if (obj != null) {
            Para p = (Para) obj;
            int len = text.length();
            int where = text.getSpanStart(obj);
            text.removeSpan(obj);
            if (where < len) {
                if (p.mAlign != null) {
                    text.setSpan(new AlignmentSpan.Standard(p.mAlign), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    private static void endTable(@NonNull SpannableStringBuilder text, TableSpan t) {
        Object obj = GLKStyleSpan.getLast(text, Table.class);
        if (obj != null) {
            int len = text.length();
            int where = text.getSpanStart(obj);
            text.removeSpan(obj);
            if (where < len) {
                text.setSpan(t, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private static void endCell(@NonNull SpannableStringBuilder text, @NonNull TableSpan t) {
        Object obj = GLKStyleSpan.getLast(text, TableCell.class);
        if (obj != null) {
            TableCell tc = (TableCell) obj;
            int len = text.length();
            int where = text.getSpanStart(obj);
            text.removeSpan(obj);
            Spanned str = null;
            if (where < len) {
                str = (Spanned) text.subSequence(where, len);
                text.delete(where, len);
            }
            t.newCell(str, tc.mIsHeader, tc.mNowrap, tc.mRowspan, tc.mColspan, tc.mAlign, tc.mVAlign, tc.mWidth, tc.mHeight, tc.mBGColor, tc.mBackground);
        }
    }

    private static void handleBody(@NonNull State state, @NonNull Map<String, String> attributes) {
        // TADSHtml allows the <BODY> link to appear multiple times per document,
        // unlike with standard HTML renderers. Each time <BODY> appears,
        // any attributes (such as the background color or text color) replace
        // the previous settings for those attributes; these settings affect the entire window.

        // URL for an image that will be used to tile the document background:
        String bgImage = attributes.get("background");

        // Background color for the document body:
        String bgColor = attributes.get("bgcolor");

        // Color to stroke the document's text:
        String fgColor = attributes.get("text");

        // Color to stroke unvisited hypertext links:
        String link = attributes.get("link");
        int col = getTADSColor(link, state);
        if (col != Color.TRANSPARENT) {
            state.mGLKModel.mHyperlinkColor = col;
        }

        // Color to stroke visited hypertext links:
        String vlink = attributes.get("vlink");

        // Color to stroke hypertext links when the user clicks the link:
        String alink = attributes.get("alink");

        // TADS-specific:  color to use for "hovering" over a hyperlink.
        // This is irrelevant for touch devices; we don't support it.
        String hlink = attributes.get("hlink");

        // TADS-specific: color for command-line input text:
        String input = attributes.get("input");

        // Set any background colour first, before setting a background image
        GLKTextWindowM w = state.mAttachedWindow;
        if (bgColor != null) {
            w.postClearWindowBackground(getTADSColor(bgColor, state));
        }
        if (bgImage != null) {
            // N.B. We don't need to rescale the drawable to the window size
            // as this is done automatically when it is set to the window's
            // background (and the latter method potentially uses much less memory).
            GLKDrawable d = state.mResMgr.getImage(bgImage, 0, 0, true);
            if (d != null) {
                w.postSetBackground(d);
            }
        }

        // Set the foreground colour if specified
        if (fgColor != null) {
            if (fgColor.toLowerCase().equals("statustext") &&
                    state.mGLKModel.mTADSStatusStyle != null) {
                // don't just change the color, change the base font as well
                w.setNormalFont(state.mGLKModel.mTADSStatusStyle);
            } else {
                w.setTextColor(getTADSColor(fgColor, state));
            }
        }
    }

    private static void handleBr(@NonNull State state, @NonNull WatchableStringBuilder text, @NonNull Map<String, String> attributes) {
        // <BR HEIGHT=number> will display the given number of blank lines. Using HEIGHT=0 will make
        // this tag behave the same way that the conventional TADS "\n" sequence does: it ends the
        // current line, but does not add any new blank lines, even if repeated.
        int N = getHTMLInt(attributes.get("height"), -1);

        if (N == -1) {
            text.append("\n");
        } else {
            // Work out if we are at the start of the line
            // TODO: there must be a more efficient way of figuring this out...
            boolean startOfLine = true;     // assume at the start of line unless can prove otherwise...
            int len = text.length();
            CharSequence line = GLKUtils.getLine(len - 1, 0, len, text,
                    state.mAttachedWindow.mBaseTextPaint, state.mAttachedWindow.mWidthGLK_PX);
            if (line != null) {
                String s = line.toString();
                if (!s.endsWith("\n")) {
                    s = s.trim();
                    if (!s.equals("")) {
                        startOfLine = false;
                    }
                }
            }
            if (!startOfLine) {
                if (N == 0) {
                    text.setZeroBreak();
                } else {
                    text.append("\n");
                }
            }
            for (int i = 0; i < N; i++) {
                text.append("\n");
            }
        }
    }

    private static void handleImg(@NonNull State state, @NonNull SpannableStringBuilder text,
                                  @NonNull Map<String, String> attributes) {
        // Images can be positioned vertically relative to the current textline or floated to the left or
        // right. See BR with the CLEAR attribute for control over textflow.
        String src = attributes.get("src");
        int h = getHTMLInt(attributes.get("height"), 0);
        int w = getHTMLInt(attributes.get("width"), 0);

        // This specifies how the image is positioned relative to the current textline in which it occurs:
        //
        //     align=top positions the top of the image with the top of the current text line. User agents
        //     vary in how they interpret this. Some only take into account what has occurred on the text line
        //     prior to the IMG element and ignore what happens after it.
        //
        //     align=middle aligns the middle of the image with the baseline for the current textline.
        //
        //     align=bottom is the default and aligns the bottom of the image with the baseline.
        //
        //     align=left floats the image to the current left margin, temporarily changing this margin,
        //     so that subsequent text is flowed along the image's righthand side. The rendering depends
        //     on whether there is any left aligned text or images that appear earlier than the current
        //     image in the markup. Such text (but not images) generally forces left aligned images to
        //     wrap to a new line, with the subsequent text continuing on the former line.
        //
        //     align=right floats the image to the current right margin, temporarily changing this margin, so
        //     that subsequent text is flowed along the image's lefthand side. The rendering depends on whether
        //     there is any right aligned text or images that appear earlier than the current image in the markup.
        //     Such text (but not images) generally forces right aligned images to wrap to a new line, with the
        //     subsequent text continuing on the former line.
        String align = attributes.get("align");

        if (src == null) return;

        GLKDrawable d = state.mResMgr.getImage(src, w, h, true);
        if (d == null) {
            /* TODO - use a better unknown image */
            GLKLogger.error("Could not find TADS image: " + src);
            return;
            /*d = Resources.getSystem().getDrawable(R.drawable.orpheus100px);
            if (d == null) return;
            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());*/
        }

        // don't allow image width to be wider than display
        GLKUtils.ensureDrawableWithinWidth(state.mAttachedWindow.mWidthGLK_PX, d);
        text.append("\uFFFC");
        int len = text.length();
        text.setSpan(new HTMLImage(d, src, align), len - 1, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static void startFont(@NonNull SpannableStringBuilder text,
                                  @NonNull Map<String, String> attributes, int baseFontSize) {

        // Used to set the color to stroke the text. Colors are given as RGB in hexadecimal notation or as one of 16 widely understood color
        // names defined as per the BGCOLOR attribute on the BODY element.
        String color = attributes.get("color");
        if (color == null) {
            // Try the British English spelling
            color = attributes.get("colour");
        }

        // Used to set the background color of the text. Colors are given as RGB in hexadecimal notation or as one of 16 widely understood color
        // names defined as per the BGCOLOR attribute on the BODY element.
        String bgcolor = attributes.get("bgcolor");
        if (bgcolor == null) {
            // Try the British English spelling
            bgcolor = attributes.get("bgcolour");
        }

        // This sets the font size for the contents of the font element. You can set size to an integer ranging from
        // 1 to 7 for an absolute font size, or specify a relative font size with a signed integer value, e.g. size="+1"
        // or size="-2". This is mapped to an absolute font size by adding the current base font size as set by the
        // BASEFONT element (see below).
        String size = attributes.get("size");

        // Not part of the HTML 3.2 standard, but supported by TADS Html.
        // A comma separated list of font names in order of preference.
        String face = attributes.get("face");

        int len = text.length();
        int abs_size = 0;

        // Work out the new size (can be relative or absolute)
        if (size != null) {
            // absolute font sizes range from 1..7
            // the change stays effective until we hit a closing </font> tag
            char c = size.charAt(0);
            boolean relSize = (c == '+' || c == '-');
            int t = getHTMLInt(size, 0);

            if (t != 0) {
                abs_size = relSize ? baseFontSize + t : t;
                if (abs_size > 7) {
                    abs_size = 7;
                } else if (abs_size < 1) {
                    abs_size = 1;
                }
            }
        }

        text.setSpan(new Font(color, bgcolor, face, abs_size), len, len, Spannable.SPAN_MARK_MARK);
    }

    private static int endFont(@NonNull State state, @NonNull SpannableStringBuilder text) {
        Object obj = GLKStyleSpan.getLast(text, Font.class);
        if (obj != null) {
            Font f = (Font) obj;
            int len = text.length();
            int where = text.getSpanStart(obj);
            text.removeSpan(obj);
            if (where < len) {
                // Set the size, if any
                if (f.mFontSize > 0) {
                    text.setSpan(new RelativeSizeSpan(FONT_SIZES[f.mFontSize - 1]),
                            where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                // Set the colour, if any
                if (!TextUtils.isEmpty(f.mColor)) {
                    text.setSpan(new ForegroundColorSpan(getTADSColor(f.mColor, state)), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                // Set the background colour, if any
                if (!TextUtils.isEmpty(f.mBGColor)) {
                    text.setSpan(new BackgroundColorSpan(getTADSColor(f.mBGColor, state)), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                // Set the face, if any
                // We stop on the first successful face match
                if (f.mFace != null) {
                    String[] faces = f.mFace.split(",");
                    Typeface family;
                    for (String face : faces) {
                        family = getTADSFontFamily(face, state);
                        if (family != null) {
                            // Success!
                            text.setSpan(new AnyFontTypefaceSpan(family), where, len,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        }
                    }
                }
            }
            return f.mFontSize;
        }
        return 0;
    }

    private static void endDiv(@NonNull SpannableStringBuilder text) {
        Object obj = GLKStyleSpan.getLast(text, Div.class);
        if (obj != null) {
            Div d = (Div) obj;
            int len = text.length();
            int where = text.getSpanStart(obj);
            text.removeSpan(obj);
            if (where < len) {
                text.setSpan(new AlignmentSpan.Standard(d.mAlign), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private static void startA(@NonNull SpannableStringBuilder text, @NonNull Map<String, String> attributes) {
        GLKStyleSpan.start(text, new Href(attributes.get("href"),
                getHTMLBoolean(attributes.get("append"), false),
                getHTMLBoolean(attributes.get("mNoEnter"), false),
                getHTMLBoolean(attributes.get("plain"), false)));
    }

    private static void endA(@NonNull SpannableStringBuilder text, @NonNull GLKTextWindowM rootWin, @NonNull State state) {
        // win is the window to which input should be posted if this link is clicked
        Object obj = GLKStyleSpan.getLast(text, Href.class);
        if (obj != null) {
            Href h = (Href) obj;
            int len = text.length();
            int where = text.getSpanStart(obj);
            text.removeSpan(obj);
            if (where < len) {
                // We have visible link text
                if (h.mHref != null) {
                    // We also have a href
                    text.setSpan(new GLKHyperlinkSpan(h.mHref, state.mGLKModel.mHyperlinkColor,
                            h.mAppend, h.mNoEnter, h.mPlain, rootWin), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    // We don't have a href (TODO: check what exactly we should do in this situation)
                    GLKLogger.warn("TADS HTML: warning - hyperlink has text but no href - text is '" + text.subSequence(where, len) + "'");
                    //  text.setSpan(new GLKHyperlinkSpan(String.valueOf(text.subSequence(where, len))), where, len,
                    //         Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    private static void endHeader(@NonNull SpannableStringBuilder text) {
        Object obj = GLKStyleSpan.getLast(text, Header.class);
        if (obj != null) {
            Header h = (Header) obj;
            int len = text.length();
            int where = text.getSpanStart(obj);
            text.removeSpan(obj);

            // Back off not to change only the text, not the blank line.
            while (len > where && text.charAt(len - 1) == '\n') {
                len--;
            }

            if (where < len) {
                text.setSpan(new RelativeSizeSpan(HEADER_SIZES[h.mLevel]),
                        where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                text.setSpan(new StyleSpan(Typeface.BOLD),
                        where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (h.mAlign != null) {
                    text.setSpan(new AlignmentSpan.Standard(h.mAlign), where, len,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    public static void process(@NonNull State htmlState) {
        while (htmlState.peekNextChar() != Tokeniser.EOF) {
            Tokeniser.parseNextToken(htmlState);
        }
    }

    /**
     * Parses an HTML start tag.
     *
     * @param tag        - the tag to process
     * @param attributes - the tag's attributes
     * @param state      - the state object that will be modified to capture the results
     */
    static void startElement(@NonNull final String tag,
                             @NonNull final Map<String, String> attributes,
                             @NonNull State state) {
        // We assume that all tags provided by the parser are lower case.
        // We recognise:
        //    TITLE, BODY, H1-H6, ADDRESS, P, UL, OL, LI, DT, DD, PRE, XMP, LISTING, PLAINTEXT,
        //    DIV, CENTER, BLOCKQUOTE, HR, TABLE, CAPTION, TR, TH, TD, TT, I, B, U, STRIKE, BIG,
        //    SMALL, SUB, SUP, EM, STRONG, DFN, CODE, SAMP, KBD, VAR, CITE, A, IMG, FONT,
        //    BASEFONT, BR, MAP, BQ, NOBR, BANNER, TAB, LH, SOUND, ABOUTBOX, WRAP, Q
        //Logger.error("start tag: " + tag + ", attributes = " + attributes.toString());

        // Process special container tags first
        GLKModel m = state.mGLKModel;
        switch (tag) {
            case "title":
                state.captureRawText(true);
                return;
            case "banner":
                boolean remove = getHTMLBoolean(attributes.get("remove"), false);
                boolean removeAll = getHTMLBoolean(attributes.get("removeall"), false);

                if (remove) {
                    // not a banner container
                    GLKController.tadsban_html_delete(m, attributes.get("id"));
                } else if (removeAll) {
                    // not a banner container
                    GLKController.tadsban_html_delete_all(m);
                } else {
                    // a banner container
                    String id = attributes.get("id");
                    state.mCurBanIDGLK = GLKController.tadsban_html_get_banner(m, id);
                    int align = getTADSBannerAlignment(attributes.get("align"));
                    boolean bannerExists = state.mCurBanIDGLK != GLKConstants.NULL;
                    boolean border = attributes.get("border") != null;

                    String sz = (align == GLKConstants.TADS_BANNER_ALIGN_TOP || align == GLKConstants.TADS_BANNER_ALIGN_BOTTOM) ?
                            attributes.get("height") : attributes.get("width");

                    // if the banner exists, clear anything that is currently displayed in it
                    if (bannerExists) {
                        GLKController.tadsban_clear(m, state.mCurBanIDGLK);
                    }

                    // now do any applicable resizing
                    if (sz != null && sz.toLowerCase().equals("previous")) {
                        // if the banner doesn't exist we size to contents;
                        // if it does exist, we leave the size unchanged
                        // FIXME: at the moment we size to contents in both cases because
                        // some windows seem to set a size that is far too small in the first instance
                        state.mCurBanResizeToContents = true;
                        if (!bannerExists) {
                            state.mCurBanIDGLK = GLKController.tadsban_html_create(m, id, align, 0, GLKConstants.TADS_BANNER_SIZE_ABS, border);
                        }
                    } else {
                        Resources r = state.getResources();
                        HTMLLength size = new HTMLLength(sz, r);
                        int winSize;
                        int szUnits;
                        if (size.mValue == HTMLLength.SIZE_TO_CONTENTS) {
                            // set an initial temporary fixed size, we'll resize properly (if applicable)
                            // after the text has been sent
                            winSize = 0;
                            szUnits = GLKConstants.TADS_BANNER_SIZE_ABS;
                            state.mCurBanResizeToContents = true;
                        } else {
                            szUnits = size.mIsPercent ? GLKConstants.TADS_BANNER_SIZE_PCT : GLKConstants.TADS_BANNER_SIZE_ABS;
                            winSize = (size.mRawValue == 1) ? 0 : size.mValue;
                        }

                        if (!bannerExists) {
                            // need to create the banner
                            state.mCurBanIDGLK = GLKController.tadsban_html_create(m, id, align, winSize, szUnits, border);
                        } else {
                            // resize an existing banner
                            if (size.mValue != HTMLLength.SIZE_TO_CONTENTS) {
                                GLKController.tadsban_set_size(m, state.mCurBanIDGLK, winSize, szUnits, 0);
                            }
                        }
                    }
                    state.captureRawText(true);
                }
                return;
            case "aboutbox":
                state.captureRawText(true);
                return;
            case "pre":
            case "xmp":
            case "listing":
            case "plaintext":
                // N.B. XMP, LISTING and PLAINTEXT are obsolete tags for
                // preformatted text that predate the introduction of PRE.
                // We support them just in case...
                state.captureRawText(true);
                break;
        }

        // Other tags are only processed if we're not certain containers
        if (state.isCapturingRawText()) {
            return;
        }

        WatchableStringBuilder outputString = state.mOutputSpanned;
        switch (tag) {
            case "body":
                handleBody(state, attributes);
                break;
            case "address":  // we treat <address> as a synonym for <p>
            case "p": {
                outputString.handlePara();
                String a = attributes.get("align");
                Layout.Alignment align = null;
                if (a != null && state.mDivAligns.size() == 0) {
                    // only set the alignment if it is explicitly stated
                    // and we haven't inherited any alignments from parent DIV
                    // elements.
                    align = getHTMLHAlign(a);
                }
                GLKStyleSpan.start(outputString, new Para(align));
                break;
            }
            case "ul":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement tag " + tag);
                break;
            case "ol":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement tag " + tag);
                break;
            case "li":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement tag " + tag);
                break;
            case "dt":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement tag " + tag);
                break;
            case "dd":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement tag " + tag);
                break;
            case "div": {
                outputString.handlePara();
                Layout.Alignment align = getHTMLHAlign(attributes.get("align"));
                state.mDivAligns.push(align);
                GLKStyleSpan.start(outputString, new Div(align));
                break;
            }
            case "center":
                // CENTER is a shorthand for DIV with ALIGN=CENTER
                outputString.handlePara();
                state.mDivAligns.push(Layout.Alignment.ALIGN_CENTER);
                GLKStyleSpan.start(outputString, new Div(Layout.Alignment.ALIGN_CENTER));
                break;
            case "blockquote":
            case "bq":  // bq is a synonym for blockquote
                outputString.handlePara();
                GLKStyleSpan.start(outputString, new Blockquote());
                break;
            case "hr": {
                outputString.handlePara();
                outputString.append("\uFFFC");
                int len2 = outputString.length();
                Resources r2 = state.getResources();
                Layout.Alignment align = state.mDivAligns.size() > 0 ? state.mDivAligns.peek() : getHTMLHAlign(attributes.get("align"));
                outputString.setSpan(new HRuleSpan(align,
                                getHTMLBoolean(attributes.get("noshade"), false),
                                getHTMLPixels(attributes.get("size"), r2, 4),
                                new HTMLLength(attributes.get("width"), r2), state.mSizeTracker, state.mAttachedWindow),
                        len2 - 1, len2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                outputString.handlePara();
                break;
            }
            case "table":
                if (state.mTables.size() == 0) {
                    outputString.handlePara();
                }
                GLKStyleSpan.start(outputString, new Table());
                outputString.append("\uFFFC");
                Resources r = state.getResources();
                int space = getHTMLPixels(attributes.get("cellspacing"), r, TableSpan.DEFAULT_CELL_SPACING);
                int pad = getHTMLPixels(attributes.get("cellpadding"), r, TableSpan.DEFAULT_CELL_PADDING);
                String border = attributes.get("border");

                state.mTables.push(new TableSpan(getHTMLHAlign(attributes.get("align")),
                        new HTMLLength(attributes.get("width"), r),
                        border != null ? getHTMLInt(border, 1) : 0,
                        space, pad,
                        getTADSColor(attributes.get("bgcolor"), state),
                        attributes.get("background"),
                        state.mAttachedWindow, state.mSizeTracker, state.mResMgr));
                return;
            case "caption":
                GLKLogger.error("TADSHTML: FIXME: Implement tag " + tag);
                break;
            case "tr": {
                if (state.mTables.size() > 0) {
                    TableSpan t = state.mTables.peek();
                    if (t.mOpenCellTags > 0) {
                        endCell(outputString, t);
                        t.mOpenCellTags--;
                    }
                    t.newRow(getHTMLHAlign(attributes.get("align")), getHTMLVAlign(attributes.get("valign")),
                            getTADSColor(attributes.get("bgcolor"), state));
                }
                break;
            }
            case "th":
                if (state.mTables.size() > 0) {
                    TableSpan t = state.mTables.peek();
                    if (t.mOpenCellTags > 0) {
                        endCell(outputString, t);
                        t.mOpenCellTags--;
                    }
                    t.mOpenCellTags++;
                    Resources res = state.getResources();
                    GLKStyleSpan.start(outputString, new TableCell(true, getHTMLBoolean(attributes.get("nowrap"), false),
                            getHTMLInt(attributes.get("rowspan"), 1), getHTMLInt(attributes.get("colspan"), 1),
                            getHTMLHAlign(attributes.get("align")), getHTMLVAlign(attributes.get("valign")),
                            new HTMLLength(attributes.get("width"), res), getHTMLPixels(attributes.get("height"), res, HTMLLength.SIZE_TO_CONTENTS),
                            getTADSColor(attributes.get("bgcolor"), state), attributes.get("background")));
                }
                break;
            case "td": {
                if (state.mTables.size() > 0) {
                    TableSpan t = state.mTables.peek();
                    if (t.mOpenCellTags > 0) {
                        endCell(outputString, t);
                        t.mOpenCellTags--;
                    }
                    t.mOpenCellTags++;
                    Resources res = state.getResources();
                    GLKStyleSpan.start(outputString, new TableCell(false, getHTMLBoolean(attributes.get("nowrap"), false),
                            getHTMLInt(attributes.get("rowspan"), 1), getHTMLInt(attributes.get("colspan"), 1),
                            getHTMLHAlign(attributes.get("align")), getHTMLVAlign(attributes.get("valign")),
                            new HTMLLength(attributes.get("width"), res), getHTMLPixels(attributes.get("height"), res, HTMLLength.SIZE_TO_CONTENTS),
                            getTADSColor(attributes.get("bgcolor"), state), attributes.get("background")));
                }
                break;
            }
            case "tt":
                GLKStyleSpan.start(outputString, new Monospace());
                break;
            case "i":
                GLKStyleSpan.start(outputString, new Italic());
                break;
            case "b":
                GLKStyleSpan.start(outputString, new Bold());
                break;
            case "u":
                GLKStyleSpan.start(outputString, new Underline());
                break;
            case "strike":
                GLKStyleSpan.start(outputString, new Strike());
                break;
            case "big":
                GLKStyleSpan.start(outputString, new Big());
                break;
            case "small":
                GLKStyleSpan.start(outputString, new Small());
                break;
            case "sub":
                GLKStyleSpan.start(outputString, new Sub());
                break;
            case "sup":
                GLKStyleSpan.start(outputString, new Super());
                break;
            case "em":
                GLKStyleSpan.start(outputString, new Italic());
                break;
            case "strong":
                GLKStyleSpan.start(outputString, new Bold());
                break;
            case "dfn":
                GLKStyleSpan.start(outputString, new Italic());
                break;
            case "code":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement tag " + tag);
                break;
            case "samp":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement tag " + tag);
                break;
            case "kbd":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement tag " + tag);
                break;
            case "var":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement tag " + tag);
                break;
            case "cite":
                GLKStyleSpan.start(outputString, new Italic());
                break;
            case "a":
                startA(outputString, attributes);
                break;
            case "img":
                handleImg(state, outputString, attributes);
                break;
            case "font":
                // this next line shouldn't be necessary, but some games
                // (e.g. Beetmonger's Journal) have broken HTML code - they
                // don't always include end font tags (which is mandatory - see HTML 3.2 standard),
                // assuming starting a new font will end the previous font (and in such cases seem to
                // assume the font size from the previous font stays in effect unless explicitly modified)
                int sz = endFont(state, outputString);
                startFont(outputString, attributes, sz == 0 ? state.mBaseFontSize : sz);
                break;
            case "basefont":
                // "Used to set the base font size. BASEFONT is an empty element so the end tag is forbidden.
                // The SIZE attribute is an integer value ranging from 1 to 7. The base font size applies to
                // the normal and preformatted text but not to headings, except where these are modified
                // using the FONT element with a relative font size."
                int size = getHTMLInt(attributes.get("size"), 3);
                if (size > 7) {
                    size = 7;
                } else if (size < 1) {
                    size = 1;
                }
                state.mBaseFontSize = size;
                break;
            case "br":
                handleBr(state, outputString, attributes);
                break;
            case "map":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement tag " + tag);
                break;
            case "nobr":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement tag " + tag);
                break;
            case "tab": {
                String id = attributes.get("id");
                String to = attributes.get("to");
                Layout.Alignment align = getHTMLHAlign(attributes.get("align"));
                int indent = getHTMLInt(attributes.get("indent"), 0);
                int multiple = getHTMLInt(attributes.get("multiple"), 0);
                outputString.append("\uFFFC");
                int len = outputString.length();
                outputString.setSpan(new TabSpan(id, to, align, indent, multiple, state), len - 1, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            }
            case "lh":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement tag " + tag);
                break;
            case "sound": {
                String t = attributes.get("repeat");
                int repeat = (t != null && t.toLowerCase().equals("loop")) ? -1 : getHTMLInt(t, 1);
                GLKController.playSoundTADS(m, attributes.get("src"), attributes.get("layer"), getHTMLInt(attributes.get("random"), -1),
                        repeat, attributes.get("sequence"), getHTMLBoolean(attributes.get("interrupt"), false),
                        attributes.get("cancel"), attributes.get("alt"), attributes.get("fadein"), attributes.get("fadeout"),
                        getHTMLInt(attributes.get("volume"), -1));
                break;
            }
            case "audio": {
                // ADRIFT sound tags
                // Form: <audio ((play )?src=(""(?<src>(https?:(//)[\w\d:#:%/;$()~_\?\+-=\\\.&]*?|[a-zA-Z]:(\\[\w_\. -~\[\]]+?)*?\.\w+?))"")?|pause|stop)( channel=(?<channel>\d))?( loop=(?<loop>(Y|N)))?>
                String loop = attributes.get("loop");
                int repeat = 1;
                if (loop != null) {
                    // -1 means loop forever
                    repeat = (loop.toLowerCase().equals("y")) ? -1 : 1;
                }

                // TODO: don't ignore the channel attribute:
                String chan = attributes.get("channel");

                int startStop = 1;
                String pause = attributes.get("pause");
                if (pause == null) {
                    String stop = attributes.get("stop");
                    if (stop != null) {
                        startStop = 0;
                    }
                } else {
                    startStop = 2;
                }

                GLKController.playSoundADRIFT(m, attributes.get("src"), startStop, 0, repeat);
                break;
            }
            case "wrap":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement tag " + tag);
                break;
            case "q":
                outputString.append(GLKConstants.LDQUOTE_STRING);
                break;
            case "waitkey":
                if (state.mHtmlType == State.HtmlType.HTML_ADRIFT) {
                    // N.B. We are currently assuming that ADRIFT HTML output
                    // is only generated by the Bebek terp
                    glk_request_char_event(m, state.mAttachedWindow.getStreamId());
                    GLKEvent ev = new GLKEvent();
                    do {
                        try {
                            glk_select(m, ev);
                        } catch (InterruptedException e) {
                            GLKLogger.error("ADRIFT HTML: manage interrupt in wait tag.");
                            break;
                        }
                    } while (ev.type != GLKConstants.evtype_CharInput);
                }
                break;
            case "cls":
                if (state.mHtmlType == State.HtmlType.HTML_ADRIFT) {
                    glk_window_clear(m, state.mAttachedWindow.getStreamId());
                }
                break;
            case "c":
                if (state.mHtmlType == State.HtmlType.HTML_ADRIFT) {
                    GLKLogger.error("ADRIFT HTML: TODO: recognise <c> tags");
                }
                break;
            default: {
                // If this is a h1..h6 tag then process it
                if (tag.length() == 2 &&
                        Character.toLowerCase(tag.charAt(0)) == 'h' &&
                        tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
                    // Process header tag
                    outputString.handlePara();
                    String a = attributes.get("align");
                    Layout.Alignment align = null;
                    if (a != null && state.mDivAligns.size() == 0) {
                        // only set the alignment if it's explicitly stated
                        // and we haven't inherited alignments from parent DIV elements.
                        align = getHTMLHAlign(a);
                    }
                    GLKStyleSpan.start(outputString, new Header(tag.charAt(1) - '1', align));
                } else {
                    // Didn't recognise it
                    GLKLogger.warn("TADSHtml: Did not recognise start tag '" + tag + "' - ignoring.");
                }
            }
        }
    }

    /**
     * Process an HTML end tag element.
     *
     * @param tag   - the end tag.
     * @param state - a state which will be modified as a result of this processing.
     */
    static void endElement(@NonNull final String tag, @NonNull State state) {
        // Logger.error("end tag: " + tag);

        // Process special container tags first
        // N.B. these tags cannot be nested
        GLKModel m = state.mGLKModel;
        switch (tag) {
            case "title": {
                String s = state.getRawText(tag.length());
                if (s.length() > 0) {
                    if (DEBUG_TADS_PARSER) {
                        GLKLogger.debug("TADS HTML: ignored Title text: '" + state.getRawText(tag.length()) + "'");
                    }
                }
                state.captureRawText(false);
                return;
            }
            case "banner": {
                String s = state.getRawText(tag.length());
                if (s.length() > 0) {
                    GLKOutputStream os = m.mStreamMgr.getOutputStream(state.mCurBanIDGLK);
                    if (os == null) {
                        GLKLogger.warn("TADS HTML: banner tag: invalid banner id: " + state.mCurBanIDGLK);
                        return;
                    }
                    os.putString(s);
                    if (state.mCurBanResizeToContents) {
                        if (DEBUG_TADS_PARSER) {
                            GLKLogger.debug("TADS HTML: captured banner text: '" + s + "'");
                        }
                        GLKController.tadsban_size_to_contents(m, state.mCurBanIDGLK);
                    }
                }
                state.captureRawText(false);
                return;
            }
            case "aboutbox": {
                String s = state.getRawText(tag.length());
                if (s.length() > 0) {
                    if (DEBUG_TADS_PARSER) {
                        GLKLogger.debug("TADS HTML: ignored AboutBox text: '" + s + "'");
                    }
                }
                state.captureRawText(false);
                return;
            }
            case "pre":
            case "xmp":
            case "listing":
            case "plaintext": {
                String s = state.getRawText(tag.length());
                if (s.length() > 0) {
                    if (DEBUG_TADS_PARSER) {
                        GLKLogger.debug("TADS HTML: captured <pre> text: '" + s + "'");
                    }
                    SpannableStringBuilder output = state.mOutputSpanned;
                    output.append(s);
                    int len = output.length();
                    int slen = s.length();
                    output.setSpan(new AnyFontTypefaceSpan("monospace"), len - slen, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                state.captureRawText(false);
                return;
            }
        }

        // Other tags are only processed if we're not in certain containers
        if (state.isCapturingRawText()) {
            return;
        }

        WatchableStringBuilder outputString = state.mOutputSpanned;
        switch (tag) {
            case "address":  // we treat <address> as a synonym for <p>
            case "p":
                outputString.handlePara();
                endPara(outputString);
                break;
            case "ul":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement end tag " + tag);
                break;
            case "ol":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement end tag " + tag);
                break;
            case "li":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement end tag " + tag);
                break;
            case "div":
            case "center":
                outputString.handlePara();
                endDiv(outputString);
                try {
                    state.mDivAligns.pop();
                } catch (EmptyStackException e) {
                    GLKLogger.warn("TADSHTML: attempted to pop empty divaligns stack. This indicates mismatch between number of div openning and closing tags. Ignoring...");
                }
                break;
            case "blockquote":
            case "bq":  // bq is a synonym for blockquote
                GLKStyleSpan.end(outputString, Blockquote.class, new QuoteSpan());
                outputString.handlePara();
                break;
            case "table":
                if (state.mTables.size() > 0) {
                    try {
                        TableSpan t = state.mTables.pop();
                        if (t.mOpenCellTags > 0) {
                            endCell(outputString, t);
                            t.mOpenCellTags--;
                        }
                        t.finaliseTable();
                        endTable(outputString, t);
                        if (state.mTables.size() == 0) {
                            GLKTextWindowM w = state.mAttachedWindow;
                            t.measure(w.getBaseTextPaint(), w.mWidthGLK_PX);
                            if (DEBUG_TADS_PARSER) {
                                GLKLogger.debug(t.toString());
                            }
                        }
                        if (DEBUG_TADS_PARSER) {
                            GLKLogger.debug("closed table, # tables = " + state.mTables.size());
                        }
                    } catch (EmptyStackException e) {
                        // shouldn't get here as the size of the stack was supposed to be > 0
                        // but we are totally paranoid when it comes to Android system functions
                        GLKLogger.warn("TADSHTML: empty stack exception on tables stack, even though size > 0. Ignoring...");
                    }
                }
                break;
            case "caption":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement end tag " + tag);
                break;
            case "tr":
            case "th":
            case "td":
                //GLKLogger.debug("TADSHTML: Ignoring unnecessary (though legal) end tag " + tag);
                break;
            case "tt":
                GLKStyleSpan.end(outputString, Monospace.class, new AnyFontTypefaceSpan("monospace"));
                break;
            case "i":
                GLKStyleSpan.end(outputString, Italic.class, new StyleSpan(Typeface.ITALIC));
                break;
            case "b":
                GLKStyleSpan.end(outputString, Bold.class, new StyleSpan(Typeface.BOLD));
                break;
            case "u":
                GLKStyleSpan.end(outputString, Underline.class, new UnderlineSpan());
                break;
            case "strike":
                GLKStyleSpan.end(outputString, Strike.class, new StrikethroughSpan());
                break;
            case "big":
                GLKStyleSpan.end(outputString, Big.class, new RelativeSizeSpan(1.25f));
                break;
            case "small":
                GLKStyleSpan.end(outputString, Small.class, new RelativeSizeSpan(0.8f));
                break;
            case "sup":
                GLKStyleSpan.end(outputString, Super.class, new SuperscriptSpan());
                break;
            case "sub":
                GLKStyleSpan.end(outputString, Sub.class, new SubscriptSpan());
                break;
            case "em":
                GLKStyleSpan.end(outputString, Italic.class, new StyleSpan(Typeface.ITALIC));
                break;
            case "strong":
                GLKStyleSpan.end(outputString, Bold.class, new StyleSpan(Typeface.BOLD));
                break;
            case "dfn":
                GLKStyleSpan.end(outputString, Italic.class, new StyleSpan(Typeface.ITALIC));
                break;
            case "code":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement end tag " + tag);
                break;
            case "samp":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement end tag " + tag);
                break;
            case "kbd":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement end tag " + tag);
                break;
            case "var":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement end tag " + tag);
                break;
            case "cite":
                GLKStyleSpan.end(outputString, Italic.class, new StyleSpan(Typeface.ITALIC));
                break;
            case "a":
                endA(outputString, GLKController.tadsban_html_get_root(m), state);
                break;
            case "font":
                endFont(state, outputString);
                break;
            case "map":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement end tag " + tag);
                break;
            case "nobr":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement end tag " + tag);
                break;
            case "lh":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement end tag " + tag);
                break;
            case "wrap":
                // TODO
                GLKLogger.error("TADSHTML: FIXME: Implement end tag " + tag);
                break;
            case "q":
                outputString.append(GLKConstants.RDQUOTE_STRING);
                break;
            default:
                // If this is a h1..h6 tag then process it
                if (tag.length() == 2 &&
                        Character.toLowerCase(tag.charAt(0)) == 'h' &&
                        tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
                    // Process header tag
                    outputString.handlePara();
                    endHeader(outputString);
                } else {
                    // Didn't recognise it
                    GLKLogger.warn("TADSHtml: Did not recognise end tag '" + tag + "' - ignoring.");
                }
        }
    }

    private static int getTADSColor(@Nullable String color, @NonNull State state) {
        if (color == null) {
            return Color.TRANSPARENT;
        }

        // Converts a TADS color string into an actual color
        switch (color.toLowerCase()) {
            case "alink":
                // "'active' hyperlink text color"
                return state.mGLKModel.mTadsColors[TADS_COLOR_ALINK];
            case "bgcolor":
                // "the default background color (note
                // that this is the default background color, not
                // necessarily the current color; this is usually
                // the color set in the user's preferences)"
                return state.mGLKModel.mTadsColors[TADS_COLOR_BGCOLOR];
            case "hlink":
                // "the 'hovering' hyperlink text color"
                return state.mGLKModel.mTadsColors[TADS_COLOR_HLINK];
            case "link":
                // "hyperlink text color"
                return state.mGLKModel.mTadsColors[TADS_COLOR_LINK];
            case "statusbg":
                // "status line background color"
                return state.mGLKModel.mTadsColors[TADS_COLOR_STATUSBG];
            case "statustext":
                // "status line text color"
                return state.mGLKModel.mTadsColors[TADS_COLOR_STATUSTEXT];
            case "text":
                // "the default text color (note that this is the default text color, not
                // necessarily the current color; this is usually the color set in the
                // user's preferences)"
                return state.mGLKModel.mTadsColors[TADS_COLOR_TEXT];
            case "input":
                // "the text color for command line input"
                return state.mGLKModel.mTadsColors[TADS_COLOR_INPUT];
            default:
                try {
                    return Color.parseColor(color);
                } catch (IllegalArgumentException e) {
                    try {
                        // need this to process ADRIFT color attributes properly
                        return Color.parseColor("#" + color);
                    } catch (IllegalArgumentException e2) {
                        GLKLogger.error("TADS HTML Parser: didn't recognise color: '" +
                                color + "' - setting to default text colour.");
                        return state.mGLKModel.mTadsColors[TADS_COLOR_TEXT];
                    }
                }
        }
    }

    private static boolean getHTMLBoolean(@Nullable String val, boolean defVal) {
        return val != null || defVal;
    }

    private static int getHTMLPixels(@Nullable String pixels, @NonNull Resources r, int defVal) {
        if (pixels == null) {
            return defVal;
        }
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, getHTMLInt(pixels, defVal), r.getDisplayMetrics());
    }

    private static int getHTMLInt(@Nullable String intValue, int defVal) {
        if (intValue == null) {
            return defVal;
        }

        try {
            return Integer.valueOf(intValue);
        } catch (IllegalArgumentException e) {
            GLKLogger.warn("TADS HTML Parser: getHTMLInt: supplied intValue not a valid integer: '" + intValue + "'");
            return defVal;
        }
    }

    @NonNull
    private static Layout.Alignment getHTMLHAlign(@Nullable String align) {
        if (align == null) {
            return Layout.Alignment.ALIGN_NORMAL;
        } else {
            switch (align.toLowerCase()) {
                case "left":
                    return Layout.Alignment.ALIGN_NORMAL;
                case "center":
                    return Layout.Alignment.ALIGN_CENTER;
                case "right":
                    return Layout.Alignment.ALIGN_OPPOSITE;
                default:
                    GLKLogger.error("TADS HTML: did not recognise horizontal alignment: '" + align + "' - defaulting to left");
                    return Layout.Alignment.ALIGN_NORMAL;
            }
        }
    }

    @NonNull
    private static VERTICAL_ALIGNMENT getHTMLVAlign(@Nullable String valign) {
        if (valign == null) {
            return VERTICAL_ALIGNMENT.TOP;
        } else {
            switch (valign.toLowerCase()) {
                case "top":
                    return VERTICAL_ALIGNMENT.TOP;
                case "middle":
                    return VERTICAL_ALIGNMENT.MIDDLE;
                case "bottom":
                    return VERTICAL_ALIGNMENT.BOTTOM;
                default:
                    GLKLogger.error("TADS HTML: did not recognise vertical alignment: '" + valign + "' - defaulting to top");
                    return VERTICAL_ALIGNMENT.TOP;
            }
        }
    }

    private static int getTADSBannerAlignment(@Nullable String align) {
        if (align == null) {
            return GLKConstants.TADS_BANNER_ALIGN_TOP;
        } else {
            switch (align.toLowerCase()) {
                case "left":
                    return GLKConstants.TADS_BANNER_ALIGN_LEFT;
                case "right":
                    return GLKConstants.TADS_BANNER_ALIGN_RIGHT;
                case "top":
                    return GLKConstants.TADS_BANNER_ALIGN_TOP;
                case "bottom":
                    return GLKConstants.TADS_BANNER_ALIGN_BOTTOM;
                default:
                    return GLKConstants.TADS_BANNER_ALIGN_TOP;
            }
        }
    }

    private static Typeface getTADSFontFamily(@NonNull String name, @NonNull State state) {
        // returns NULL if the font family wasn't recognised
        Typeface t;
        switch (name.toLowerCase()) {
            case "tads-serif":
            case "ms serif":
            case "times new roman":
                t = state.mGLKModel.mTadsFonts[TADS_FONT_SERIF];
                return (t != null) ? t : Typeface.create("serif", Typeface.NORMAL);
            case "tads-sans":
            case "ms sans serif":
            case "sans-serif":
            case "arial":
                t = state.mGLKModel.mTadsFonts[TADS_FONT_SANS];
                return (t != null) ? t : Typeface.create("sans-serif", Typeface.NORMAL);
            case "tads-script":
                t = state.mGLKModel.mTadsFonts[TADS_FONT_SCRIPT];
                return (t != null) ? t : Typeface.create("serif", Typeface.NORMAL);
            case "tads-typewriter":
            case "courier new":
                t = state.mGLKModel.mTadsFonts[TADS_FONT_TYPEWRITER];
                return (t != null) ? t : Typeface.create("monospace", Typeface.NORMAL);
            case "tads-input":
                t = state.mGLKModel.mTadsFonts[TADS_FONT_INPUT];
                return (t != null) ? t : Typeface.create("serif", Typeface.NORMAL);
            default:
                GLKLogger.warn("did not recognise font family: " + name);
                return null;
        }
    }

    enum VERTICAL_ALIGNMENT {TOP, MIDDLE, BOTTOM}

    private static class Bold {
    }

    private static class Italic {
    }

    private static class Underline {
    }

    private static class Strike {
    }

    private static class Big {
    }

    private static class Small {
    }

    private static class Monospace {
    }

    private static class Blockquote {
    }

    private static class Super {
    }

    private static class Sub {
    }

    private static class Div {
        final Layout.Alignment mAlign;

        Div(Layout.Alignment align) {
            mAlign = align;
        }
    }

    private static class Font {
        final String mColor;
        final String mBGColor;
        final String mFace;
        final int mFontSize;  // 0 if not set, 1 .. 7 otherwise

        public Font(String color, String bgcolor, String face, int size) {
            mColor = color;
            mFace = face;
            mFontSize = size;
            mBGColor = bgcolor;
        }
    }

    private static class Href {
        final String mHref;
        final boolean mAppend;
        final boolean mNoEnter;
        final boolean mPlain;

        Href(String href, boolean append, boolean noenter, boolean plain) {
            mHref = href;
            mAppend = append;
            mNoEnter = noenter;
            mPlain = plain;
        }
    }

    private static class Header {
        private final int mLevel;
        private final Layout.Alignment mAlign;  // can be null

        public Header(int level, Layout.Alignment align) {
            mLevel = level;
            mAlign = align;
        }
    }

    private static class Para {
        private final Layout.Alignment mAlign;  // can be null

        Para(Layout.Alignment align) {
            mAlign = align;
        }
    }

    // TABLE markers
    private static class Table {
    }

    private static class TableCell {
        final boolean mIsHeader;
        final boolean mNowrap;
        final int mRowspan;
        final int mColspan;
        final Layout.Alignment mAlign;
        final VERTICAL_ALIGNMENT mVAlign;
        final HTMLLength mWidth;
        final int mHeight;
        final int mBGColor;
        final String mBackground;

        TableCell(boolean isheader, boolean nowrap, int rowspan, int colspan,
                  Layout.Alignment align, VERTICAL_ALIGNMENT valign,
                  HTMLLength width, int height, int bgColor, String background) {
            mIsHeader = isheader;
            mNowrap = nowrap;
            mRowspan = rowspan;
            mColspan = colspan;
            mAlign = align;
            mVAlign = valign;
            mWidth = width;
            mHeight = height;
            mBGColor = bgColor;
            mBackground = background;
        }
    }
}