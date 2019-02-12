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

package com.luxlunae.glk.view.keyboard;

/*
 * Copyright (C) 2008-2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;

import com.luxlunae.fabularium.R;
import com.luxlunae.glk.GLKUtils;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This is a modified version of Android's standard Keyboard class.
 *
 * Loads an XML description of a keyboard and stores the attributes of the keys. A keyboard
 * consists of rows of keys.
 * <p>The layout file for a keyboard contains XML that looks like the following snippet:</p>
 * <pre>
 * &lt;Keyboard
 *         android:keyWidth="%10p"
 *         android:keyHeight="50px"
 *         android:horizontalGap="2px"
 *         android:verticalGap="2px" &gt;
 *     &lt;Row android:keyWidth="32px" &gt;
 *         &lt;Key android:keyLabel="A" /&gt;
 *         ...
 *     &lt;/Row&gt;
 *     ...
 * &lt;/Keyboard&gt;
 * </pre>
 *
 *   android.R.styleable#Keyboard_keyWidth
 *   android.R.styleable#Keyboard_keyHeight
 *   android.R.styleable#Keyboard_horizontalGap
 *   android.R.styleable#Keyboard_verticalGap
 */
public class GLKKeyboard {
    static final int EDGE_LEFT = 0x01;
    static final int EDGE_RIGHT = 0x02;
    static final int EDGE_TOP = 0x04;
    static final int EDGE_BOTTOM = 0x08;
    static final int KEYCODE_SHIFT = -1;
    static final int KEYCODE_MODE_CHANGE = -2;
    static final int KEYCODE_CANCEL = -3;
    static final int KEYCODE_DONE = -4;
    static final int KEYCODE_DELETE = -5;
    static final int KEYCODE_ALT = -6;
    static final String TAG = "Keyboard";

    // Keyboard XML Tags
    private static final String TAG_KEYBOARD = "Keyboard";
    private static final String TAG_ROW = "Row";
    private static final String TAG_KEY = "Key";
    private static final int GRID_WIDTH = 10;
    private static final int GRID_HEIGHT = 5;
    private static final int GRID_SIZE = GRID_WIDTH * GRID_HEIGHT;
    /**
     * Horizontal gap default for all rows
     */
    private int mDefaultHorizontalGap;
    /**
     * Default key width
     */
    private int mDefaultWidth;
    /**
     * Default key height
     */
    private int mDefaultHeight;
    /**
     * Default gap between rows
     */
    private int mDefaultVerticalGap;
    /**
     * Is the keyboard in the shifted state
     */
    private boolean mShifted;
    /**
     * Key instance for the shift key, if present
     */
    @Nullable
    private Key[] mShiftKeys = {null, null};
    /**
     * Key index for the shift key, if present
     */
    @NonNull
    private int[] mShiftKeyIndices = {-1, -1};
    /**
     * Total height of the keyboard, including the padding and keys
     */
    private int mTotalHeight;
    /**
     * Total width of the keyboard, including left side gaps and keys, but not any gaps on the
     * right side.
     */
    private int mTotalWidth;
    /**
     * List of keys in this keyboard
     */
    private List<Key> mKeys;
    // Variables for pre-computing nearest keys.
    /**
     * Width of the screen available to fit the keyboard
     */
    private int mDisplayWidth;
    /**
     * Height of the screen
     */
    private int mDisplayHeight;
    /**
     * Keyboard mode, or zero, if none.
     */
    private int mKeyboardMode;
    private int mCellWidth;
    private int mCellHeight;
    private int[][] mGridNeighbors;
    private int mProximityThreshold;
    @NonNull
    private ArrayList<Row> rows = new ArrayList<>();

    GLKKeyboard(@NonNull Context context, @NonNull GLKKeyboardMapping mapping) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        mDisplayWidth = dm.widthPixels;
        mDisplayHeight = dm.heightPixels;
        //Log.v(TAG, "keyboard's display metrics:" + dm);
        mDefaultHorizontalGap = 0;
        mDefaultWidth = mDisplayWidth / 10;
        mDefaultVerticalGap = 0;
        mDefaultHeight = mDefaultWidth;
        mKeys = new ArrayList<>();
        mKeyboardMode = 0;
        loadKeyboard(context, context.getResources().getXml(mapping.mXmlLayoutResId));

