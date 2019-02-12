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
package com.luxlunae.glk.model.stream;

import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.model.GLKModel;
import com.luxlunae.glk.model.stream.file.GLKFileRef;
import com.luxlunae.glk.model.stream.file.GLKFileStream;
import com.luxlunae.glk.model.stream.file.GLKMemoryStream;
import com.luxlunae.glk.model.stream.sound.GLKSoundStream;
import com.luxlunae.glk.model.stream.window.GLKGraphicsM;
import com.luxlunae.glk.model.stream.window.GLKNonPairM;
import com.luxlunae.glk.model.stream.window.GLKPairM;
import com.luxlunae.glk.model.stream.window.GLKTextBufferM;
import com.luxlunae.glk.model.stream.window.GLKTextGridM;
import com.luxlunae.glk.model.stream.window.GLKTextWindowM;
import com.luxlunae.glk.model.stream.window.GLKWindowM;

import java.io.IOException;
import java.util.ArrayList;

public class GLKStreamManager {

    @NonNull
    private final SparseArray<GLKStream> mStreams = new SparseArray<>();
    @NonNull
    private final ArrayList<GLKFileRef> mTmpFiles = new ArrayList<>();
    private int mCurOutStreamID = GLKConstants.NULL;
    private int mNextID = 1;    // start at 1 rather than 0, as 0 = GLKConstants.NULL

    @NonNull
    private static String getStreamType(@NonNull GLKStream s) {
        if (s instanceof GLKTextGridM) {
            return "Text Grid";
        }
        if (s instanceof GLKTextBufferM) {
            return "Text Buffer";
        }
        if (s instanceof GLKPairM) {
            return "Pair";
        }
        if (s instanceof GLKGraphicsM) {
            return "Graphics";
        }
        if (s instanceof GLKSoundStream) {
            return "Sound";
        }
        if (s instanceof GLKFileStream) {
            return "File";
        }
        if (s instanceof GLKMemoryStream) {
            return "Memory File";
        }
        return "Unknown";
    }

    public void registerTempFile(@NonNull GLKFileRef f) {
        mTmpFiles.add(f);
    }

    public void deleteTempFiles() {
        for (GLKFileRef f : mTmpFiles) {
            if (!f.delete()) {
                GLKLogger.warn("deleteTempFiles: could not delete: " + f.getPath());
            } else {
                GLKLogger.debug("deleteTempFiles: deleted " + f.getPath());
            }
        }
    }

    public int getWindowType(int win) {
        GLKStream s = mStreams.get(win);
        if (s instanceof GLKTextBufferM) {
            return GLKConstants.wintype_TextBuffer;
        }
        if (s instanceof GLKTextGridM) {
            return GLKConstants.wintype_TextGrid;
        }
        if (s instanceof GLKPairM) {
            return GLKConstants.wintype_Pair;
        }
        if (s instanceof GLKGraphicsM) {
            return GLKConstants.wintype_Graphics;
        }
        return GLKConstants.NULL;
    }

    public int getNextStream(int ref, @Nullable int[] rock) {
        int start = (ref == GLKConstants.NULL) ? 0 : mStreams.indexOfKey(ref) + 1;
        int ret = GLKConstants.NULL;
        GLKStream s;

        for (int i = start, sz = mStreams.size(); i < sz; i++) {
            s = mStreams.valueAt(i);
            if (s != null) {
                ret = mStreams.keyAt(i);
                if (rock != null) {
                    rock[0] = s.getRock();
                }
                break;
            }
        }
        return ret;
    }

    public int getNextWindow(int win, @Nullable int[] rock) {
        int start = (win == GLKConstants.NULL) ? 0 : mStreams.indexOfKey(win) + 1;
        int ret = GLKConstants.NULL;
        GLKStream s;

        for (int i = start, sz = mStreams.size(); i < sz; i++) {
            s = mStreams.valueAt(i);
            if (s instanceof GLKWindowM) {
                ret = mStreams.keyAt(i);
                if (rock != null) {
                    rock[0] = s.getRock();
                }
                break;
            }
        }
        return ret;
    }

    @Nullable
    public GLKWindowM getNextWindow(@Nullable GLKWindowM win) {
        int start = (win == null) ? 0 : mStreams.indexOfKey(win.getStreamId()) + 1;
        GLKWindowM ret = null;
        GLKStream s;

        for (int i = start, sz = mStreams.size(); i < sz; i++) {
            s = mStreams.valueAt(i);
            if (s instanceof GLKWindowM) {
                ret = (GLKWindowM) s;
                break;
            }
        }
        return ret;
    }

