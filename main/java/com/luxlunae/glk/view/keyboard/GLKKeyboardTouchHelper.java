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

import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.widget.ExploreByTouchHelper;
import android.view.accessibility.AccessibilityEvent;

import com.luxlunae.glk.GLKConstants;

import java.util.List;

import static com.luxlunae.glk.view.keyboard.GLKKeyboardView.NOT_A_KEY;

public class GLKKeyboardTouchHelper extends ExploreByTouchHelper {
    @NonNull
    private final GLKKeyboard mKeyboard;
    @NonNull
    private final GLKKeyboardView mKeyboardView;

    /**
     * Constructs a new helper that can expose a virtual view hierarchy for the
     * specified host view.
     *
     * @param host view whose virtual view hierarchy is exposed by this helper
     */
    GLKKeyboardTouchHelper(@NonNull GLKKeyboardView host, @NonNull GLKKeyboard keyboard) {
        super(host);
        mKeyboard = keyboard;
        mKeyboardView = host;
    }

    private static CharSequence getDescriptionForUnicode(int unicode) {
        if (Character.isLetter(unicode)) {
            return String.valueOf((char) unicode);
        } else if (Character.isSpaceChar(unicode)) {
            return "space";
        } else {
            switch (unicode) {
                case 8:
                case GLKConstants.keycode_Delete:
                    return "delete";
                case 9:
                case GLKConstants.keycode_Tab:
                    return "tab";
                case 10:
                case GLKConstants.keycode_Return:
                    return "enter";
                case 27:
                case GLKConstants.keycode_Escape:
                    return "escape";
                case 8592:
                case GLKConstants.keycode_Left:
                    return "left arrow";
                case 8593:
                case GLKConstants.keycode_Up:
                    return "up arrow";
                case 8594:
                case GLKConstants.keycode_Right:
                    return "right arrow";
                case 8595:
                case GLKConstants.keycode_Down:
                    return "down arrow";
                case 8670:
                case GLKConstants.keycode_PageUp:
                    return "page up";
                case 8671:
                case GLKConstants.keycode_PageDown:
                    return "page down";
                default:
                    return "unicode " + unicode;
            }
        }
    }

    /**
     * Provides a mapping between view-relative coordinates and logical items.
     *
     * @param x - The view-relative x coordinate
     * @param y - The view-relative y coordinate
     * @return virtual view identifier for the logical item under coordinates (x,y)
     * or HOST_ID if there is no item at the given coordinates.
     */
    @Override
    protected int getVirtualViewAt(float x, float y) {
        // Return the key at (x, y)
        int index = mKeyboardView.getKeyIndex((int) x, (int) y);
        if (index != NOT_A_KEY) {
            return index;
        }
        return ExploreByTouchHelper.INVALID_ID;
    }

    /**
     * Populates a list with the view's visible items. The ordering of items
     * within virtualViewIds specifies order of accessibility focus traversal.
     *
     * @param virtualViewIds - The list to populate with visible items.
     */
    @Override
    protected void getVisibleVirtualViews(@NonNull List<Integer> virtualViewIds) {
        // Developer must guarantee virtual view ids are: one-to-one, stable and non-negative
        // We use the key indices
        List<GLKKeyboard.Key> keys = mKeyboard.getKeys();
        int count = keys.size();
        for (int index = 0; index < count; index++) {
            virtualViewIds.add(index);
        }
    }

    // Speaks the index and value. Informative, but not verbose.
    private CharSequence getDescriptionForIndex(int index) {
        List<GLKKeyboard.Key> keys = mKeyboard.getKeys();
        if (keys != null) {
            GLKKeyboard.Key k = keys.get(index);
            if (k.label != null && !k.label.equals("")) {
                return k.label;
            } else if (k.codes != null && k.codes.length > 0) {
                // try to work out a description based on the first key code
                return getDescriptionForUnicode(k.codes[0]);
            }
        }
        return "blank";
    }