        // Remap the keys as appropriate
        CharSequence[] keyLabels = mapping.mKeyLabels;
        int[][] keyCodes = mapping.mKeyCodes;
        CharSequence[] hintLabels = mapping.mHintLabels;
        int[][] hintCodes = mapping.mHintCodes;
        Boolean[] repeatable = mapping.mRepeatable;
        Integer[] keyLabelSize = mapping.mKeyLabelSize;
        Integer[] hintLabelSize = mapping.mHintLabelSize;
        int szLabels = (keyLabels != null) ? keyLabels.length : 0;
        int szCodes = (keyCodes != null) ? keyCodes.length : 0;
        int szHintLabels = (hintLabels != null) ? hintLabels.length : 0;
        int szHintCodes = (hintCodes != null) ? hintCodes.length : 0;
        int szRepeatable = (repeatable != null) ? repeatable.length : 0;
        int szKeyLabelSize = (keyLabelSize != null) ? keyLabelSize.length : 0;
        int szHintLabelSize = (hintLabelSize != null) ? hintLabelSize.length : 0;
        int i = 0;
        for (Key k : mKeys) {
            if (i < szLabels && keyLabels[i] != null) {
                k.label = keyLabels[i];
                k.icon = null;   // remove any default icon on this key if the user has over-ridden the label
            }
            if (i < szCodes && keyCodes[i] != null) {
                k.codes = keyCodes[i];
            }
            if (i < szHintLabels && hintLabels[i] != null) {
                k.hintLabel = hintLabels[i];
            }
            if (i < szHintCodes && hintCodes[i] != null) {
                k.hintCodes = hintCodes[i];
                k.repeatable = false;   // falsify any default repeatable if the user has over-ridden long-press codes
            }
            if (i < szRepeatable && repeatable[i] != null) {
                k.repeatable = repeatable[i];
                if (k.repeatable) {
                    k.hintCodes = null;     // remove any long-press codes if the user has over-ridden the repeatable option
                }
            }
            if (i < szKeyLabelSize && keyLabelSize[i] != null) {
                k.labelSize = GLKUtils.spToPx(keyLabelSize[i], context.getResources().getDisplayMetrics());
            }
            if (i < szHintLabelSize && hintLabelSize[i] != null) {
                k.hintLabelSize = GLKUtils.spToPx(hintLabelSize[i], context.getResources().getDisplayMetrics());
            }
            i++;
        }
    }

    private static int getDimensionOrFraction(TypedArray a, int index, int base, int defValue) {
        TypedValue value = a.peekValue(index);
        if (value == null) return defValue;
        if (value.type == TypedValue.TYPE_DIMENSION) {
            return a.getDimensionPixelOffset(index, defValue);
        } else if (value.type == TypedValue.TYPE_FRACTION) {
            // Round it to avoid values like 47.9999 from getting truncated
            return Math.round(a.getFraction(index, base, base, defValue));
        }
        return defValue;
    }

    private static Key createKeyFromXml(@NonNull Resources res, @NonNull Row parent, int x, int y,
                                        XmlResourceParser parser) {
        return new Key(res, parent, x, y, parser);
    }

    private static void skipToEndOfRow(XmlResourceParser parser) throws XmlPullParserException, IOException {
        int event;
        while ((event = parser.next()) != XmlResourceParser.END_DOCUMENT) {
            if (event == XmlResourceParser.END_TAG
                    && parser.getName().equals(TAG_ROW)) {
                break;
            }
        }
    }

    final void resize(int newWidth, int newHeight) {
        int numRows = rows.size();
        for (int rowIndex = 0; rowIndex < numRows; ++rowIndex) {
            Row row = rows.get(rowIndex);
            int numKeys = row.mKeys.size();
            int totalGap = 0;
            int totalWidth = 0;
            for (int keyIndex = 0; keyIndex < numKeys; ++keyIndex) {
                Key key = row.mKeys.get(keyIndex);
                if (keyIndex > 0) {
                    totalGap += key.gap;
                }
                totalWidth += key.width;
            }
            if (totalGap + totalWidth > newWidth) {
                int x = 0;
                float scaleFactor = (float) (newWidth - totalGap) / totalWidth;
                for (int keyIndex = 0; keyIndex < numKeys; ++keyIndex) {
                    Key key = row.mKeys.get(keyIndex);
                    key.width *= scaleFactor;
                    key.x = x;
                    x += key.width + key.gap;
                }
            }
        }
        mTotalWidth = newWidth;
        // TODO: This does not adjust the vertical placement according to the new size.
        // The main problem in the previous code was horizontal placement/size, but we should
        // also recalculate the vertical sizes/positions when we get this resize call.
    }

    public List<Key> getKeys() {
        return mKeys;
    }

    protected int getHorizontalGap() {
        return mDefaultHorizontalGap;
    }

    protected void setHorizontalGap(int gap) {
        mDefaultHorizontalGap = gap;
    }

    protected int getVerticalGap() {
        return mDefaultVerticalGap;
    }

    protected void setVerticalGap(int gap) {
        mDefaultVerticalGap = gap;
    }

    protected int getKeyHeight() {
        return mDefaultHeight;
    }

    protected void setKeyHeight(int height) {
        mDefaultHeight = height;
    }

    protected int getKeyWidth() {
        return mDefaultWidth;
    }

    protected void setKeyWidth(int width) {
        mDefaultWidth = width;
    }

    /**
     * Returns the total height of the keyboard
     *
     * @return the total height of the keyboard
     */
    public int getHeight() {
        return mTotalHeight;
    }

    int getMinWidth() {
        return mTotalWidth;
    }

    boolean setShifted(boolean shiftState) {
        for (Key shiftKey : mShiftKeys) {
            if (shiftKey != null) {
                shiftKey.on = shiftState;
            }
        }
        if (mShifted != shiftState) {
            mShifted = shiftState;
            return true;
        }
        return false;
    }

    public boolean isShifted() {
        return mShifted;
    }

    private void computeNearestNeighbors() {
        // Round-up so we don't have any pixels outside the grid
        mCellWidth = (getMinWidth() + GRID_WIDTH - 1) / GRID_WIDTH;
        mCellHeight = (getHeight() + GRID_HEIGHT - 1) / GRID_HEIGHT;
        mGridNeighbors = new int[GRID_SIZE][];
        int[] indices = new int[mKeys.size()];
        final int gridWidth = GRID_WIDTH * mCellWidth;
        final int gridHeight = GRID_HEIGHT * mCellHeight;
        for (int x = 0; x < gridWidth; x += mCellWidth) {
            for (int y = 0; y < gridHeight; y += mCellHeight) {
                int count = 0;
                for (int i = 0; i < mKeys.size(); i++) {
                    final Key key = mKeys.get(i);
                    if (key.squaredDistanceFrom(x, y) < mProximityThreshold ||
                            key.squaredDistanceFrom(x + mCellWidth - 1, y) < mProximityThreshold ||
                            key.squaredDistanceFrom(x + mCellWidth - 1, y + mCellHeight - 1)
                                    < mProximityThreshold ||
                            key.squaredDistanceFrom(x, y + mCellHeight - 1) < mProximityThreshold) {
                        indices[count++] = i;
                    }
                }
                int[] cell = new int[count];
                System.arraycopy(indices, 0, cell, 0, count);
                mGridNeighbors[(y / mCellHeight) * GRID_WIDTH + (x / mCellWidth)] = cell;
            }
        }
    }

    /**
     * Returns the indices of the keys that are closest to the given point.
     *
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return the array of integer indices for the nearest keys to the given point. If the given
     * point is out of range, then an array of size zero is returned.
     */
    int[] getNearestKeys(int x, int y) {
        if (mGridNeighbors == null) computeNearestNeighbors();
        if (x >= 0 && x < getMinWidth() && y >= 0 && y < getHeight()) {
            int index = (y / mCellHeight) * GRID_WIDTH + (x / mCellWidth);
            if (index < GRID_SIZE) {
                return mGridNeighbors[index];
            }
        }
        return new int[0];
    }

    private Row createRowFromXml(@NonNull Resources res, XmlResourceParser parser) {
        return new Row(res, this, parser);
    }

    private void loadKeyboard(Context context, @NonNull XmlResourceParser parser) {
        boolean inKey = false;
        boolean inRow = false;
        int x = 0;
        int y = 0;
        Key key = null;
        Row currentRow = null;
        Resources res = context.getResources();
        boolean skipRow;
        try {
            int event;
            while ((event = parser.next()) != XmlResourceParser.END_DOCUMENT) {
                if (event == XmlResourceParser.START_TAG) {
                    String tag = parser.getName();
                    if (TAG_ROW.equals(tag)) {
                        inRow = true;
                        x = 0;
                        currentRow = createRowFromXml(res, parser);
                        rows.add(currentRow);
                        skipRow = currentRow.mode != 0 && currentRow.mode != mKeyboardMode;
                        if (skipRow) {
                            skipToEndOfRow(parser);
                            inRow = false;
                        }
                    } else if (TAG_KEY.equals(tag)) {
                        inKey = true;
                        key = createKeyFromXml(res, currentRow, x, y, parser);
                        mKeys.add(key);
                        if (key.codes[0] == KEYCODE_SHIFT) {
                            // Find available shift key slot and put this shift key in it
                            for (int i = 0; i < mShiftKeys.length; i++) {
                                if (mShiftKeys[i] == null) {
                                    mShiftKeys[i] = key;
                                    mShiftKeyIndices[i] = mKeys.size() - 1;
                                    break;
                                }
                            }
                        }
                        currentRow.mKeys.add(key);
                    } else if (TAG_KEYBOARD.equals(tag)) {
                        parseKeyboardAttributes(res, parser);
                    }
                } else if (event == XmlResourceParser.END_TAG) {
                    if (inKey) {
                        inKey = false;
                        x += key.gap + key.width;
                        if (x > mTotalWidth) {
                            mTotalWidth = x;
                        }
                    } else if (inRow) {
                        inRow = false;
                        y += currentRow.verticalGap;
                        y += currentRow.defaultHeight;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Parse error:" + e);
            e.printStackTrace();
        }
        mTotalHeight = y - mDefaultVerticalGap;
    }

    private void parseKeyboardAttributes(Resources res, XmlResourceParser parser) {
        TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.GLKKeyboard);
        mDefaultWidth = getDimensionOrFraction(a,
                R.styleable.GLKKeyboard_keyWidth,
                mDisplayWidth, mDisplayWidth / 10);
        mDefaultHeight = getDimensionOrFraction(a,
                R.styleable.GLKKeyboard_keyHeight,
                mDisplayHeight, 50);
        mDefaultHorizontalGap = getDimensionOrFraction(a,
                R.styleable.GLKKeyboard_horizontalGap,
                mDisplayWidth, 0);
        mDefaultVerticalGap = getDimensionOrFraction(a,
                R.styleable.GLKKeyboard_verticalGap,
                mDisplayHeight, 0);

        // Number of key widths from current touch point to search for nearest keys.
        float SEARCH_DISTANCE = 1.8f;
        mProximityThreshold = (int) (mDefaultWidth * SEARCH_DISTANCE);
        mProximityThreshold = mProximityThreshold * mProximityThreshold; // Square it for comparison
        a.recycle();
    }

    /**
     * Container for keys in the keyboard. All keys in a row are at the same Y-coordinate.
     * Some of the key size defaults can be overridden per row from what the {@link GLKKeyboard}
     * defines.
     *
     *  R.styleable#Keyboard_keyWidth
     *  R.styleable#Keyboard_keyHeight
     *  R.styleable#Keyboard_horizontalGap
     *  R.styleable#Keyboard_verticalGap
     *  R.styleable#Keyboard_Row_rowEdgeFlags
     *  R.styleable#Keyboard_Row_keyboardMode
     */
    public static class Row {
        /**
         * The keyboard mode for this row
         */
        public int mode;
        /**
         * Default width of a key in this row.
         */
        int defaultWidth;
        /**
         * Default height of a key in this row.
         */
        int defaultHeight;
        /**
         * Default horizontal gap between keys in this row.
         */
        int defaultHorizontalGap;
        /**
         * Vertical gap following this row.
         */
        int verticalGap;
        /**
         * Edge flags for this row of keys. Possible values that can be assigned are
         * {@link GLKKeyboard#EDGE_TOP EDGE_TOP} and {@link GLKKeyboard#EDGE_BOTTOM EDGE_BOTTOM}
         */
        int rowEdgeFlags;
        @NonNull
        ArrayList<Key> mKeys = new ArrayList<>();
        private GLKKeyboard parent;

        public Row(GLKKeyboard parent) {
            this.parent = parent;
        }

        public Row(Resources res, GLKKeyboard parent, XmlResourceParser parser) {
            this.parent = parent;
            TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser),
                    R.styleable.GLKKeyboard);
            defaultWidth = getDimensionOrFraction(a,
                    R.styleable.GLKKeyboard_keyWidth,
                    parent.mDisplayWidth, parent.mDefaultWidth);
            defaultHeight = getDimensionOrFraction(a,
                    R.styleable.GLKKeyboard_keyHeight,
                    parent.mDisplayHeight, parent.mDefaultHeight);
            defaultHorizontalGap = getDimensionOrFraction(a,
                    R.styleable.GLKKeyboard_horizontalGap,
                    parent.mDisplayWidth, parent.mDefaultHorizontalGap);
            verticalGap = getDimensionOrFraction(a,
                    R.styleable.GLKKeyboard_verticalGap,
                    parent.mDisplayHeight, parent.mDefaultVerticalGap);
            a.recycle();
            a = res.obtainAttributes(Xml.asAttributeSet(parser),
                    R.styleable.GLKKeyboard_Row);
            rowEdgeFlags = a.getInt(R.styleable.GLKKeyboard_Row_rowEdgeFlags, 0);
            mode = a.getResourceId(R.styleable.GLKKeyboard_Row_keyboardMode,
                    0);
        }
    }

    /**
     * Class for describing the position and characteristics of a single key in the keyboard.
     *
     *  R.styleable#Keyboard_keyWidth
     *  R.styleable#Keyboard_keyHeight
     *  R.styleable#Keyboard_horizontalGap
     *  R.styleable#Keyboard_Key_codes
     *  R.styleable#Keyboard_Key_keyIcon
     *  R.styleable#Keyboard_Key_keyLabel
     *  R.styleable#Keyboard_Key_iconPreview
     *  R.styleable#Keyboard_Key_isSticky
     *  R.styleable#Keyboard_Key_isRepeatable
     *  R.styleable#Keyboard_Key_isModifier
     *  R.styleable#Keyboard_Key_popupKeyboard
     *  R.styleable#Keyboard_Key_popupCharacters
     *  R.styleable#Keyboard_Key_keyOutputText
     *  R.styleable#Keyboard_Key_keyEdgeFlags
     */
    public static class Key {
        private final static int[] KEY_STATE_NORMAL_ON = {
                android.R.attr.state_checkable,
                android.R.attr.state_checked
        };
        private final static int[] KEY_STATE_PRESSED_ON = {
                android.R.attr.state_pressed,
                android.R.attr.state_checkable,
                android.R.attr.state_checked
        };
        private final static int[] KEY_STATE_NORMAL_OFF = {
                android.R.attr.state_checkable
        };
        private final static int[] KEY_STATE_PRESSED_OFF = {
                android.R.attr.state_pressed,
                android.R.attr.state_checkable
        };
        private final static int[] KEY_STATE_NORMAL = {
        };
        private final static int[] KEY_STATE_PRESSED = {
                android.R.attr.state_pressed
        };
        /**
         * All the key codes (unicode or custom code) that this key could generate, zero'th
         * being the most important.
         */
        public int[] codes;
        /**
         * Label to display
         */
        public CharSequence label;
        /**
         * Size of the label (in pixels)
         */
        @Nullable
        public Integer labelSize;
        /**
         * Icon to display instead of a label. Icon takes precedence over a label
         */
        @Nullable
        public Drawable icon;
        /**
         * Width of the key, not including the gap
         */
        public int width;
        /**
         * Height of the key, not including the gap
         */
        public int height;
        /**
         * The horizontal gap before this key
         */
        public int gap;
        /**
         * Whether this key is sticky, i.e., a toggle key
         */
        public boolean sticky;
        /**
         * X coordinate of the key in the keyboard layout
         */
        public int x;
        /**
         * Y coordinate of the key in the keyboard layout
         */
        public int y;
        /**
         * The current pressed state of this key
         */
        public boolean pressed;
        /**
         * If this is a sticky key, is it on?
         */
        public boolean on;
        /**
         * Text to output when pressed. This can be multiple characters, like ".com"
         */
        public CharSequence text;
        /**
         * Whether this is a modifier key, such as Shift or Alt
         */
        public boolean modifier;
        /**
         * Whether this key repeats itself when held down
         */
        public boolean repeatable;
        /**
         * Preview version of the icon, for the preview popup
         */
        @Nullable
        Drawable iconPreview;
        /**
         * Popup characters
         */
        CharSequence popupCharacters;
        /**
         * Flags that specify the anchoring to edges of the keyboard for detecting touch events
         * that are just out of the boundary of the key. This is a bit mask of
         * {@link GLKKeyboard#EDGE_LEFT}, {@link GLKKeyboard#EDGE_RIGHT}, {@link GLKKeyboard#EDGE_TOP} and
         * {@link GLKKeyboard#EDGE_BOTTOM}.
         */
        int edgeFlags;
        /**
         * If this key pops up a mini keyboard, this is the resource id for the XML layout for that
         * keyboard.
         */
        int popupResId;
        @Nullable
        int[] hintCodes;
        @Nullable
        CharSequence hintLabel;
        /**
         * Size of hint label in pixels
         */
        @Nullable
        Integer hintLabelSize;
        /**
         * The keyboard that this key belongs to
         */
        private GLKKeyboard keyboard;

        /**
         * Create an empty key with no attributes.
         */
        public Key(Row parent) {
            keyboard = parent.parent;
            height = parent.defaultHeight;
            width = parent.defaultWidth;
            gap = parent.defaultHorizontalGap;
            edgeFlags = parent.rowEdgeFlags;
        }

        /**
         * Create a key with the given top-left coordinate and extract its attributes from
         * the XML parser.
         *
         * @param res    resources associated with the caller's context
         * @param parent the row that this key belongs to. The row must already be attached to
         *               a {@link GLKKeyboard}.
         * @param x      the x coordinate of the top-left
         * @param y      the y coordinate of the top-left
         * @param parser the XML parser containing the attributes for this key
         */
        public Key(Resources res, @NonNull Row parent, int x, int y, XmlResourceParser parser) {
            this(parent);
            this.x = x;
            this.y = y;

            TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser),
                    R.styleable.GLKKeyboard);
            width = getDimensionOrFraction(a,
                    R.styleable.GLKKeyboard_keyWidth,
                    keyboard.mDisplayWidth, parent.defaultWidth);
            height = getDimensionOrFraction(a,
                    R.styleable.GLKKeyboard_keyHeight,
                    keyboard.mDisplayHeight, parent.defaultHeight);
            gap = getDimensionOrFraction(a,
                    R.styleable.GLKKeyboard_horizontalGap,
                    keyboard.mDisplayWidth, parent.defaultHorizontalGap);
            a.recycle();
            a = res.obtainAttributes(Xml.asAttributeSet(parser),
                    R.styleable.GLKKeyboard_Key);
            this.x += gap;
            TypedValue codesValue = new TypedValue();
            a.getValue(R.styleable.GLKKeyboard_Key_codes,
                    codesValue);
            if (codesValue.type == TypedValue.TYPE_INT_DEC
                    || codesValue.type == TypedValue.TYPE_INT_HEX) {
                codes = new int[]{codesValue.data};
            } else if (codesValue.type == TypedValue.TYPE_STRING) {
                codes = parseCSV(codesValue.string.toString());
            }

            iconPreview = a.getDrawable(R.styleable.GLKKeyboard_Key_iconPreview);
            if (iconPreview != null) {
                iconPreview.setBounds(0, 0, iconPreview.getIntrinsicWidth(),
                        iconPreview.getIntrinsicHeight());
            }
            popupCharacters = a.getText(
                    R.styleable.GLKKeyboard_Key_popupCharacters);
            popupResId = a.getResourceId(
                    R.styleable.GLKKeyboard_Key_popupKeyboard, 0);
            repeatable = a.getBoolean(
                    R.styleable.GLKKeyboard_Key_isRepeatable, false);
            modifier = a.getBoolean(
                    R.styleable.GLKKeyboard_Key_isModifier, false);
            sticky = a.getBoolean(
                    R.styleable.GLKKeyboard_Key_isSticky, false);
            edgeFlags = a.getInt(R.styleable.GLKKeyboard_Key_keyEdgeFlags, 0);
            edgeFlags |= parent.rowEdgeFlags;
            icon = a.getDrawable(
                    R.styleable.GLKKeyboard_Key_keyIcon);
            if (icon != null) {
                icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
            }
            label = a.getText(R.styleable.GLKKeyboard_Key_keyLabel);
            text = a.getText(R.styleable.GLKKeyboard_Key_keyOutputText);

            if (codes == null && !TextUtils.isEmpty(label)) {
                codes = new int[]{label.charAt(0)};
            }
            a.recycle();
        }

        @NonNull
        static int[] parseCSV(String value) {
            int count = 0;
            int lastIndex = 0;
            if (value.length() > 0) {
                count++;
                while ((lastIndex = value.indexOf(",", lastIndex + 1)) > 0) {
                    count++;
                }
            }
            int[] values = new int[count];
            count = 0;
            StringTokenizer st = new StringTokenizer(value, ",");
            while (st.hasMoreTokens()) {
                try {
                    values[count++] = Integer.parseInt(st.nextToken());
                } catch (NumberFormatException nfe) {
                    Log.e(TAG, "Error parsing keycodes " + value);
                }
            }
            return values;
        }

        /**
         * Informs the key that it has been pressed, in case it needs to change its appearance or
         * state.
         *
         * @see #onReleased(boolean)
         */
        void onPressed() {
            pressed = !pressed;
        }

        /**
         * Changes the pressed state of the key.
         * <p>
         * <p>Toggled state of the key will be flipped when all the following conditions are
         * fulfilled:</p>
         * <p>
         * <ul>
         * <li>This is a sticky key, that is, {@link #sticky} is {@code true}.
         * <li>The parameter {@code inside} is {@code true}.
         * <li>{@link android.os.Build.VERSION#SDK_INT} is greater than
         * {@link android.os.Build.VERSION_CODES#LOLLIPOP_MR1}.
         * </ul>
         *
         * @param inside whether the finger was released inside the key. Works only on Android M and
         *               later. See the method document for details.
         * @see #onPressed()
         */
        void onReleased(boolean inside) {
            pressed = !pressed;
            if (sticky && inside) {
                on = !on;
            }
        }

        /**
         * Detects if a point falls inside this key.
         *
         * @param x the x-coordinate of the point
         * @param y the y-coordinate of the point
         * @return whether or not the point falls inside the key. If the key is attached to an edge,
         * it will assume that all points between the key and the edge are considered to be inside
         * the key.
         */
        boolean isInside(int x, int y) {
            boolean leftEdge = (edgeFlags & EDGE_LEFT) > 0;
            boolean rightEdge = (edgeFlags & EDGE_RIGHT) > 0;
            boolean topEdge = (edgeFlags & EDGE_TOP) > 0;
            boolean bottomEdge = (edgeFlags & EDGE_BOTTOM) > 0;
            return (x >= this.x || (leftEdge && x <= this.x + this.width))
                    && (x < this.x + this.width || (rightEdge && x >= this.x))
                    && (y >= this.y || (topEdge && y <= this.y + this.height))
                    && (y < this.y + this.height || (bottomEdge && y >= this.y));
        }

        /**
         * Returns the square of the distance between the center of the key and the given point.
         *
         * @param x the x-coordinate of the point
         * @param y the y-coordinate of the point
         * @return the square of the distance of the point from the center of the key
         */
        int squaredDistanceFrom(int x, int y) {
            int xDist = this.x + width / 2 - x;
            int yDist = this.y + height / 2 - y;
            return xDist * xDist + yDist * yDist;
        }

        /**
         * Returns the drawable state for the key, based on the current state and type of the key.
         *
         * @return the drawable state of the key.
         * @see android.graphics.drawable.StateListDrawable#setState(int[])
         */
        @NonNull
        int[] getCurrentDrawableState() {
            int[] states = KEY_STATE_NORMAL;
            if (on) {
                if (pressed) {
                    states = KEY_STATE_PRESSED_ON;
                } else {
                    states = KEY_STATE_NORMAL_ON;
                }
            } else {
                if (sticky) {
                    if (pressed) {
                        states = KEY_STATE_PRESSED_OFF;
                    } else {
                        states = KEY_STATE_NORMAL_OFF;
                    }
                } else {
                    if (pressed) {
                        states = KEY_STATE_PRESSED;
                    }
                }
            }
            return states;
        }
    }
}