    public int getNextFRef(int fref, @Nullable int[] rock) {
        int start = (fref == GLKConstants.NULL) ? 0 : mStreams.indexOfKey(fref) + 1;
        int ret = GLKConstants.NULL;
        GLKStream s;

        for (int i = start, sz = mStreams.size(); i < sz; i++) {
            s = mStreams.valueAt(i);
            if (s instanceof GLKFileRef) {
                ret = mStreams.keyAt(i);
                if (rock != null) {
                    rock[0] = s.getRock();
                }
                break;
            }
        }
        return ret;
    }

    public int getNextSChannel(int ref, @Nullable int[] rock) {
        int start = (ref == GLKConstants.NULL) ? 0 : mStreams.indexOfKey(ref) + 1;
        int ret = GLKConstants.NULL;
        GLKStream s;

        for (int i = start, sz = mStreams.size(); i < sz; i++) {
            s = mStreams.valueAt(i);
            if (s instanceof GLKSoundStream) {
                ret = mStreams.keyAt(i);
                if (rock != null) {
                    rock[0] = s.getRock();
                }
                break;
            }
        }
        return ret;
    }

    @Nullable
    public GLKTextWindowM getFirstTextWindow() {
        for (int i = 0, sz = mStreams.size(); i < sz; i++) {
            GLKStream s = mStreams.valueAt(i);
            if (s instanceof GLKTextWindowM) {
                return (GLKTextWindowM) s;
            }
        }
        return null;
    }

    @NonNull
    public String getStreamInfo() {
        StringBuilder ret = new StringBuilder();
        ret.append("Active streams:\n");
        for (int i = 0, sz = mStreams.size(); i < sz; i++) {
            int strId = mStreams.keyAt(i);
            GLKStream s = mStreams.valueAt(i);
            if (s != null) {
                ret.append("\t").append(strId).append(": ").append(getStreamType(s)).append("\n");
                ret.append("\t\t").append("rock: ").append(s.getRock()).append("\n");
                if (s instanceof GLKInputStream) {
                    ret.append("\t\t").append("read: ").append(((GLKInputStream) s).getReadCount()).append("\n");
                }
                if (s instanceof GLKOutputStream) {
                    ret.append("\t\t").append("written: ").append(((GLKOutputStream) s).getWriteCount()).append("\n");
                }
            }
        }
        ret.append("\n");
        return ret.toString();
    }

    @NonNull
    public String getWindowInfo() {
        StringBuilder ret = new StringBuilder();
        Point po;
        ret.append("Open windows:\n");
        for (int i = 0, sz = mStreams.size(); i < sz; i++) {
            int winId = mStreams.keyAt(i);
            GLKWindowM w = getWindow(winId);
            if (w != null) {
                po = w.getGLKSize();
                ret.append("\t").append(winId).append(": ").append(getStreamType(w)).append("\n");
                ret.append("\t\tparent -> ").append(w.getParentId()).append("\n");
                ret.append("\t\tsibling -> ").append(w.getSiblingId()).append("\n");
                ret.append("\t\twidth: ").append(w.getWidth()).append(" px (").append(po.x);
                if (w instanceof GLKGraphicsM) {
                    ret.append(" dp)\n");
                } else {
                    ret.append(" cols)\n");
                }
                ret.append("\t\theight: ").append(w.getHeight()).append(" px (").append(po.y);
                if (w instanceof GLKGraphicsM) {
                    ret.append(" dp)\n");
                } else {
                    ret.append(" rows)\n");
                }
                if (w instanceof GLKPairM) {
                    GLKPairM p = (GLKPairM) w;
                    int kwID = p.getKeyWinID();
                    int meth = p.getSplitMethod();
                    ret.append("\t\tshow borders = ").append(p.showingBorder()).append("\n");
                    ret.append("\t\tkeywin -> ").append(kwID).append("\n");
                    ret.append("\t\tchild1 -> ").append(p.getChild1().getStreamId()).append("\n");
                    ret.append("\t\tchild2 -> ").append(p.getChild2().getStreamId()).append("\n");
                    ret.append("\t\tsplit method = ").append(GLKConstants.winmethodToString(meth)).append("\n");
                    ret.append("\t\tsplit size = ").append(p.getSplitSize());
                    GLKNonPairM kw = getNonPairWindow(kwID);
                    if (kw != null) {
                        if ((meth & GLKConstants.winmethod_DivisionMask) == GLKConstants.winmethod_Fixed) {
                            if (kw instanceof GLKGraphicsM) {
                                ret.append(" dp\n");
                            } else {
                                int dir = (meth & GLKConstants.winmethod_DirMask);
                                if (dir == GLKConstants.winmethod_Above || dir == GLKConstants.winmethod_Below) {
                                    ret.append(" rows\n");
                                } else {
                                    ret.append(" cols\n");
                                }
                            }
                        } else {
                            ret.append("%\n");
                        }
                    }
                }
            }
        }
        ret.append("\n");
        return ret.toString();
    }

