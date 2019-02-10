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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.ReplacementSpan;

import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.model.GLKDrawable;
import com.luxlunae.glk.model.GLKResourceManager;
import com.luxlunae.glk.model.stream.window.GLKTextWindowM;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TableSpan extends ReplacementSpan {

    static final int DEFAULT_CELL_PADDING = 1;
    static final int DEFAULT_CELL_SPACING = 1;
    private static final boolean DEBUG_TABLESPAN = false;
    // thanks http://stackoverflow.com/questions/4731055/whitespace-matching-regex-java
    private static final String BREAK_CHARS = ""       /* dummy empty string for homogeneity */
            + "\\u0009" // CHARACTER TABULATION
            + "\\u000A" // LINE FEED (LF)
            + "\\u000B" // LINE TABULATION
            + "\\u000C" // FORM FEED (FF)
            + "\\u000D" // CARRIAGE RETURN (CR)
            + "\\u0020" // SPACE
            + "\\u0085" // NEXT LINE (NEL)
            + "\\u1680" // OGHAM SPACE MARK
            + "\\u180E" // MONGOLIAN VOWEL SEPARATOR
            + "\\u2000" // EN QUAD
            + "\\u2001" // EM QUAD
            + "\\u2002" // EN SPACE
            + "\\u2003" // EM SPACE
            + "\\u2004" // THREE-PER-EM SPACE
            + "\\u2005" // FOUR-PER-EM SPACE
            + "\\u2006" // SIX-PER-EM SPACE
            + "\\u2007" // FIGURE SPACE
            + "\\u2008" // PUNCTUATION SPACE
            + "\\u2009" // THIN SPACE
            + "\\u200A" // HAIR SPACE
            + "\\u2028" // LINE SEPARATOR
            + "\\u2029" // PARAGRAPH SEPARATOR
            + "\\u205F" // MEDIUM MATHEMATICAL SPACE
            + "\\u3000" // IDEOGRAPHIC SPACE
            ;
    private static final Pattern WORDS = Pattern.compile("[^" + BREAK_CHARS + "]+");
    private final Layout.Alignment mAlign;
    private final HTMLLength mWidth;
    private final int mBorder;
    private final int mCellSpacing;
    private final int mCellPadding;
    @NonNull
    private final ArrayList<Cell> mCells;
    private final GLKTextWindowM mAttachedWin;
    private final Point mSizeTracker;
    private final int mBGColor;
    private final String mBackground;
    @NonNull
    private final Paint mPaint;
    @NonNull
    private final Grid tblDim;
    private final ArrayList<Integer> nextAvailRow = new ArrayList<>();
    private final GLKResourceManager mResMgr;
    int mOpenCellTags = 0;
    private GridColumn[] colDims;
    private int[] rowHeights;
    private int mCurRow = -1;
    private int mRowCount = 0;
    private int mColCount = 0;
    private int mCurCol = 0;
    private Layout.Alignment mCurRowAlign;
    private Parser.VERTICAL_ALIGNMENT mCurRowVAlign;
    private int mCurRowBGColor;
    @Nullable
    private WeakReference<Drawable> mDrawableRef;

    /**
     * @param align       - table position relative to window (where)
     * @param width       - table width relative to window (length)
     * @param border      - controls frame width around table (pixels)
     * @param cellspacing - spacing between cells (pixels)
     * @param cellpadding - spacing within cells (pixels)
     * @param bgcolor
     * @param w
     * @param sizeTracker
     */
    TableSpan(Layout.Alignment align, HTMLLength width, int border, int cellspacing, int cellpadding, int bgcolor, String background,
              GLKTextWindowM w, Point sizeTracker, GLKResourceManager g) {
        if (DEBUG_TABLESPAN) {
            GLKLogger.debug("New table: align = " + align + ", width = " + width + ", border = " + border + ", cellspacing = " + cellspacing + ", cellpadding = " +
                    cellpadding + ", bgcolor = " + bgcolor);
        }
        mAttachedWin = w;
        mSizeTracker = sizeTracker;
        mPaint = new Paint();
        mCells = new ArrayList<>();
        mBGColor = bgcolor;
        mBackground = background;
        mAlign = align;
        mWidth = width;
        mBorder = border;
        mCellPadding = cellpadding;
        mCellSpacing = cellspacing;
        tblDim = new Grid();
        mResMgr = g;
    }

    private static int calcSpannedWidth(@Nullable Spanned s, TextPaint tp, float leadingMult) {
        if (s == null) {
            return 0;
        }
        StaticLayout tempLayout = new StaticLayout(s, tp, 500, Layout.Alignment.ALIGN_NORMAL, leadingMult, 0f, false);
        int lineCount = tempLayout.getLineCount();
        float textWidth = 0;
        for (int x = 0; x < lineCount; x++) {
            textWidth += tempLayout.getLineWidth(x);
        }
        return (int) textWidth;
    }

    private static void calcColumnBounds(TextPaint tp,
                                         @NonNull ArrayList<Cell> cells, @NonNull GridColumn[] colDims, @NonNull Grid tblDim,
                                         float leadingMult, @NonNull Point cellWidth, int cellPad, int cellSpace, int borderSize) {
        // TODO: This function is currently terribly inefficient, can we improve the performance?
        // The minimum width of a column is the narrowest the column can be to fit all of its contents with the harshest
        // possible word-wrapping; so, it's the width of the widest single word (or picture or other element) that
        // can't be split across lines with word-wrapping.
        //
        // The maximum width of a column is the largest of the maximum widths of the cells in the column. The maximum width
        // of a cell is simply the total width of all of the contents of the cell with no word-wrapping; so it's the
        // width of the cell with all of its contents on a single line. Note that this term can be a little misleading,
        // because it isn't a maximum in the sense of a limit on how big the cell can get; rather, it's just a measurement
        // of the most space a cell would need to display all of its contents without any word-wrapping. A cell (or a column)
        // can always be made arbitrarily wider than this "maximum" by padding it with blank space.
        int j;
        int double_cellpad = 2 * (cellPad + borderSize);
        float w;

        // Clear any existing bounds
        tblDim.minWidth = 0;
        tblDim.maxWidth = 0;
        tblDim.actWidth = 0;
        for (GridColumn colDim : colDims) {
            colDim.widthType = WIDTH_TYPE.WIDTH_UNSET;
            colDim.widthTypeVal = 0;
            colDim.minWidth = 0;
            colDim.maxWidth = 0;
            colDim.actWidth = 0;
        }

        // Recalculate

        // Determine bounds
        int cur_width = cellWidth.x;
        cellWidth.set(0, -1);
        for (Cell c : cells) {
            c.updateBounds(tp, leadingMult);

            if (c.mColSpan == 1) {
                // Only include single column cells in the calculation of
                // column and table bounds in this step
                j = c.mGridStart.y;
                colDims[j].maxWidth = Math.max(colDims[j].maxWidth, c.mMaxWidth);
                colDims[j].minWidth = Math.max(colDims[j].minWidth, c.mMinWidth);

                // Set the width type information (percent takes precedence over pixel)
                w = c.mWidth.mValue;
                if (w != HTMLLength.SIZE_TO_CONTENTS && c.mWidth.mIsPercent) {
                    if (colDims[j].widthType == WIDTH_TYPE.WIDTH_PERCENT) {
                        colDims[j].widthTypeVal = Math.max(colDims[j].widthTypeVal, w);
                    } else {
                        colDims[j].widthType = WIDTH_TYPE.WIDTH_PERCENT;
                        colDims[j].widthTypeVal = w;
                    }
                } else if (w != HTMLLength.SIZE_TO_CONTENTS) {
                    colDims[j].widthType = WIDTH_TYPE.WIDTH_PIXEL;
                    colDims[j].widthTypeVal = Math.max(colDims[j].widthTypeVal, w);
                }
            }
        }
        cellWidth.set(cur_width, -1);

        // Work out if we need to adjust the columns and table bounds to accommodate
        // multi-column cells.  The TADS HTML algorithm is defined as follows:
        //
        // [Let "current" column widths be the column widths based on the single column cell pass - i.e. colDims[]]
        //
        //  "For each multi-column cell, we calculate the cell's minimum and maximum widths as normal. We
        //  then add up the "current" minimum widths for the columns, and we add up the current maximum widths
        //  for the spanned columns; let's call these the "total current" minimum and maximum widths for the spanned
        //  columns. For example, if we have a cell that starts in column 2 and has COLSPAN=3, we add up the current
        //  minimum widths of columns 2, 3, and 4, and call this the total current minimum width; likewise for the
        //  total current maximum width.
        //
        //  Next, we compare the cell's minimum and maximum widths to the corresponding total current widths. If the
        //  cell's widths are smaller, we do nothing further, because the spanned columns in aggregate are big enough for
        //  the cell. If the cell's widths are bigger, we calculate the excess of the cell's widths over the corresponding
        //  total current widths. We then distribute this excess width to each spanned column in proportion to the column's
        //  share of the total current width. For example, if the current minimum widths of columns 2, 3, and 4 are 100,
        //  200, and 300 respectively, and we have 60 pixels of excess width to distribute, we distribute one-sixth
        //  (100/(100+200+300)) to column 2, two-sixths to column 3, and three-sixths to column 4.
        //
        //  When we say we "distribute the excess widths," we simply mean that we increase the current column widths
        //  by the proportional share of the excess amount. So, after the example above, the new current minimum widths
        //  of columns 2, 3, and 4 become 110, 220, and 330.
        //
        //  Finally, we go back and repeat the process for the next multi-column cell we find in the table."
        int tot_cur_min, tot_cur_max, excess, start_col, col_count;
        for (Cell c : cells) {
            tot_cur_min = tot_cur_max = 0;
            if (c.mColSpan > 1) {
                if (DEBUG_TABLESPAN) {
                    GLKLogger.debug("multi-column cell detected.");
                }
                start_col = c.mGridStart.y;
                col_count = start_col + c.mColSpan;
                for (j = start_col; j < col_count; j++) {
                    tot_cur_min += colDims[j].minWidth;
                    tot_cur_max += colDims[j].maxWidth;
                }

                if (c.mMinWidth > tot_cur_min) {
                    if (DEBUG_TABLESPAN) {
                        GLKLogger.debug("multi-column cell has minimum width greater than the current total width for the cols it spans... increasing those widths.");
                    }
                    excess = c.mMinWidth - tot_cur_min;
                    for (j = start_col; j < col_count; j++) {
                        colDims[j].minWidth += (int) ((float) excess * (float) colDims[j].minWidth / (float) tot_cur_min);
                    }
                }

                if (c.mMaxWidth > tot_cur_min) {
                    if (DEBUG_TABLESPAN) {
                        GLKLogger.debug("multi-column cell has maximum width greater than the current total width for the cols it spans... increasing those widths.");
                    }
                    excess = c.mMaxWidth - tot_cur_max;
                    for (j = start_col; j < col_count; j++) {
                        colDims[j].maxWidth += (int) ((float) excess * (float) colDims[j].maxWidth / (float) tot_cur_max);
                    }
                }
            }
        }

        // Increase bounds to include cell padding and cell spacing
        for (GridColumn colDim : colDims) {
            colDim.minWidth += (double_cellpad + cellSpace);
            colDim.maxWidth += (double_cellpad + cellSpace);
            tblDim.minWidth += colDim.minWidth;
            tblDim.maxWidth += colDim.maxWidth;
        }
        colDims[0].minWidth += cellSpace;
        colDims[0].maxWidth += cellSpace;

        if (DEBUG_TABLESPAN) {
            GLKLogger.debug("leaving getColBounds(), col_widths = " + getArrayAsString(colDims));
        }
    }

    private static void calcColumnWidths(@NonNull GridColumn[] colDims, @NonNull Grid tblDim) {
        // Determining the column widths.
        //
        // Once we know the overall width of the table, we figure the widths of the
        // individual columns. Since we choose the overall table width before determining the
        // column widths, our job at this point is to determine how the table width is divided
        // among the columns. We use the following rules, in order of priority:
        //
        //     *  The first overriding requirement is that the sum of all of the column widths equals
        //     the table width. That is, a table's columns must exactly fill the table's overall width.
        //     Columns cannot overlap.
        //
        //     * The second overriding requirement is that a column can never be smaller than its
        //     minimum width. (Note that these first two column constraints could contradict one
        //     another if it weren't for the table constraint that says that a table can never be
        //     narrow than the sum of the minimum widths of its columns. That table constraint guarantees
        //     that we'll always be able to satisfy both of these column constraints.)
        //
        //     * If any columns specify percentage widths, we try to accommodate the requests. If this
        //     pushes us over the table size, then we proportionally reduce the widths of the percentage
        //    columns until they fit the table size (but we never reduce a column below its minimum width,
        //    of course).
        //
        //    * If there's any more space after satisfying percentage width requests, we try to satisfy pixel
        //    width requests. If there's enough space to satisfy all pixel width requests, we set each
        //    pixel-width column to its exact requested width; otherwise, we distribute the remaining space
        //    among the pixel-width columns in proportion to the requested widths.
        //
        //    * If there's still any extra space after satisfying percentage and column width requests, we
        //    distribute the extra space among the columns. If there are any columns without any width
        //    requests at all, we distribute all of the extra space to those columns; otherwise, we
        //    distribute the space to all columns. We distribute space as follows:
        //
        //       ** We first distribute space to eligible columns that are below their maximum
        //       column widths. We distribute this space proportionally to the difference between
        //       maximum and minimum widths for these columns. (This odd basis was chosen because
        //       it causes the columns to expand out to their maximum widths evenly as the table width increases.)
        //
        //       ** Once all eligible columns are at their maximum widths, we distribute any remaining
        //       space to the eligible columns in proportion to their maximum widths. (This keeps the
        //       amount of whitespace in each column balanced with the amount of content in that column.)
        //
        //  Note that percentage width requests have higher priority than pixel widths. This means we'll satisfy any
        //  percentage requests (as closely as possible) before satisfying any pixel width requests; so, if there's
        //  not room to satisfy both kinds of requests, any pixel-width columns will be set to their minimum size to
        //   make as much room as possible for percentage-width columns.
        //
        //  When the table is so wide that there's extra space to distribute beyond explicit WIDTH requests, note
        //  that we preferentially distribute the space to unconstrained columns (columns without any WIDTH requests).
        //  We prefer to distribute space to unconstrained columns because this allows columns with explicit WIDTH
        //  requests to stay at the exact size requested.
        //
        int tableWidth = tblDim.actWidth;
        int remWidth = tableWidth;
        float w;
        int w2;
        int nPercentWeight = 0;
        int nPixelWeight = 0;

        if (DEBUG_TABLESPAN) {
            GLKLogger.debug("calcColumnWidths: fitting table to constraint of " + tableWidth);
        }

        // try to satisfy percentage width requests first
        for (GridColumn colDim : colDims) {
            if (colDim.widthType == WIDTH_TYPE.WIDTH_PERCENT) {
                w2 = (int) (colDim.widthTypeVal / 100f * (float) tableWidth);
                nPercentWeight += colDim.widthTypeVal;
                colDim.actWidth = Math.max(w2, colDim.minWidth);
            } else {
                colDim.actWidth = colDim.minWidth;
            }
            remWidth -= colDim.actWidth;
        }
        if (DEBUG_TABLESPAN) {
            GLKLogger.debug("total width after %s = " + (tableWidth - remWidth) + ", col_widths = " + getArrayAsString(colDims));
        }

        if (remWidth < 0) {
            // If there is not enough space to satisfy all percentage width requests, then we proportionally
            // reduce the widths of the percentage columns until they fit the table size
            // (but we never reduce a column below its minimum width, of course).
            if (DEBUG_TABLESPAN) {
                GLKLogger.debug("  table width constraint exceeded - proportionally reduce the widths of the percentage columns until they fit the table size");
            }
            int excess = -remWidth;
            if (nPercentWeight > 0) {
                for (GridColumn colDim : colDims) {
                    if (colDim.widthType == WIDTH_TYPE.WIDTH_PERCENT) {
                        int diff = (int) ((float) excess * (colDim.widthTypeVal / (float) nPercentWeight));
                        if (diff > colDim.actWidth - colDim.minWidth) {
                            int amt = (diff - (colDim.actWidth - colDim.minWidth));
                            excess += amt;
                            nPercentWeight -= amt;
                            colDim.actWidth = colDim.minWidth;
                            remWidth += (colDim.actWidth - colDim.minWidth);
                        } else {
                            colDim.actWidth -= diff;
                            remWidth += diff;
                        }
                    }
                }
            }
            tblDim.actWidth -= remWidth;
            if (DEBUG_TABLESPAN) {
                GLKLogger.debug("final total width = " + tblDim.actWidth + ", col_widths = " + getArrayAsString(colDims));
            }
            return;
        } else if (remWidth > 0) {
            // If there's any more space after satisfying percentage width requests, we try to satisfy pixel
            // width requests.
            for (GridColumn colDim : colDims) {
                if (colDim.widthType == WIDTH_TYPE.WIDTH_PIXEL) {
                    w = colDim.widthTypeVal;
                    nPixelWeight += w;
                    int diff = (int) w - colDim.actWidth;
                    if (diff > 0) {
                        remWidth -= diff;
                        colDim.actWidth += diff;
                    }
                }
            }
        }
        if (DEBUG_TABLESPAN) {
            GLKLogger.debug("total width after pixels = " + (tableWidth - remWidth) + ", col_widths = " + getArrayAsString(colDims));
        }

        if (remWidth < 0) {
            // If there is not enough space to satisfy all pixel width requests, we
            // distribute the remaining space among the pixel-width columns in proportion to the requested widths.
            if (DEBUG_TABLESPAN) {
                GLKLogger.debug("  table width constraint exceeded - proportionally reduce the widths of the pixel columns until they fit the table size");
            }
            int excess = -remWidth;
            if (nPixelWeight > 0) {
                for (GridColumn colDim : colDims) {
                    if (colDim.widthType == WIDTH_TYPE.WIDTH_PIXEL) {
                        int diff = (int) ((float) excess * (colDim.widthTypeVal / (float) nPixelWeight));
                        if (diff > colDim.actWidth - colDim.minWidth) {
                            int amt = (diff - (colDim.actWidth - colDim.minWidth));
                            excess += amt;
                            nPixelWeight -= amt;
                            colDim.actWidth = colDim.minWidth;
                            remWidth += (colDim.actWidth - colDim.minWidth);
                        } else {
                            colDim.actWidth -= diff;
                            remWidth += diff;
                        }
                    }
                }
            }
            tblDim.actWidth -= remWidth;
            if (DEBUG_TABLESPAN) {
                GLKLogger.debug("final total width = " + tblDim.actWidth + ", col_widths = " + getArrayAsString(colDims));
            }
            return;
        } else if (remWidth > 0) {
            // If there's still any extra space after satisfying percentage and column width requests, we
            // distribute the extra space among the columns.  If there are any columns without any width
            // requests at all, we distribute all of the extra space to those columns; otherwise, we
            // distribute the space to all columns.
            boolean areUnsetCols = false;
            for (GridColumn colDim : colDims) {
                if (colDim.widthType == WIDTH_TYPE.WIDTH_UNSET) {
                    areUnsetCols = true;
                    break;
                }
            }

            if (DEBUG_TABLESPAN) {
                GLKLogger.debug("  Still extra space (" + remWidth + " px) after satisfying percentage and pixel width requests.. distributing it among " +
                        (areUnsetCols ? "unset width " : "all ") + "columns...");
            }

            // We first distribute space to eligible columns that are below their maximum
            // column widths. We distribute this space proportionally to the difference between
            // maximum and minimum widths for these columns. (This odd basis was chosen because
            // it causes the columns to expand out to their maximum widths evenly as the table width increases.)
            if (DEBUG_TABLESPAN) {
                GLKLogger.debug(" First distribute based on diff between max and min widths...");
            }

            float totalWeight = 0f;
            int addWidth;
            int widthToDistribute = remWidth;
            for (GridColumn colDim : colDims) {
                if ((!areUnsetCols || colDim.widthType == WIDTH_TYPE.WIDTH_UNSET) && colDim.actWidth < colDim.maxWidth) {
                    totalWeight += (colDim.maxWidth - colDim.minWidth);
                }
            }
            for (GridColumn colDim : colDims) {
                if ((!areUnsetCols || colDim.widthType == WIDTH_TYPE.WIDTH_UNSET) && colDim.actWidth < colDim.maxWidth) {
                    addWidth = Math.min((colDim.maxWidth - colDim.minWidth),
                            (int) ((float) (colDim.maxWidth - colDim.minWidth) / totalWeight * (float) widthToDistribute));
                    colDim.actWidth += addWidth;
                    remWidth -= addWidth;
                }
            }

            if (remWidth > 0) {
                // Once all eligible columns are at their maximum widths, we distribute any remaining
                // space to the eligible columns in proportion to their maximum widths. (This keeps the
                // amount of whitespace in each column balanced with the amount of content in that column.)
                if (DEBUG_TABLESPAN) {
                    GLKLogger.debug("  Still got " + remWidth + " px unassigned, distribute rest in proportion to max widths..");
                }
                widthToDistribute = remWidth;
                totalWeight = 0;
                for (GridColumn colDim : colDims) {
                    if ((!areUnsetCols || colDim.widthType == WIDTH_TYPE.WIDTH_UNSET)) {
                        totalWeight += colDim.maxWidth;
                    }
                }
                for (GridColumn colDim : colDims) {
                    if ((!areUnsetCols || colDim.widthType == WIDTH_TYPE.WIDTH_UNSET)) {
                        addWidth = (int) ((float) colDim.maxWidth / totalWeight * (float) widthToDistribute);
                        colDim.actWidth += addWidth;
                        remWidth -= addWidth;
                    }
                }
            }
        }
        tblDim.actWidth -= remWidth;
        if (DEBUG_TABLESPAN) {
            GLKLogger.debug("final total width = " + tblDim.actWidth + ", col_widths = " + getArrayAsString(colDims));
        }
    }

    private static String getArrayAsString(@NonNull GridColumn[] arr) {
        StringBuilder s = new StringBuilder();
        s.append('[');
        for (GridColumn anArr : arr) {
            s.append(anArr.minWidth).append(',').append(anArr.actWidth).append(',').append(anArr.maxWidth).append('|');
        }
        s.append(']');
        return s.toString();
    }

    private static void calcTableHeight(TextPaint tp, @NonNull ArrayList<Cell> cells, GridColumn[] colDims, @NonNull int[] rowHeights,
                                        @NonNull Grid tblDim, @NonNull Point cellWidth, float leadingMult, int cellPad, int cellSpace, int borderSize) {
        // Step 3 - Work out table's height.
        //
        // Once the column widths are chosen, the renderer performs word-wrapping
        // on the contents of each cell to fit the contents to the final size of the cell's column. This process
        // determines the minimum height of a cell, which is the height required to hold the word-wrapped contents
        // of the cell at its final width. The minimum height of a row is the largest of the minimum heights of the row's cells.
        //
        // If a cell or a row has a HEIGHT attribute, then we'll use the larger of the minimum height and the explicit HEIGHT
        // attribute value.
        //
        // If the <TABLE> tag has a HEIGHT attribute, and the height is larger than the sum of the minimum heights of the
        // rows, then the table's height is the explicitly given HEIGHT; otherwise, the table's height is the sum of the
        // minimum row heights. If the explicit HEIGHT is larger, then we distribute the extra vertical space
        // (the difference between the explicit HEIGHT and the actual minimum height of the rows) among the rows as follows:
        //
        //     * If there are any rows (<TR> tags) with "HEIGHT=*" settings, then we distribute the extra space
        //     evenly among the "HEIGHT=*" rows.
        //
        //     * Otherwise, we distribute the space to the rows in proportion to their minimum heights. (For example,
        //     if there are two rows, and the first row's minimum height is twice that of the second row, then we
        //     distribute 2/3 of the extra vertical space to the first row and 1/3 to the second row.)
        //
        // If a cell has a ROWSPAN greater than 1, we distribute its height among the rows it spans in proportion to the
        // heights of the rows as they would be if we considered only the cells with ROWSPAN=1. The detailed algorithm
        // is almost exactly the same as the one described above for multi-column (COLSPAN) cells.
        int double_cellpad = 2 * (cellPad + borderSize);
        int cur_width, w, i, j, sz;
        int start_col, col_count;

        if (DEBUG_TABLESPAN) {
            GLKLogger.debug("calcTableHeight ... " + rowHeights.length + " rows... ");
        }

        // Clear any existing bounds
        for (i = 0, sz = rowHeights.length; i < sz; i++) {
            rowHeights[i] = 0;
        }

        // Recalculate
        cur_width = cellWidth.x;
        for (Cell c : cells) {
            w = 0;
            start_col = c.mGridStart.y;
            col_count = start_col + c.mColSpan;
            for (j = start_col; j < col_count; j++) {
                w += (j == 0) ? colDims[j].actWidth - double_cellpad - 2 * cellSpace : colDims[j].actWidth - double_cellpad - cellSpace;
            }

            cellWidth.set(w, -1);
            c.updateCellLayout(tp, w, leadingMult);
            cellWidth.set(cur_width, -1);

            if (c.mRowSpan == 1) {
                // Only include single row cells in the calculation of
                // row heights in this step
                i = c.mGridStart.x;
                rowHeights[i] = Math.max(rowHeights[i], c.mActHeight);
            }
        }

        // Work out if we need to adjust row heights for any multi-row cells
        int tot_cur, excess, start_row, row_count;
        for (Cell c : cells) {
            tot_cur = 0;
            if (c.mRowSpan > 1) {
                if (DEBUG_TABLESPAN) {
                    GLKLogger.debug("multi-row cell detected.");
                }
                start_row = c.mGridStart.x;
                row_count = start_row + c.mRowSpan;
                for (i = start_row; i < row_count; i++) {
                    tot_cur += rowHeights[i];
                }

                if (c.mActHeight > tot_cur) {
                    if (DEBUG_TABLESPAN) {
                        GLKLogger.debug("multi-row cell has height greater than the current total height for the rows it spans... increasing those heights.");
                    }
                    excess = c.mActHeight - tot_cur;
                    for (i = start_row; i < row_count; i++) {
                        rowHeights[i] += (int) ((float) excess * (float) rowHeights[i] / (float) tot_cur);
                    }
                }
            }
        }

        // Increase heights to include cell padding and cell spacing
        tblDim.actHeight = 0;
        rowHeights[0] += cellSpace;
        for (i = 0, sz = rowHeights.length; i < sz; i++) {
            rowHeights[i] += (double_cellpad + cellSpace);
            tblDim.actHeight += rowHeights[i];
        }
    }

    private static void calcCellScreenPos(@NonNull ArrayList<Cell> cells, int[] rowHeights, GridColumn[] colDims, int cellspacing) {
        int i, j, a, b, x, y;

        for (Cell c : cells) {
            i = c.mGridStart.x;
            if (i == 0) {
                y = cellspacing;
            } else {
                y = 0;
                for (a = 0; a < i; a++) {
                    y += rowHeights[a];
                }
            }
            j = c.mGridStart.y;
            if (j == 0) {
                x = cellspacing;
            } else {
                x = 0;
                for (b = 0; b < j; b++) {
                    x += colDims[b].actWidth;
                }
            }
            c.setScreenPos(x, y);
        }
    }

    private static void drawEtchedBorder(@NonNull Canvas canvas, int left, int top, int right, int bottom, int top_color, int bottom_color, int borderSize) {
        Paint p = new Paint();
        p.setStyle(Paint.Style.FILL);
        p.setColor(top_color);
        canvas.drawRect(left, top, right, top + borderSize, p);
        canvas.drawRect(left, top, left + borderSize, bottom, p);
        p.setColor(bottom_color);
        canvas.drawRect(left, bottom - borderSize, right, bottom, p);
        canvas.drawRect(right - borderSize, top, right, bottom, p);
    }

    private static void getTableWidth(@NonNull Grid tblDim, int max_width, @NonNull HTMLLength pref_width) {
        // FROM TADS HTML documentation:
        // Figuring the table's width:
        //
        //     * Above all else, the table must be at least as wide as the sum of the minimum widths of all of its
        //     columns. This means that a table must be large enough to accommodate all of its columns without any
        //     clipping or overlapping. If any other rule would choose a table size below this minimum, we always
        //     override that decision and use this minimum table size instead.
        //
        //     * If the <TABLE> tag specifies a WIDTH attribute, then we use that value (except that the
        //     minimum table size above overrides this if the WIDTH value is below the minimum).
        //
        //     * In the absence of a WIDTH attribute, the largest we'll make the table is the full
        //     available space in the display window (except, as always, that we'll never go below the
        //     minimum table size). We set this upper limit so that the table fits in the window without
        //     any horizontal scrolling, since horizontal scrolling would make the table much harder to
        //     read. (It might seem that we're just replacing horizontal scrolling with vertical scrolling,
        //     but in many cases a table won't become tall enough to need vertical scrolling even after being
        //     forced to fit the window's width. Even when a table is very tall, vertical scrolling is preferable to
        //     horizontal for most users, since text games tend to display enough sequential information that vertical
        //     scrolling occurs anyway.)
        //
        //     * If any column has an explicit pixel width specified, we'll try to make the table big enough to
        //     accommodate that pixel width.
        //
        //     * For any column that doesn't have any explicit width specified, we'll try to make the table big enough to
        //     accommodate the maximum width of the column.
        //
        //     * If any column has a percentage width specified, we'll try to make the table big enough that, at the column's
        //     maximum width, the column would have the requested percentage of the total table width. (For example, if a
        //     column specifies a width of 10%, we try to make sure that the table is big enough that the column would only
        //     take up 10% of the total table size if the column were at its maximum size.)
        //
        //     * If some columns have percentage widths and some don't, we try to make the table big enough that the
        //     non-percentage columns would only take up the left-over percentage at their maximum sizes. (For example,
        //     if we have three columns in the table, and the first two specify widths of 20% and 50% respectively, we try
        //     to make sure that the table is big enough that the third column, at its maximum width, would take up only 30% of
        //     the total table width.)
        if (pref_width.mValue == HTMLLength.SIZE_TO_CONTENTS) {
            // Table should expand to take up maximum width possible
            tblDim.actWidth = max_width;
        } else {
            // Table width is either in pixels or a percentage
            int cand_width = pref_width.mIsPercent ? (int) (((float) pref_width.mValue / 100f) * (float) max_width) : pref_width.mValue;
            tblDim.actWidth = Math.min(Math.max(tblDim.minWidth, cand_width), max_width);
        }
    }

    /**
     *
     * @param align - horizontal alignment in cells
     * @param valign - vertical alignment in cells
     * @param bgColor
     */
    void newRow(Layout.Alignment align, Parser.VERTICAL_ALIGNMENT valign, int bgColor) {
        if (DEBUG_TABLESPAN) {
            GLKLogger.debug("New row: align = " + align + ", valign = " + valign + ", bgColor = " + bgColor);
        }
        mCurRowAlign = align;
        mCurRowVAlign = valign;
        mCurRowBGColor = bgColor;
        mCurRow++;
        mColCount = Math.max(mColCount, mCurCol);
        mCurCol = 0;
    }

    /**
     * @param s          - contents of cell
     * @param nowrap     - suppress word wrap
     * @param rowspan    - number of grid rows spanned by cell
     * @param colspan    - number of grid cols spanned by cell
     * @param align      - horizontal alignment in cell
     * @param valign     - vertical alignment in cell
     * @param width      - suggested width for cell (pixels)
     * @param height     - suggested height for cell (pixels)
     * @param bgColor
     * @param background
     */
    void newCell(Spanned s, boolean isheader, boolean nowrap, int rowspan, int colspan,
                 Layout.Alignment align, Parser.VERTICAL_ALIGNMENT valign,
                 HTMLLength width, int height, int bgColor, String background) {
        if (DEBUG_TABLESPAN) {
            GLKLogger.debug("New cell: nowrap = " + nowrap + ", rowspan = " + rowspan + ", colspan = " + colspan +
                    ", halign = " + align + ", valign = " + valign +
                    ", width = " + width + ", height = " + height + ", bgColor = " + bgColor + ", background = " + background + ", text: '" + s + "'");
        }
        if (mCurRow < 0) {
            // This is necessary for tables that have only one row and omit the <TR> tags
            mCurRow = 0;
        }
        while (mCurCol < nextAvailRow.size() && nextAvailRow.get(mCurCol) > mCurRow) {
            mCurCol++;
        }
        mCells.add(new Cell(s, isheader, nowrap, new Point(mCurRow, mCurCol),
                rowspan, colspan, align, valign, width, height, bgColor, background));
        mRowCount = Math.max(mRowCount, mCurRow + rowspan);

        if (nextAvailRow.size() <= mCurCol) {
            nextAvailRow.add(mCurRow + rowspan);
        } else {
            nextAvailRow.set(mCurCol, mCurRow + rowspan);
        }

        mCurCol += colspan;
    }

    void finaliseTable() {
        mColCount = Math.max(mColCount, mCurCol);
        colDims = new GridColumn[mColCount];
        for (int i = 0; i < mColCount; i++) {
            colDims[i] = new GridColumn();
        }
        rowHeights = new int[mRowCount];
    }

    @NonNull
    @Override
    public String toString() {
        return "Table has " + mRowCount + " rows, " + mColCount + " cols:\n" + "TODO: show cell representation";
    }

    /**
     * Measures this table and any of its inner tables.
     * This should be called whenever the width changes.
     *
     * @param tp
     */
    public void measure(TextPaint tp, int max_width) {
        float leadingMult = mAttachedWin.getLeadingMult();

        // (1) Work out table width
        calcColumnBounds(tp, mCells, colDims, tblDim, leadingMult, mSizeTracker, mCellPadding, mCellSpacing, mBorder);
        getTableWidth(tblDim, max_width, mWidth);

        // (2) Work out column widths
        calcColumnWidths(colDims, tblDim);

        // (3) Work out table height
        calcTableHeight(tp, mCells, colDims, rowHeights, tblDim, mSizeTracker, leadingMult, mCellPadding, mCellSpacing, mBorder);

        // (4) Calculate cell pixel coordinates
        calcCellScreenPos(mCells, rowHeights, colDims, mCellSpacing);

        if (DEBUG_TABLESPAN) {
            GLKLogger.debug("table dims: w = [" + tblDim.minWidth + ", " + tblDim.actWidth + ", " + tblDim.maxWidth + "], h = " + tblDim.actHeight);
        }
    }

    @NonNull
    private Drawable getCachedDrawable(@NonNull Context c) {
        // Here we copy the source code for DynamicDrawableSpan
        // TODO: could we just extend that class rather than
        // repeating chunks of it here?
        WeakReference<Drawable> wr = mDrawableRef;
        Drawable d = null;

        if (wr != null)
            d = wr.get();

        if (d == null) {
            d = getDrawable(c.getResources());
            mDrawableRef = new WeakReference<>(d);
        }

        return d;
    }

    @NonNull
    private Drawable getDrawable(Resources res) {
        Bitmap b = Bitmap.createBitmap(tblDim.actWidth, tblDim.actHeight, Bitmap.Config.ARGB_8888);
        drawTable(new Canvas(b));
        BitmapDrawable d = new BitmapDrawable(res, b);
        d.setBounds(0, 0, tblDim.actWidth, tblDim.actHeight);
        return d;
    }

    /**
     * Draws this table onto the given canvas
     *
     * @param canvas
     */
    private void drawTable(@NonNull Canvas canvas) {
        // draw table border
        if (mBorder > 0) {
            drawEtchedBorder(canvas, 0, 0, tblDim.actWidth, tblDim.actHeight, Color.rgb(210, 210, 210), Color.rgb(76, 76, 76), mBorder);
        }

        // draw table background, colour first, then image
        if (mBGColor != Color.TRANSPARENT) {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(mBGColor);
            canvas.drawRect(mBorder, mBorder,
                    tblDim.actWidth - (2 * mBorder),
                    tblDim.actHeight - (2 * mBorder), mPaint);
        }
        if (mBackground != null) {
            GLKDrawable mBackgroundDrawable = mResMgr.getImage(mBackground, tblDim.actWidth - (2 * mBorder),
                    tblDim.actHeight - (2 * mBorder), false);
            if (mBackgroundDrawable != null) {
                canvas.drawBitmap(mBackgroundDrawable.getBitmap(),
                        mBorder, mBorder, null);
            }
        }

        // draw cells
        for (Cell cell : mCells) {
            cell.draw(mResMgr, canvas, mCellPadding, mBorder);
        }
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm) {
        // See http://www.tads.org/t3doc/doc/htmltads/tables.htm for TADS-specific table layout rules
        if (mRowCount == 0 || mColCount == 0) {
            // nothing to display
            return 0;
        }

        if (mSizeTracker.x != -1) {
            // this call to getSize is part of a measure operation
            measure(new TextPaint(paint), mSizeTracker.x);
        }

        // Set the return parameters (height and width)
        if (fm != null) {
            // N.B. ascent and top are negative
            // We centre the table vertically over the baseline
            int halfHeight = (int) ((float) tblDim.actHeight / 2f);
            fm.ascent = fm.top = -halfHeight;
            fm.descent = fm.bottom = halfHeight;
            fm.leading = 0;
        }

        return tblDim.actWidth;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        if (mRowCount == 0 || mColCount == 0) {
            // nothing to draw
            return;
        }

        canvas.save();
        canvas.translate(x, top);
        drawTable(canvas);
        canvas.restore();
    }

    @Nullable
    public StaticLayout getCellForScreenCoords(int x, int y, @NonNull Point cellPos) {
        Rect rect = new Rect();
        int l, t, r, b;
        for (Cell c : mCells) {
            l = c.mScreenPosition.x;
            r = l + c.mActWidth;
            t = c.mScreenPosition.y;
            b = t + c.mActHeight;
            rect.set(l, t, r, b);
            if (rect.contains(x, y)) {
                cellPos.set(c.mScreenPosition.x, c.mScreenPosition.y);
                return c.mLayout;
            }
        }
        return null;
    }


    private enum WIDTH_TYPE {WIDTH_UNSET, WIDTH_PIXEL, WIDTH_PERCENT}

    private static class Cell {
        final int mBGColor;
        final String mBackground;
        final Spanned mText;
        final boolean mIsHeader;
        private final Layout.Alignment mAlign;
        private final Parser.VERTICAL_ALIGNMENT mVAlign;
        private final HTMLLength mWidth;
        private final int mHeight;
        private final Point mGridStart;   // top left grid cell of this cell
        @NonNull
        private final Point mScreenPosition;
        private final int mRowSpan;
        private final int mColSpan;
        private final boolean mNoWrap;
        int mActWidth;
        int mActHeight;
        private StaticLayout mLayout;
        private int mMinWidth;
        private int mMaxWidth;

        Cell(Spanned s, boolean isheader, boolean nowrap, Point gridStart,
             int rowspan, int colspan,
             Layout.Alignment align, Parser.VERTICAL_ALIGNMENT valign,
             HTMLLength width, int height, int bgcolor, String background) {
            mText = s;
            mIsHeader = isheader;
            mGridStart = gridStart;
            mRowSpan = rowspan;
            mColSpan = colspan;
            mAlign = align;
            mVAlign = valign;
            mWidth = width;
            mHeight = height;
            mBGColor = bgcolor;
            mBackground = background;
            mNoWrap = nowrap;
            mScreenPosition = new Point(0, 0);
        }

        void updateBounds(TextPaint tp, float leadingMult) {
            // Find maximum width
            mMaxWidth = calcSpannedWidth(mText, tp, leadingMult);

            // Find minimum width - measure the widest single word in the cell
            if (mText == null) {
                mMinWidth = 0;
            } else {
                if (mNoWrap) {
                    mMinWidth = mMaxWidth;
                } else {
                    Matcher m = WORDS.matcher(mText);
                    mMinWidth = 0;
                    while (m.find()) {
                        mMinWidth = Math.max(mMinWidth, calcSpannedWidth((Spanned) mText.subSequence(m.start(), m.end()), tp, leadingMult));
                    }
                }
            }
        }

        void updateCellLayout(TextPaint paint, int width, float leadingMult) {
            if (mText == null) {
                mActWidth = 0;
                mActHeight = 0;
            } else {
                TextPaint tmp = new TextPaint(paint);
                tmp.setFakeBoldText(mIsHeader);
                mLayout = new StaticLayout(mText, tmp, width, mAlign, leadingMult, 0.0f, false);
                mActWidth = mLayout.getWidth();
                mActHeight = mLayout.getHeight();
                if (mHeight != HTMLLength.SIZE_TO_CONTENTS) {
                    mActHeight = Math.max(mActHeight, mHeight);
                }
            }
        }

        void setScreenPos(int x, int y) {
            mScreenPosition.set(x, y);
        }

        public void draw(@NonNull GLKResourceManager resMgr, @NonNull Canvas canvas, int cellPadding, int borderSize) {
            canvas.save();

            Paint p = new Paint();
            int double_pad = 2 * (cellPadding + borderSize);

            canvas.translate(mScreenPosition.x, mScreenPosition.y);

            // draw cell border (only appears if cell has some contents)
            if (mLayout != null && borderSize > 0) {
                drawEtchedBorder(canvas, 0, 0, mActWidth + double_pad, mActHeight + double_pad, Color.rgb(76, 76, 76), Color.rgb(210, 210, 210), borderSize);
            }

            // draw cell background - colour, then image
            // overlaps any cell padding (TODO: check if this behaviour consistent with the spec)
            if (mBGColor != Color.TRANSPARENT) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(mBGColor);
                canvas.drawRect(borderSize, borderSize,
                        mActWidth + double_pad - (2 * borderSize),
                        mActHeight + double_pad - (2 * borderSize), p);
            }

            if (mBackground != null) {
                GLKDrawable d = resMgr.getImage(mBackground,
                        mActWidth + double_pad - (2 * borderSize),
                        mActHeight + double_pad - (2 * borderSize), false);
                if (d != null) {
                    canvas.drawBitmap(d.getBitmap(),
                            borderSize, borderSize, null);
                }
            }

            // draw cell contents, if any, inside any cell padding
            if (mLayout != null) {
                canvas.translate(cellPadding, cellPadding);
                mLayout.draw(canvas);
            }

            canvas.restore();
        }

        @NonNull
        @Override
        public String toString() {
            // expand inner tables
            StringBuilder sb = new StringBuilder();
            sb.append("[CELL ");
            sb.append(mText.toString());
            TableSpan[] spans = mText.getSpans(0, mText.length(), TableSpan.class);
            if (spans.length > 0) {
                sb.append(" (with table spans:) ");
            }
            for (TableSpan s : spans) {
                int start = mText.getSpanStart(s);
                int end = mText.getSpanEnd(s);
                sb.append(s.toString());
                sb.append(" from ").append(start).append(" to ").append(end);
            }
            sb.append("]");
            return sb.toString();
        }
    }

    private class Grid {
        int minWidth;
        int actWidth;
        int maxWidth;
        int actHeight;
        WIDTH_TYPE width_type;
        float widthTypeVal;
    }

    private class GridColumn {
        int minWidth;
        int actWidth;
        int maxWidth;
        WIDTH_TYPE widthType;
        float widthTypeVal;
    }
}