    /**
     * Populates an AccessibilityEvent with information about the specified item.
     *
     * The helper class automatically populates the following fields based on the
     * values set by onPopulateNodeForVirtualView(int, AccessibilityNodeInfoCompat),
     * but implementations may optionally override them:
     *
     *  event text, see getText()
     *  content description, see setContentDescription(CharSequence)
     *  scrollability, see setScrollable(boolean)
     *  password state, see setPassword(boolean)
     *  enabled state, see setEnabled(boolean)
     *  checked state, see setChecked(boolean)
     *
     * The following required fields are automatically populated by the helper class and may not be overridden:
     *
     *  item class name, set to the value used in onPopulateNodeForVirtualView(int, AccessibilityNodeInfoCompat)
     *  package name, set to the package of the host view's Context, see setPackageName(CharSequence)
     *  event source, set to the host view and virtual view identifier, see setSource(AccessibilityRecord, View, int)
     *
     * @param virtualViewId - The virtual view id for the item for which to populate the event.
     * @param evt - The event to populate.
     */
    @Override
    protected void onPopulateEventForVirtualView(int virtualViewId, @NonNull AccessibilityEvent evt) {
        CharSequence desc = getDescriptionForIndex(virtualViewId);
        evt.setContentDescription(desc);
    }

    /**
     * Populates an AccessibilityNodeInfoCompat with information about the specified item.
     *
     * Implementations must populate the following required fields:
     *
     *   event text, see setText(CharSequence) or setContentDescription(CharSequence)
     *   bounds in parent coordinates, see setBoundsInParent(Rect)
     *
     * The helper class automatically populates the following fields with default values, but implementations may optionally override them:
     *
     *   enabled state, set to true, see setEnabled(boolean)
     *   keyboard focusability, set to true, see setFocusable(boolean)
     *   item class name, set to android.view.View, see setClassName(CharSequence)
     *
     * The following required fields are automatically populated by the helper class and may not be overridden:
     *
     *   package name, identical to the package name set by onPopulateEventForVirtualView(int, AccessibilityEvent), see setPackageName(CharSequence)
     *   node source, identical to the event source set in onPopulateEventForVirtualView(int, AccessibilityEvent), see setSource(View, int)
     *   parent view, set to the host view, see setParent(View)
     *   visibility, computed based on parent-relative bounds, see setVisibleToUser(boolean)
     *   accessibility focus, computed based on internal helper state, see setAccessibilityFocused(boolean)
     *   keyboard focus, computed based on internal helper state, see setFocused(boolean)
     *   bounds in screen coordinates, computed based on host view bounds, see setBoundsInScreen(Rect)
     *
     * Additionally, the helper class automatically handles keyboard focus and accessibility focus management
     * by adding the appropriate ACTION_FOCUS, ACTION_CLEAR_FOCUS, ACTION_ACCESSIBILITY_FOCUS, or
     * ACTION_CLEAR_ACCESSIBILITY_FOCUS actions. Implementations must never manually add these actions.
     *
     * The helper class also automatically modifies parent- and screen-relative bounds to reflect
     * the portion of the item visible within its parent.
     *
     * @param virtualViewId - The virtual view identifier of the item for which to populate the node.
     * @param node - The node to populate.
     */
    @Override
    protected void onPopulateNodeForVirtualView(int virtualViewId, @NonNull AccessibilityNodeInfoCompat node) {
        // Node and event descriptions are usually identical.
        CharSequence desc = getDescriptionForIndex(virtualViewId);
        node.setContentDescription(desc);

        // Since the user can tap a bar, add the CLICK action.
        //  node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);

        // Reported bounds should be consistent with onDraw().
        GLKKeyboard.Key k = mKeyboard.getKeys().get(virtualViewId);
        Rect bounds = new Rect(k.x, k.y, k.x + k.width, k.y + k.height);
        node.setBoundsInParent(bounds);
    }

    /**
     * Performs the specified accessibility action on the item associated with the
     * virtual view identifier. See performAction(int, Bundle) for more information.
     *
     * Implementations must handle any actions added manually in
     * onPopulateNodeForVirtualView(int, AccessibilityNodeInfoCompat).
     *
     * The helper class automatically handles focus management resulting from
     * ACTION_ACCESSIBILITY_FOCUS and ACTION_CLEAR_ACCESSIBILITY_FOCUS actions.
     *
     * @param virtualViewId - The virtual view identifier of the item on which to perform the action.
     * @param action - The accessibility action to perform.
     * @param arguments - (Optional) A bundle with additional arguments, or null.
     *
     * @return true if the action was performed.
     */
    @Override
    protected boolean onPerformActionForVirtualView(int virtualViewId, int action, @Nullable Bundle arguments) {
        switch (action) {
            case AccessibilityNodeInfoCompat.ACTION_CLICK:
                // Click handling should be consistent with onTouchEvent().
                //       onBarClicked(virtualViewId);
                return true;
        }

        // Only need to handle actions added in populateNode.
        return false;
    }
}