    public void addStreamToPool(@NonNull GLKStream str) {
        // Returns the ID assigned to stream 'str'
        mStreams.append(mNextID, str);
        str.setStreamId(mNextID);
        mNextID++;
    }

    public void closeStream(int id) {
        GLKStream s = mStreams.get(id);
        if (s != null) {
            mStreams.remove(id);
            s.setStreamId(GLKConstants.NULL);
            try {
                s.close();
            } catch (IOException e) {
                GLKLogger.error("GLKStreamManager: closeAllStreams: could not close stream " + id);
            }
        }
        if (mCurOutStreamID == id) {
            mCurOutStreamID = GLKConstants.NULL;
        }
    }

    public void closeAllStreams(int rootWinID) {
        int id;

        // close the windows first - by closing the root window
        // this is the most efficient way to shut down that structure
        // as we don't backtrack to parents
        closeStream(rootWinID);

        // now try to close any other streams
        ArrayList<Integer> streams = new ArrayList<>();
        for (int i = 0, sz = mStreams.size(); i < sz; i++) {
            id = mStreams.keyAt(i);
            streams.add(id);
        }
        for (int i = 0, sz = streams.size(); i < sz; i++) {
            closeStream(streams.get(i));
        }
    }

    public void setHTMLForAllTextBuffers(@NonNull GLKModel m, boolean on) {
        for (int i = 0, sz = mStreams.size(); i < sz; i++) {
            GLKStream s = mStreams.valueAt(i);
            if (s instanceof GLKTextBufferM) {
                ((GLKTextBufferM) s).setHTMLOutput(m, on);
            }
        }
    }

    public int getCurrentOutputStream() {
        return mCurOutStreamID;
    }

    public void setCurrentOutputStream(int id) {
        mCurOutStreamID = id;
    }

    public GLKStream getStream(int id) {
        return mStreams.get(id);
    }

    @Nullable
    public GLKInputStream getInputStream(int id) {
        GLKStream s = mStreams.get(id);
        if (s instanceof GLKInputStream) {
            return (GLKInputStream) s;
        }
        return null;
    }

    @Nullable
    public GLKOutputStream getOutputStream(int id) {
        GLKStream s = mStreams.get(id);
        if (s instanceof GLKOutputStream) {
            return (GLKOutputStream) s;
        }
        return null;
    }

    @Nullable
    public GLKWindowM getWindow(int id) {
        GLKStream s = mStreams.get(id);
        if (s instanceof GLKWindowM) {
            return (GLKWindowM) s;
        }
        return null;
    }

    @Nullable
    public GLKPairM getPairWindow(int id) {
        GLKStream s = mStreams.get(id);
        if (s instanceof GLKPairM) {
            return (GLKPairM) s;
        }
        return null;
    }

    @Nullable
    public GLKNonPairM getNonPairWindow(int id) {
        GLKStream s = mStreams.get(id);
        if (s instanceof GLKNonPairM) {
            return (GLKNonPairM) s;
        }
        return null;
    }

    @Nullable
    public GLKSoundStream getSoundStream(int id) {
        GLKStream s = mStreams.get(id);
        if (s instanceof GLKSoundStream) {
            return (GLKSoundStream) s;
        }
        return null;
    }

    @Nullable
    public GLKFileRef getFileRef(int id) {
        GLKStream s = mStreams.get(id);
        if (s instanceof GLKFileRef) {
            return (GLKFileRef) s;
        }
        return null;
    }

    @Nullable
    public GLKGraphicsM getGraphics(int id) {
        GLKStream s = mStreams.get(id);
        if (s instanceof GLKGraphicsM) {
            return (GLKGraphicsM) s;
        }
        return null;
    }

    @Nullable
    public GLKTextWindowM getTextWindow(int id) {
        GLKStream s = mStreams.get(id);
        if (s instanceof GLKTextWindowM) {
            return (GLKTextWindowM) s;
        }
        return null;
    }

    @Nullable
    public GLKTextGridM getTextGrid(int id) {
        GLKStream s = mStreams.get(id);
        if (s instanceof GLKTextGridM) {
            return (GLKTextGridM) s;
        }
        return null;
    }

    @Nullable
    public GLKTextBufferM getTextBuffer(int id) {
        GLKStream s = mStreams.get(id);
        if (s instanceof GLKTextBufferM) {
            return (GLKTextBufferM) s;
        }
        return null;
    }
}
