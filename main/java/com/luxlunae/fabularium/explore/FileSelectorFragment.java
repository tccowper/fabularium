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
package com.luxlunae.fabularium.explore;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.luxlunae.fabularium.AsyncProgressFragment;
import com.luxlunae.fabularium.FabLogger;
import com.luxlunae.fabularium.MainActivity;
import com.luxlunae.fabularium.PreferencesActivity;
import com.luxlunae.fabularium.R;
import com.luxlunae.fabularium.RunProgramResultReceiver;
import com.luxlunae.fabularium.RunProgramService;
import com.luxlunae.fabularium.TextEditorActivity;
import com.luxlunae.fabularium.play.GameSelectorFragment;
import com.luxlunae.fabularium.play.IFictionDbHelper;
import com.luxlunae.glk.GLKConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.luxlunae.fabularium.MainActivity.POS_TAB_EXPLORE;

/**
 * The main interface that appears under the EXPLORE tab and allows the user to browse and manage
 * their file system.
 */
public class FileSelectorFragment extends Fragment
        implements OnItemClickListener,
        AsyncProgressFragment.CopyFilesCallback,
        AsyncProgressFragment.RenameFilesCallback,
        AsyncProgressFragment.DeleteFilesCallback,
        AsyncProgressFragment.UnzipFilesCallback {
    private static final boolean DEBUG_LIFECYCLE = false;

    private static final String WORKING_DIR = "com.luxlunae.fabularium.WORKING_DIR";
    private static final String AUTO_COPY_TAG = "com.luxlunae.fabularium.AUTO_COPY";
    private static final String MANUAL_COPY_TAG = "com.luxlunae.fabularium.MANUAL_COPY";
    private static final String NORMAL_COPY_TAG = "com.luxlunae.fabularium.NORMAL_COPY";

    @Nullable
    private RunProgramResultReceiver mGameUtilReceiver;
    @Nullable
    private File mWorkingDir;
    @NonNull
    private DISPLAY_TYPE mCurrentDisplay = DISPLAY_TYPE.NORMAL;
    @Nullable
    private ArrayList<File> mSrcFiles;
    @Nullable
    private ArrayList<File> mMountPoints;
    private ListView mListView;
    private TextView mPathView;
    @Nullable
    private FileListAdapter mAdapter;
    @Nullable
    private IFictionDbHelper mDbHelper;

    @NonNull
    private ArrayList<File> getMountPoints() {
        ArrayList<File> mountPoints = new ArrayList<>();
        Shortcut s;
        s = new Shortcut(Environment.getExternalStorageDirectory().getAbsolutePath());
        s.setDisplayText(getString(R.string.primary_storage));
        mountPoints.add(s);
        // mountPoints.add(Environment.getRootDirectory());
        // mountPoints.add(Environment.getDataDirectory());

        // From Android developer documentation:
        //   getExternalFilesDirs:
        //      "... Returns absolute paths to application-specific directories
        //       on all shared/external storage devices where the application can
        //       place persistent files it owns. These files are internal to the
        //       application, and not typically visible to the user as media.
        //
        //       This is like getFilesDir() in that these files will be deleted
        //       when the application is uninstalled..."
        Context c = getContext();
        if (c != null) {
            File[] externalStorageFiles = ContextCompat.getExternalFilesDirs(c, null);
            for (File f : externalStorageFiles) {
                if (f != null && !mountPoints.contains(f)) {
                    s = new Shortcut(f.getAbsolutePath());
                    s.setDisplayText(getString(R.string.private_storage));
                    mountPoints.add(s);
                }
            }
        }

        // Try to detect and add any other relevant mount points listed in the proc file
        // Thanks
        //   https://stackoverflow.com/questions/6824463/how-to-get-all-the-mount-point-information-for-android-device
        //   https://stackoverflow.com/questions/9340332/how-can-i-get-the-list-of-mounted-external-storage-of-android-device/19982338
        BufferedReader buf_reader = null;
        try {
            // From https://www.kernel.org/doc/Documentation/filesystems/proc.txt:
            //   3.5	/proc/<pid>/mountinfo - Information about mounts
            //   --------------------------------------------------------
            //    This file contains lines of the form:
            //        36 35 98:0 /mnt1 /mnt2 rw,noatime master:1 - ext3 /dev/root rw,errors=continue
            //        (1)(2)(3)   (4)   (5)      (6)      (7)   (8) (9)   (10)         (11)
            //
            // (1) mount ID:  unique identifier of the mount (may be reused after umount)
            // (2) parent ID:  ID of parent (or of self for the top of the mount tree)
            // (3) major:minor:  value of st_dev for files on filesystem
            // (4) root:  root of the mount within the filesystem
            // (5) mount point:  mount point relative to the process's root
            // (6) mount options:  per mount options
            // (7) optional fields:  zero or more fields of the form "tag[:value]"
            // (8) separator:  marks the end of the optional fields
            // (9) filesystem type:  name of filesystem of the form "type[.subtype]"
            // (10) mount source:  filesystem specific information or "none"
            // (11) super options:  per super block options
            buf_reader = new BufferedReader(new FileReader("/proc/self/mountinfo"));
            String line;
            while ((line = buf_reader.readLine()) != null) {
                if (line.contains("/mnt") || line.contains("/storage")) {
                    // From http://www.linfo.org/mnt.html:
                    //     "The /mnt directory and its subdirectories are intended for use as the
                    //      temporary mount points for mounting storage devices, such as CDROMs,
                    //      floppy disks and USB (universal serial bus) key drives...
                    //
                    //      Although /mnt exists specifically for mounting storage devices,
                    //      other directories can also be used for this purpose..."
                    //
                    // We also include /storage as that seems to be where most OEMs mount secondary
                    // SD cards.
                    StringTokenizer tokens = new StringTokenizer(line, " ");
                    tokens.nextToken();                                 // mount id
                    tokens.nextToken();                                 // parent ID
                    tokens.nextToken();                                 // major:minor
                    Shortcut mount_point = new Shortcut(tokens.nextToken());    // mount point
                    if (mountPoints.contains(mount_point)) {
                        continue;
                    }
                    //  List<String> flags = Arrays.asList(tokens.nextToken().split(",")); // mount options
                    if (line.contains("/dev/block/vold")) {
                        if (!line.contains("/mnt/secure")
                                && !line.contains("/mnt/asec")
                                && !line.contains("/mnt/obb")
                                && !line.contains("/dev/mapper")
                                && !line.contains("tmpfs")) {
                            mount_point.setDisplayText(getString(R.string.secondary_storage));
                            mountPoints.add(mount_point);
                        }
                    }
                }
            }
        } catch (NoSuchElementException e) {
            // thrown by nextToken() if there are no more tokens in this tokenizer's string.
            // do nothing
        } catch (FileNotFoundException e) {
            // do nothing
        } catch (IOException e) {
            // do nothing
        } finally {
            if (buf_reader != null) {
                try {
                    buf_reader.close();
                } catch (IOException ex) {
                    // do nothing
                }
            }
        }

        // Now filter: check we can actually display the contents of these mount points and remove
        // any for which we can't
        List<File> toRemove = new ArrayList<>();
        for (File f : mountPoints) {
            if (f.listFiles() == null) {
                toRemove.add(f);
            }
        }
        mountPoints.removeAll(toRemove);

        return mountPoints;
    }

    /**
     * Called to do initial creation of a fragment. This is called after onAttach(Activity) and
     * before onCreateView(LayoutInflater, ViewGroup, Bundle), but is not called if the
     * fragment instance is retained across Activity re-creation (see setRetainInstance(boolean)).
     * <p>
     * Note that this can be called while the fragment's activity is still in the process of being
     * created. As such, you can not rely on things like the activity's content view hierarchy
     * being initialized at this point. If you want to do work once the activity itself is created,
     * see onActivityCreated(Bundle).
     * <p>
     * If your app's targetSdkVersion is M or lower, child fragments being restored from the
     * savedInstanceState are restored after onCreate returns. When targeting N or above and
     * running on an N or newer platform version they are restored by Fragment.onCreate.
     * <p>
     * If you override this method you must call through to the superclass implementation.
     *
     * @param savedInstanceState -   If the fragment is being re-created from a previous
     *                           saved state, this is the state. This value may be null.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (DEBUG_LIFECYCLE) {
            FabLogger.debug("FileSelector: onCreate()");
        }
        super.onCreate(savedInstanceState);
        setupServiceReceiver();
        setHasOptionsMenu(true);
        FragmentActivity act = getActivity();
        if (act != null) {
            mDbHelper = new IFictionDbHelper(act);
        } else {
            // According to Android docs, onCreate is always called after onAttach, so a getActivity() call here shouldn't return NULL
            FabLogger.error("ANDROID ERROR: FileSelector: getActivity() returned NULL in onCreate! This should not happen! Expect failure.");
        }
        mMountPoints = getMountPoints();
    }

    /**
     * Called to have the fragment instantiate its user interface view. This is optional, and
     * non-graphical fragments can return null (which is the default implementation). This will be
     * called between onCreate(Bundle) and onActivityCreated(Bundle).
     * <p>
     * If you return a View from here, you will later be called in onDestroyView() when the view is being released.
     *
     * @param inflater           - The LayoutInflater object that can be used to inflate any views in the fragment,
     * @param container          - If non-null, this is the parent view that the fragment's UI should be attached to.
     *                           The fragment should not add the view itself, but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState -  If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return - Return the View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (DEBUG_LIFECYCLE) {
            FabLogger.debug("FileSelector: onCreateView()");
        }
        View v = inflater.inflate(R.layout.file_selector, container, false);
        mPathView = v.findViewById(R.id.tvPath);
        mListView = v.findViewById(R.id.lvFiles);
        mListView.setOnItemClickListener(this);
        registerForContextMenu(mListView);

        Context c = getContext();
        if (c == null) {
            FabLogger.error("FileSelectorFragment: onCreateView: context is null!");
            mWorkingDir = null;
        } else {
            if (savedInstanceState == null) {
                try {
                    mWorkingDir = new File(GLKConstants.getDir(c, null));
                } catch (@NonNull IOException | SecurityException e) {
                    FabLogger.error("FileSelectorFragment: cannot access app directory. Check you have enabled file permissions. Also check that, if you have overridden the default paths in your settings, Fabularium has read/write access to that path.");
                    mWorkingDir = null;
                }
            } else {
                String path = savedInstanceState.getString(WORKING_DIR, "");
                if (DEBUG_LIFECYCLE) {
                    FabLogger.error("restored working directory: " + path);
                }
                try {
                    mWorkingDir = (path.equals("")) ? new File(GLKConstants.getDir(c, null)) : new File(path);
                } catch (@NonNull IOException | SecurityException e) {
                    FabLogger.error("FileSelectorFragment: cannot access app directory. Check you have enabled file permissions. Also check that, if you have overridden the default paths in your settings, Fabularium has read/write access to that path.");
                    mWorkingDir = null;
                }
            }
        }
        return v;
    }

    /**
     * Called when the fragment's activity has been created and this fragment's view
     * hierarchy instantiated. It can be used to do final initialization once these
     * pieces are in place, such as retrieving views or restoring state. It is
     * also useful for fragments that use setRetainInstance(boolean) to retain
     * their instance, as this callback tells the fragment when it is fully
     * associated with the new activity instance. This is called after
     * onCreateView(LayoutInflater, ViewGroup, Bundle) and before onViewStateRestored(Bundle).
     * <p>
     * If you override this method you must call through to the superclass implementation.
     *
     * @param savedInstanceState - If the fragment is being re-created from a previous
     *                           saved state, this is the state. This value may be null.
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        if (DEBUG_LIFECYCLE) {
            FabLogger.debug("FileSelector: onActivityCreated()");
        }
        super.onActivityCreated(savedInstanceState);
    }

    /**
     * Called when the fragment is visible to the user and actively running. This is
     * generally tied to Activity.onResume of the containing Activity's lifecycle.
     */
    @Override
    public void onResume() {
        // The FileSelector fragment uses some views that don't actually belong to this fragment's root -
        // in particular, the toolbar and various associated buttons / checkboxes on it. So we need to
        // access those views from the activity itself, and to be safe we only do that after the activity
        // has been fully created
        if (DEBUG_LIFECYCLE) {
            FabLogger.debug("FileSelector: onResume()");
        }
        FragmentActivity a = getActivity();
        if (a != null && !a.isFinishing() && a instanceof MainActivity) {
            final MainActivity act = (MainActivity) a;

            // Re-wire the activity's toolbar buttons to this fragment
            act.setToolbarCheckBoxListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    selectAll(isChecked);
                }
            });

            act.setToolbarOKListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mCurrentDisplay != DISPLAY_TYPE.NORMAL) {
                        Toast.makeText(getContext(),
                                "You can only copy or move files to other areas of the file system, not to shortcuts!",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (mSrcFiles != null && mSrcFiles.size() > 0) {
                        // TODO: should we prompt user if / when a file is about to be overwritten?
                        File dest;
                        MainActivity.TOOLBAR_MODE toolbarMode = act.getToolbarMode();
                        switch (toolbarMode) {
                            case COPY_FILES: {
                                ArrayList<File> files = new ArrayList<>();
                                for (File src : mSrcFiles) {
                                    dest = new File(mWorkingDir, src.getName());
                                    files.add(src);
                                    files.add(dest);
                                }

                                Fragment f = act.getTab(POS_TAB_EXPLORE);
                                if (f instanceof FileSelectorFragment && files.size() > 0) {
                                    File[] arr = new File[files.size()];
                                    arr = files.toArray(arr);
                                    AsyncProgressFragment frag = new AsyncProgressFragment();
                                    act.getSupportFragmentManager().beginTransaction().add(frag, "frag_copyfiles").commit();
                                    frag.copyFiles(act.getResources(), NORMAL_COPY_TAG, (FileSelectorFragment) f, arr);
                                }
                                break;
                            }

                            case MOVE_FILES: {
                                ArrayList<File> files = new ArrayList<>();
                                for (File src : mSrcFiles) {
                                    dest = new File(mWorkingDir, src.getName());
                                    files.add(src);
                                    files.add(dest);
                                }

                                Fragment f = act.getTab(POS_TAB_EXPLORE);
                                if (f instanceof FileSelectorFragment && files.size() > 0) {
                                    File[] arr = new File[files.size()];
                                    arr = files.toArray(arr);
                                    AsyncProgressFragment frag = new AsyncProgressFragment();
                                    act.getSupportFragmentManager().beginTransaction().add(frag, "frag_movefiles").commit();
                                    frag.renameFiles(act.getResources(), (FileSelectorFragment) f, arr);
                                }
                                break;
                            }
                        }
                    }
                    mSrcFiles = null;
                    act.setToolbarSelected(0, 0);
                    act.setToolbarMode(MainActivity.TOOLBAR_MODE.SELECT_FILES);
                    if (mWorkingDir != null) {
                        loadFileList(mWorkingDir);
                    }
                }
            });

            act.setToolbarCancelListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    act.setToolbarSelected(0, 0);
                    act.setToolbarMode(MainActivity.TOOLBAR_MODE.SELECT_FILES);
                    if (mWorkingDir != null) {
                        loadFileList(mWorkingDir);
                    }
                }
            });
        } else {
            // According to Android docs, onCreate is always called after onAttach, so a getActivity() call here shouldn't return NULL
            FabLogger.error("ANDROID ERROR: FileSelector: getActivity() returned NULL in onActivityCreated! This should not happen! Expect failure.");
        }
        super.onResume();
    }

    /**
     * Called to ask the fragment to save its current dynamic state, so it can later be
     * reconstructed in a new instance of its process is restarted. If a new instance of the
     * fragment later needs to be created, the data you place in the Bundle here will be
     * available in the Bundle given to onCreate(Bundle), onCreateView(LayoutInflater, ViewGroup,
     * Bundle), and onActivityCreated(Bundle).
     * <p>
     * This corresponds to Activity.onSaveInstanceState(Bundle) and most of the discussion there
     * applies here as well. Note however: this method may be called at any time before onDestroy(). There
     * are many situations where a fragment may be mostly torn down (such as when placed on the back
     * stack with no UI showing), but its state will not be saved until its owning activity actually
     * needs to save its state.
     *
     * @param outState -  Bundle in which to place your saved state.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // TODO: We currently only save the current working directory
        // We *don't* save any information about selected files
        // or the mode. Reason for this is that app will crash if
        // that data exceeds 1 Mb (i.e. lots of files selected) unless
        // we handle it specially, which is more work than we are prepared
        // to get ourselves into at the moment
        if (DEBUG_LIFECYCLE) {
            FabLogger.debug("FileSelector: onSaveInstanceStart()");
        }
        if (mWorkingDir != null) {
            outState.putString(WORKING_DIR, mWorkingDir.getAbsolutePath());
        }
        super.onSaveInstanceState(outState);
    }

    /**
     * Called when the Fragment is visible to the user. This is generally tied to
     * Activity.onStart of the containing Activity's lifecycle.
     * <p>
     * If you override this method you must call through to the superclass implementation.
     */
    @Override
    public void onStart() {
        if (DEBUG_LIFECYCLE) {
            FabLogger.debug("FileSelector: onStart()");
        }
        super.onStart();
        if (mWorkingDir != null) {
            loadFileList(mWorkingDir);
        } else {
            loadMountPoints();
        }
    }

    @Override
    public void onStop() {
        if (DEBUG_LIFECYCLE) {
            FabLogger.debug("FileSelector: onStop()");
        }
        super.onStop();
        FragmentActivity a = getActivity();
        if (a instanceof MainActivity) {
            // Remove toolbar listeners
            final MainActivity act = (MainActivity) a;
            act.setToolbarCheckBoxListener(null);
            act.setToolbarOKListener(null);
            act.setToolbarCancelListener(null);
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu. You should place
     * your menu items in to menu. For this method to be called, you must have first
     * called setHasOptionsMenu(boolean). See Activity.onCreateOptionsMenu for more information.
     *
     * @param menu     - The options menu in which you place your items.
     * @param inflater - The inflater.
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (DEBUG_LIFECYCLE) {
            FabLogger.debug("FileSelector: onCreateOptionsMenu()");
        }

        FragmentActivity a = getActivity();
        if (a instanceof MainActivity) {
            int numSelected = ((MainActivity) a).getToolbarSelected();
            inflater.inflate(R.menu.filesel, menu);
            if (numSelected > 0) {
                menu.findItem(R.id.action_newdir).setVisible(false);
                menu.findItem(R.id.action_newfile).setVisible(false);
                menu.findItem(R.id.action_goToParent).setVisible(false);
                menu.findItem(R.id.action_showShortcuts).setVisible(false);
                menu.findItem(R.id.action_showStorage).setVisible(false);
                menu.findItem(R.id.action_goToDir).setVisible(false);

                if (numSelected == 1) {
                    // single file/directory actions:
                    menu.findItem(R.id.action_rename).setVisible(mCurrentDisplay != DISPLAY_TYPE.ZIP);
                    menu.findItem(R.id.action_details).setVisible(true);

                    // single file actions:
                    ArrayList<File> selected = mAdapter.getSelected();
                    if (selected.size() > 0) {
                        boolean isFile = selected.get(0).isFile();
                        menu.findItem(R.id.action_agt2agx).setVisible(isFile && mCurrentDisplay != DISPLAY_TYPE.ZIP);
                    }
                } else {
                    menu.findItem(R.id.action_agt2agx).setVisible(false);
                    menu.findItem(R.id.action_rename).setVisible(false);
                    menu.findItem(R.id.action_details).setVisible(false);
                }

                // multiple file actions:
                menu.findItem(R.id.action_delete).setVisible(mCurrentDisplay != DISPLAY_TYPE.ZIP);
                menu.findItem(R.id.action_copy).setVisible(mCurrentDisplay != DISPLAY_TYPE.ZIP);
                menu.findItem(R.id.action_move).setVisible(mCurrentDisplay != DISPLAY_TYPE.ZIP);
                menu.findItem(R.id.action_addAuto).setVisible(true);
                menu.findItem(R.id.action_addManual).setVisible(true);
                menu.findItem(R.id.action_uncompress).setVisible(mCurrentDisplay != DISPLAY_TYPE.ZIP);
            } else {
                menu.findItem(R.id.action_rename).setVisible(false);
                menu.findItem(R.id.action_details).setVisible(false);
                menu.findItem(R.id.action_delete).setVisible(false);
                menu.findItem(R.id.action_uncompress).setVisible(false);
                menu.findItem(R.id.action_agt2agx).setVisible(false);
                menu.findItem(R.id.action_delete).setVisible(false);
                menu.findItem(R.id.action_copy).setVisible(false);
                menu.findItem(R.id.action_move).setVisible(false);
                menu.findItem(R.id.action_addAuto).setVisible(false);
                menu.findItem(R.id.action_addManual).setVisible(false);
                menu.findItem(R.id.action_uncompress).setVisible(false);

                // actions that only apply when no files are selected:
                menu.findItem(R.id.action_newdir).setVisible(mWorkingDir != null && mCurrentDisplay != DISPLAY_TYPE.ZIP);
                menu.findItem(R.id.action_newfile).setVisible(mWorkingDir != null && mCurrentDisplay != DISPLAY_TYPE.ZIP);
                menu.findItem(R.id.action_goToParent).setVisible(mWorkingDir != null);
                menu.findItem(R.id.action_showShortcuts).setVisible(mCurrentDisplay != DISPLAY_TYPE.SHORTCUTS);
                menu.findItem(R.id.action_showStorage).setVisible(mCurrentDisplay != DISPLAY_TYPE.STORAGE);
                menu.findItem(R.id.action_goToDir).setVisible(true);
            }
            menu.findItem(R.id.action_reloadMountPoints).setVisible(mCurrentDisplay == DISPLAY_TYPE.STORAGE);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * This hook is called whenever an item in your options menu is selected. The default
     * implementation simply returns false to have the normal processing happen (calling the
     * item's Runnable or sending a message to its Handler as appropriate). You can use this
     * method for any items for which you would like to do processing without those other facilities.
     * <p>
     * Derived classes should call through to the base class for it to perform the default menu handling.
     *
     * @param item - The menu item that was selected.
     * @return Return false to allow normal menu processing to proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final ArrayList<File> selected = mAdapter.getSelected();
        final int id = item.getItemId();
        final FragmentActivity act = getActivity();

        if (act == null || act.isFinishing()) {
            FabLogger.error("FileSelectorFragment: cannot retrieve parent activity or activity is finishing, options item command aborted.");
            return true;
        }

        switch (id) {
            case R.id.action_delete: {
                AlertDialog.Builder builder = new AlertDialog.Builder(act);
                builder.setTitle(R.string.dialog_delete_file_title);
                builder.setMessage("About to delete " + selected.size() + " item(s).");
                builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (selected.size() > 0 && !act.isFinishing() && act instanceof MainActivity) {
                            Fragment f = ((MainActivity) act).getTab(POS_TAB_EXPLORE);
                            if (f instanceof FileSelectorFragment) {
                                File[] arr = new File[selected.size()];
                                arr = selected.toArray(arr);
                                AsyncProgressFragment frag = new AsyncProgressFragment();
                                act.getSupportFragmentManager().beginTransaction().add(frag, "frag_deletefiles").commit();
                                frag.deleteFiles(act.getResources(), (FileSelectorFragment) f, arr);
                            }
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
                return true;
            }

            case R.id.action_goToParent: {
                onBackPressed();
                return true;
            }

            case R.id.action_showStorage: {
                loadMountPoints();
                return true;
            }

            case R.id.action_showShortcuts: {
                loadShortcuts();
                return true;
            }

            case R.id.action_goToDir: {
                AlertDialog.Builder builder = new AlertDialog.Builder(act);
                final EditText input = new EditText(builder.getContext());
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setTitle(R.string.dialog_goToFolder_title);
                builder.setView(input);
                builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String folderName = input.getText().toString();
                        if (!folderName.equals("")) {
                            loadFileList(new File(folderName));
                        }
                    }
                }).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                });
                builder.show();
                return true;
            }

            case R.id.action_reloadMountPoints: {
                mMountPoints = getMountPoints();
                Toast.makeText(act, "Refreshed mount points.", Toast.LENGTH_SHORT).show();
                return true;
            }

            case R.id.action_details: {
                // TODO: make this interactive so user can set/unset read-only, etc.
                if (selected.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    SimpleDateFormat formatter = new SimpleDateFormat("d MMM yyyy HH:mm:ss", Locale.US);
                    File f = selected.get(0);
                    sb.append("Name:       ").append(f.getName()).append("\n");
                    if (f.isFile()) {
                        sb.append("Size:       ").append((f.length() / 1024)).append("KB\n");
                    }
                    sb.append("Modified:   ").append(formatter.format(f.lastModified())).append("\n");
                    sb.append("Readable:   ").append(f.canRead()).append("\n");
                    sb.append("Writable:   ").append(f.canWrite()).append("\n");
                    sb.append("Executable: ").append(f.canExecute()).append("\n");

                    AlertDialog.Builder builder = new AlertDialog.Builder(act);
                    builder.setTitle(R.string.dialog_file_properties_title);
                    builder.setMessage(sb);
                    builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // do nothing
                        }
                    });
                    builder.show();
                }
                return true;
            }

            case R.id.action_newdir: {
                AlertDialog.Builder builder = new AlertDialog.Builder(act);
                final EditText input = new EditText(builder.getContext());
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setTitle(R.string.dialog_create_new_folder_title);
                builder.setView(input);
                builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString();
                        if (!name.equals("") && !act.isFinishing()) {
                            File newdir = new File(mWorkingDir, name);
                            if (!newdir.mkdir()) {
                                Toast.makeText(act, "Could not create new directory here.", Toast.LENGTH_LONG).show();
                                return;
                            }
                            if (mWorkingDir != null) {
                                loadFileList(mWorkingDir);
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
                return true;
            }

            case R.id.action_newfile: {
                AlertDialog.Builder builder = new AlertDialog.Builder(act);
                final EditText input = new EditText(builder.getContext());
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setTitle(R.string.dialog_create_new_file_title);
                builder.setView(input);
                builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString();
                        if (!name.equals("") && !act.isFinishing()) {
                            File newdir = new File(mWorkingDir, name);
                            try {
                                if (!newdir.createNewFile()) {
                                    Toast.makeText(act, "Could not create new file here.", Toast.LENGTH_LONG).show();
                                    return;
                                }
                            } catch (IOException e) {
                                Toast.makeText(act, "Could not create new file here.", Toast.LENGTH_LONG).show();
                                return;
                            }
                            if (mWorkingDir != null) {
                                loadFileList(mWorkingDir);
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
                return true;
            }

            case R.id.action_rename: {
                if (selected.size() > 0) {
                    final File f = selected.get(0);
                    AlertDialog.Builder builder = new AlertDialog.Builder(act);
                    final EditText input = new EditText(builder.getContext());
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    input.setText(f.getName());
                    builder.setTitle(R.string.dialog_rename_file_title);
                    builder.setView(input);
                    builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String name = input.getText().toString();
                            if (!name.equals("") && !act.isFinishing() && act instanceof MainActivity) {
                                ArrayList<File> files = new ArrayList<>();
                                files.add(f);
                                files.add(new File(mWorkingDir, name));

                                Fragment f = ((MainActivity) act).getTab(POS_TAB_EXPLORE);
                                if (f instanceof FileSelectorFragment && files.size() > 0) {
                                    File[] arr = new File[files.size()];
                                    arr = files.toArray(arr);
                                    AsyncProgressFragment frag = new AsyncProgressFragment();
                                    act.getSupportFragmentManager().beginTransaction().add(frag, "frag_movefiles").commit();
                                    frag.renameFiles(act.getResources(), (FileSelectorFragment) f, arr);
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
                }
                return true;
            }

            case R.id.action_move:
            case R.id.action_copy: {
                mSrcFiles = new ArrayList<>(selected);
                mAdapter.toggleCheckBoxes(false);
                if (act instanceof MainActivity) {
                    ((MainActivity) act).setToolbarMode(id == R.id.action_move ?
                            MainActivity.TOOLBAR_MODE.MOVE_FILES : MainActivity.TOOLBAR_MODE.COPY_FILES);
                }
                return true;
            }

            case R.id.action_addAuto: {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
                boolean copyFile = sharedPref.getBoolean(PreferencesActivity.KEY_PREF_COPY_GAME_FILE_ON_INSTALL, true);
                ArrayList<File> files = new ArrayList<>();
                File dest;
                for (File src : selected) {
                    if (copyFile || (src instanceof ZipShortcut)) {
                        if (src instanceof ZipShortcut) {
                            try {
                                ZipShortcut src2 = (ZipShortcut) src;
                                src = ((ZipShortcut) src).extractToTempFile(act, false);
                                dest = new File(GLKConstants.getDir(act, GLKConstants.SUBDIR.GAME), src2.getName());
                            } catch (IOException e) {
                                Toast.makeText(act,
                                        "Can't extract temporary file '" +
                                                src.getAbsolutePath() + "': " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                                return true;
                            }
                        } else {
                            try {
                                dest = new File(GLKConstants.getDir(act, GLKConstants.SUBDIR.GAME), src.getName());
                            } catch (IOException e) {
                                Toast.makeText(act,
                                        "Can't access game directory. Check you have enabled file read / write permissions. Also check that, if you have overridden the default paths in your settings, Fabularium has read/write access to your specified path.",
                                        Toast.LENGTH_LONG).show();
                                return true;
                            }
                        }
                        files.add(src);
                        files.add(dest);
                    } else {
                        IFictionDbHelper.addGameMetaData(act, src, mDbHelper, ByteBuffer.allocateDirect(2000000), this);
                    }
                }

                if (copyFile && files.size() > 0) {
                    File[] arr = new File[files.size()];
                    arr = files.toArray(arr);
                    AsyncProgressFragment frag = new AsyncProgressFragment();
                    act.getSupportFragmentManager().beginTransaction().add(frag, "frag_copyfiles").commit();
                    frag.copyFiles(act.getResources(), AUTO_COPY_TAG, this, arr);
                }
                return true;
            }

            case R.id.action_addManual: {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
                boolean copyFile = sharedPref.getBoolean(PreferencesActivity.KEY_PREF_COPY_GAME_FILE_ON_INSTALL, true);
                ArrayList<File> files = new ArrayList<>();
                File dest;
                for (File src : selected) {
                    if (copyFile || (src instanceof ZipShortcut)) {
                        if (src instanceof ZipShortcut) {
                            try {
                                ZipShortcut src2 = (ZipShortcut) src;
                                src = ((ZipShortcut) src).extractToTempFile(act, false);
                                dest = new File(GLKConstants.getDir(act, GLKConstants.SUBDIR.GAME), src2.getName());
                            } catch (IOException e) {
                                Toast.makeText(act,
                                        "Can't extract temporary file '" +
                                                src.getAbsolutePath() + "': " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                                return true;
                            }
                        } else {
                            try {
                                dest = new File(GLKConstants.getDir(act, GLKConstants.SUBDIR.GAME), src.getName());
                            } catch (IOException e) {
                                Toast.makeText(act,
                                        "Can't access game directory. Check you have enabled file read / write permissions. Also check that, if you have overridden the default paths in your settings, Fabularium has read/write access to your specified path.",
                                        Toast.LENGTH_LONG).show();
                                return true;
                            }
                        }
                        files.add(src);
                        files.add(dest);
                    } else {
                        IFictionDbHelper.addGameMetaDataManually(src, this);
                    }
                }

                if (copyFile && files.size() > 0) {
                    File[] arr = new File[files.size()];
                    arr = files.toArray(arr);
                    AsyncProgressFragment frag = new AsyncProgressFragment();
                    act.getSupportFragmentManager().beginTransaction().add(frag, "frag_copyfiles").commit();
                    frag.copyFiles(act.getResources(), MANUAL_COPY_TAG, this, arr);
                }
                return true;
            }

            case R.id.action_uncompress: {
                ArrayList<File> files = new ArrayList<>();
                if (selected.size() > 0) {
                    // Extract to the same directory as the first
                    // source file.
                    File src1 = selected.get(0);
                    if (src1.isFile()) {
                        File dest = src1.getParentFile();
                        for (File src : selected) {
                            files.add(src);
                            files.add(dest);
                        }
                        File[] arr = new File[files.size()];
                        arr = files.toArray(arr);
                        AsyncProgressFragment frag = new AsyncProgressFragment();
                        act.getSupportFragmentManager().beginTransaction().add(frag, "frag_unzipfiles").commit();
                        frag.unzipFiles(act.getResources(), this, arr);
                    }
                }
                return true;
            }

            case R.id.action_agt2agx: {
                for (File f : selected) {
                    performActionOnFile(id, f);
                }
                return true;
            }

            default: {
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
            }
        }
    }

    /**
     * Callback method to be invoked when an item in this AdapterView has been clicked.
     * <p>
     * Implementers can call getItemAtPosition(position) if they need to access the data
     * associated with the selected item.
     *
     * @param parent   - The AdapterView where the click happened.
     * @param view     - The view within the AdapterView that was clicked (this will be a view provided by the adapter)
     * @param position - The position of the view in the adapter.
     * @param id       - The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(@NonNull AdapterView<?> parent, @NonNull View view, int position, long id) {
        File f = (File) parent.getItemAtPosition(position);
        String name = f.getName();
        ZipFile zf = null;
        if (f.isFile() && name.toLowerCase().endsWith(".zip")) {
            try {
                zf = new ZipFile(f);
            } catch (IOException e) {
                // can't open as a ZIP; treat it like a normal file
            }
        }
        if (name.equals("..") || f.isDirectory() || zf != null) {
            if (mWorkingDir != null) {
                // a directory - either go up or down
                if (zf != null) {
                    // list the contents of the ZIP file
                    loadZipFileList(mWorkingDir, zf);
                } else {
                    if (mWorkingDir instanceof ZipShortcut) {
                        loadFileList(new ZipShortcut((ZipShortcut) mWorkingDir, name + "/"));
                    } else {
                        loadFileList(name.equals("..") ? mWorkingDir.getParentFile() : new File(mWorkingDir, name));
                    }
                }
            } else {
                // a mount point
                loadFileList(f);
            }
        } else {
            // a file - open it in the text editor if relevant
            final FragmentActivity act = getActivity();
            if (act == null || act.isFinishing()) {
                FabLogger.error("FileSelectorFragment: onItemClick: cannot retrieve activity or activity is finishing - command aborted.");
                return;
            }

            // We only open files as text when they either have no extension or match one of the extensions specified
            // in the user preferences.
            String[] file_parts = name.split("\\.");
            if (file_parts.length == 2) {
                String file_ext = file_parts[1].toLowerCase();
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
                String s = sharedPref.getString(PreferencesActivity.KEY_PREF_TEXT_FILE_EXTS, "");
                if (s.equals("")) {
                    FabLogger.warn("FileSelectorFragment: your settings currently don't allow any files to open in the text editor.");
                    return;
                }
                boolean found = false;
                String[] text_exts = s.split(";");
                for (String text_ext : text_exts) {
                    if (file_ext.equals(text_ext.toLowerCase())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // TODO: see if we can open the file as a game
                    Toast.makeText(getContext(),
                            getString(R.string.explore_msg_is_this_a_game),
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }

            if (f instanceof ZipShortcut) {
                try {
                    f = ((ZipShortcut) f).extractToTempFile(act, true);
                } catch (IOException e) {
                    Toast.makeText(act,
                            "Can't extract temporary file '" +
                                    f.getAbsolutePath() + "': " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }

            // check if it is excessively large (> 500 kB) and ask the
            // user if they really to proceed on such a basis (and understand
            // risk of ANRs, etc)
            if (f.length() > 500000) {
                // Too big!
                final String path = f.getAbsolutePath();
                AlertDialog.Builder builder = new AlertDialog.Builder(act);
                builder.setTitle(R.string.dialog_loading_large_file_title);
                builder.setMessage(R.string.dialog_msg_loading_large_file);
                builder.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Don't say we didn't warn you!
                        if (!act.isFinishing()) {
                            Intent intent = new Intent(act, TextEditorActivity.class);
                            intent.putExtra(TextEditorActivity.TEXTFILE, path);
                            startActivity(intent);
                        }
                    }
                }).setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Wise move!
                    }
                });
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.show();
            } else {
                // Ok, this is manageable, cool bananas
                Intent intent = new Intent(act, TextEditorActivity.class);
                intent.putExtra(TextEditorActivity.TEXTFILE, f.getAbsolutePath());
                startActivity(intent);
            }
        }
    }

    /**
     * Process a back key press. If we are currently already at the
     * top of the file stack (i.e. in the mount points view), this event
     * isn't consumed and nothing happens. Otherwise, the event is consumed
     * and the file selector either moves to the parent directory of the current
     * working directory (if that exists and can be displayed) or displays the
     * mount points otherwise.
     *
     * @return TRUE if the event was consumed, FALSE otherwise.
     */
    public boolean onBackPressed() {
        if (mWorkingDir == null) {
            // already at the topmost level (mount points), so
            // we do not consume this event
            return false;
        }

        // If the parent doesn't exist, show the mount points
        File parent = mWorkingDir.getParentFile();
        if (parent == null) {
            loadMountPoints();
            return true;
        }

        // If we can't display the contents of the parent, show the mount points
        File[] files = parent.listFiles();
        if (files == null) {
            loadMountPoints();
            return true;
        }

        // Ok, we can actually show the parent
        loadFileList(parent);
        return true;
    }

    /**
     * If an adapter has been loaded, you can call this method to
     * select or unselect all items.
     *
     * @param on - if TRUE, select all, if FALSE, select none.
     */
    public void selectAll(boolean on) {
        if (mAdapter != null) {
            mAdapter.setAll(on);
        }
    }

    private void loadShortcuts() {
        if (DEBUG_LIFECYCLE) {
            FabLogger.debug("loadHomeDirs");
        }
        FragmentActivity a = getActivity();
        if (a != null && !a.isFinishing() && a instanceof MainActivity) {
            MainActivity act = (MainActivity) a;

            // Load in the new file list
            mWorkingDir = null;
            List<File> list = new ArrayList<>();
            Shortcut s;
            try {
                s = new Shortcut(GLKConstants.getDir(a, null));
                s.setDisplayText("Root");
                list.add(s);
            } catch (IOException e) {
                Toast.makeText(a,
                        "Cannot access Fabularium's root folder. Did you override it in the settings? Please check it exists and that Fabularium has read/write access to it.",
                        Toast.LENGTH_LONG).show();
            }
            try {
                s = new Shortcut(GLKConstants.getDir(a, GLKConstants.SUBDIR.GAME));
                s.setDisplayText("Games");
                list.add(s);
            } catch (IOException e) {
                Toast.makeText(a,
                        "Cannot access Fabularium's game folder. Did you override it in the settings? Please check it exists and that Fabularium has read/write access to it.",
                        Toast.LENGTH_LONG).show();
            }
            try {
                s = new Shortcut(GLKConstants.getDir(a, GLKConstants.SUBDIR.GAMEDATA));
                s.setDisplayText("Game Data");
                list.add(s);
            } catch (IOException e) {
                Toast.makeText(a,
                        "Cannot access Fabularium's game data folder. Did you override it in the settings? Please check it exists and that Fabularium has read/write access to it.",
                        Toast.LENGTH_LONG).show();
            }
            try {
                s = new Shortcut(GLKConstants.getDir(a, GLKConstants.SUBDIR.FONTS));
                s.setDisplayText("Fonts");
                list.add(s);
            } catch (IOException e) {
                Toast.makeText(a,
                        "Cannot access Fabularium's fonts folder. Did you override it in the settings? Please check it exists and that Fabularium has read/write access to it.",
                        Toast.LENGTH_LONG).show();
            }
            try {
                s = new Shortcut(GLKConstants.getDir(a, GLKConstants.SUBDIR.PROJECTS));
                s.setDisplayText("Projects");
                list.add(s);
            } catch (IOException e) {
                Toast.makeText(a,
                        "Cannot access Fabularium's projects folder. Did you override it in the settings? Please check it exists and that Fabularium has read/write access to it.",
                        Toast.LENGTH_LONG).show();
            }
            try {
                s = new Shortcut(GLKConstants.getDir(a, GLKConstants.SUBDIR.LIB));
                s.setDisplayText("Compiler libraries");
                list.add(s);
            } catch (IOException e) {
                Toast.makeText(a,
                        "Cannot access Fabularium's library folder. Did you override it in the settings? Please check it exists and that Fabularium has read/write access to it.",
                        Toast.LENGTH_LONG).show();
            }
            try {
                s = new Shortcut(GLKConstants.getDir(a, GLKConstants.SUBDIR.INCLUDE));
                s.setDisplayText("Compiler headers");
                list.add(s);
            } catch (IOException e) {
                Toast.makeText(a,
                        "Cannot access Fabularium's include folder. Did you override it in the settings? Please check it exists and that Fabularium has read/write access to it.",
                        Toast.LENGTH_LONG).show();
            }
            mAdapter = new FileListAdapter(act, list, act);
            mListView.setAdapter(mAdapter);
            mPathView.setText(R.string.shortcuts);
            mPathView.setContentDescription("Showing shortcuts");
            mAdapter.toggleCheckBoxes(false);

            mCurrentDisplay = DISPLAY_TYPE.SHORTCUTS;

            // Clear any selections and ensure the toolbar
            // only display the up-directory button if there
            // is a parent directory to go to
            act.setToolbarSelected(0, 0);
            act.invalidateOptionsMenu();
        }
    }

    private void loadMountPoints() {
        if (DEBUG_LIFECYCLE) {
            FabLogger.debug("loadMountPoints");
        }
        FragmentActivity a = getActivity();
        if (a != null && !a.isFinishing() && a instanceof MainActivity) {
            MainActivity act = (MainActivity) a;

            // Load in the new file list
            mWorkingDir = null;
            if (mMountPoints == null) {
                mMountPoints = getMountPoints();
            }
            List<File> list = mMountPoints;
            Collections.sort(list);
            mAdapter = new FileListAdapter(act, list, act);
            mListView.setAdapter(mAdapter);
            mPathView.setText(R.string.mount_points);
            mPathView.setContentDescription("Showing storage locations");
            mAdapter.toggleCheckBoxes(false);

            mCurrentDisplay = DISPLAY_TYPE.STORAGE;

            // Clear any selections and ensure the toolbar
            // only display the up-directory button if there
            // is a parent directory to go to
            act.setToolbarSelected(0, 0);
            act.invalidateOptionsMenu();
        }
    }

    private void loadZipFileList(@NonNull File workingDir, @NonNull ZipFile zipFile) {
        if (DEBUG_LIFECYCLE) {
            FabLogger.debug("loadFileList: workingDir = " + workingDir.getAbsolutePath());
        }

        FragmentActivity a = getActivity();
        if (a != null && !a.isFinishing() && a instanceof MainActivity) {
            MainActivity act = (MainActivity) a;

            // Load in the new file list
            ArrayList<File> list = new ArrayList<>();
            ArrayList<String> names = new ArrayList<>();

            String readFailed = null;
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            final HashMap<String, ZipEntry> zipEntries = new HashMap<>();
            final String zipPath = zipFile.getName();
            while (entries.hasMoreElements()) {
                try {
                    final ZipEntry ze = entries.nextElement();
                    zipEntries.put(ze.getName(), ze);
                } catch (IllegalArgumentException e) {
                    readFailed = e.getMessage();
                }
            }

            if (readFailed != null) {
                Toast.makeText(getContext(),
                        "Couldn't read all the entries of zip file '" +
                                zipPath + "" + "': " + readFailed,
                        Toast.LENGTH_LONG).show();
            }

            try {
                zipFile.close();
            } catch (IOException e) {
                Toast.makeText(getContext(),
                        "Couldn't close the zip file '" +
                                zipPath + "" + "': " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }

            for (ZipEntry ze : zipEntries.values()) {
                String zePath = ze.getName().replaceAll("/.*", "/");
                if (!names.contains(zePath) && zePath.length() > 0) {
                    names.add(zePath);
                    list.add(new ZipShortcut(null, zePath, zipPath, zipEntries));
                }
            }

            Collections.sort(list);

            mWorkingDir = new ZipShortcut(null, "/", zipPath, zipEntries);
            mAdapter = new FileListAdapter(act, list, act);
            mListView.setAdapter(mAdapter);
            String p = mWorkingDir.getAbsolutePath();
            mPathView.setText(p);
            mPathView.setContentDescription("Showing contents of zip file " + p);

            // Only allow user to select items if we are in SELECT_FILES mode
            MainActivity.TOOLBAR_MODE toolbarMode = act.getToolbarMode();
            if (toolbarMode != MainActivity.TOOLBAR_MODE.SELECT_FILES) {
                mAdapter.toggleCheckBoxes(false);
            }

            mCurrentDisplay = DISPLAY_TYPE.ZIP;

            // Clear any selections and ensure the toolbar
            // only display the up-directory button if there
            // is a parent directory to go to
            act.setToolbarSelected(0, 0);
            act.invalidateOptionsMenu();
        }
    }

    private void loadFileList(@NonNull File workingDir) {
        if (DEBUG_LIFECYCLE) {
            FabLogger.debug("loadFileList: workingDir = " + workingDir.getAbsolutePath());
        }
        FragmentActivity a = getActivity();
        if (a != null && !a.isFinishing() && a instanceof MainActivity) {
            MainActivity act = (MainActivity) a;

            // Load in the new file list
            // From Android developer docs:
            //    listFiles():
            //       "If this abstract pathname does not denote a directory, then
            //        this method returns null. Otherwise an array of File objects
            //        is returned, one for each file or directory in the directory.
            //        Pathnames denoting the directory itself and the directory's
            //        parent directory are not included in the result. Each
            //        resulting abstract pathname is constructed from this abstract
            //        pathname using the File(File, String) constructor. Therefore if
            //        this pathname is absolute then each resulting pathname is
            //        absolute; if this pathname is relative then each resulting
            //        pathname will be relative to the same directory.
            //
            //        There is no guarantee that the name strings in the
            //        resulting array will appear in any specific order;
            //        they are not, in particular, guaranteed to appear in
            //        alphabetical order."
            List<File> list;
            File[] files;
            try {
                files = workingDir.listFiles();
            } catch (SecurityException e) {
                // If a security manager exists and its checkRead(String) method denies read access to the directory
                Toast.makeText(getContext(), "Cannot list contents of '" + workingDir.getAbsolutePath() + "" +
                                "' as according to Android, your security manager has denied read access to this directory.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (files == null) {
                // Abstract pathname does not denote a directory, or an I/O error occurred.
                Toast.makeText(getContext(), "Cannot list contents of '" + workingDir.getAbsolutePath() + "" +
                                "' as according to Android either this path does not denote a directory, or an I/O error occurred.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            list = Arrays.asList(files);
            Collections.sort(list);

            mWorkingDir = workingDir;
            mAdapter = new FileListAdapter(act, list, act);
            mListView.setAdapter(mAdapter);
            String p = mWorkingDir.getAbsolutePath();
            mPathView.setText(p);
            mPathView.setContentDescription("Showing contents of folder " + p);

            // Only allow user to select items if we are in SELECT_FILES mode
            MainActivity.TOOLBAR_MODE toolbarMode = act.getToolbarMode();
            if (toolbarMode != MainActivity.TOOLBAR_MODE.SELECT_FILES) {
                mAdapter.toggleCheckBoxes(false);
            }

            mCurrentDisplay = (workingDir instanceof ZipShortcut) ?
                    DISPLAY_TYPE.ZIP : DISPLAY_TYPE.NORMAL;

            // Clear any selections and ensure the toolbar
            // only display the up-directory button if there
            // is a parent directory to go to
            act.setToolbarSelected(0, 0);
            act.invalidateOptionsMenu();
        }
    }

    private void setupServiceReceiver() {
        mGameUtilReceiver = new RunProgramResultReceiver(new Handler());
        mGameUtilReceiver.setReceiver(new RunProgramResultReceiver.Receiver() {
            @Override
            public void onReceiveResult(int resultCode, @NonNull Bundle resultData) {
                if (resultCode == Activity.RESULT_OK) {
                    int resultVal = resultData.getInt(RunProgramService.CMD_RESULT);
                    String resMsg = resultData.getString(RunProgramService.CMD_RESULT_MSG);
                    FragmentActivity act = getActivity();

                    if (act != null && !act.isFinishing()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(act);
                        builder.setTitle((resultVal != 0 ?
                                R.string.dialog_prog_failure_title : R.string.dialog_prog_success_title))
                                .setMessage(resMsg)
                                .setPositiveButton(R.string.dialog_got_it, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // User closed the dialog
                                    }
                                })
                                .show();

                        // Reload the file list for the current directory
                        if (mWorkingDir != null) {
                            loadFileList(mWorkingDir);
                        }
                    }
                }
            }
        });
    }

    private void performActionOnFile(int actionID, @NonNull File f) {
        // TODO: correctly handle cases where the file path has spaces in it
        FragmentActivity act = getActivity();
        if (act == null || act.isFinishing()) {
            FabLogger.error("FileSelector: error: cannot perform action on file as cannot retrieve parent activity or the activity is finishing.");
            return;
        }
        if (mGameUtilReceiver == null) {
            Toast.makeText(act, "Error: Can't perform action on file as service failed to initialise.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (!f.exists()) {
            Toast.makeText(act, "Error: File doesn't exist.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!f.isFile()) {
            Toast.makeText(act, "Error: That's not a file.", Toast.LENGTH_LONG).show();
            return;
        }

        // Ok, we have a valid file
        String args = null;
        switch (actionID) {
            case R.id.action_agt2agx: {
                // try to convert to AGX
                String[] split = f.getName().split("\\.");
                String path = f.getAbsolutePath();
                if (split.length > 1) {
                    // we only attempt to process if the file has an extension
                    String ext = split[1];
                    String path2 = path.substring(0, path.length() - ext.length() - 1);
                    args = "agt2agx " + path2;
                } else {
                    Toast.makeText(act,
                            "That doesn't seem to be an AGT file - no extension.",
                            Toast.LENGTH_LONG).show();
                }
                break;
            }

            default:
                FabLogger.warn("FileSelector: performActionOnFile: didn't recognise the dialogID.");
                return;
        }

        if (args != null) {
            // If we got here, we're ready to call the service
            Intent intent = new Intent(act, RunProgramService.class);
            intent.putExtra(RunProgramService.CMD_ARGS, args);
            intent.putExtra(RunProgramService.CMD_RECEIVER, mGameUtilReceiver);
            act.startService(intent);
        }
    }

    @Override
    public void onCopyFilesPostExecute(@NonNull String tag, @Nullable File[] results) {
        // Callback once async file copy has completed
        // Results includes a boolean for each file copy attempt indicating success or failure
        int nCopied = 0;

        FragmentActivity act = getActivity();
        if (act == null || act.isFinishing()) {
            FabLogger.error("FileSelectorFragment: copy done but cannot notify the user - cannot retrieve activity, or the activity is finishing.");
            return;
        }

        switch (tag) {
            case AUTO_COPY_TAG:
                if (results == null) {
                    Toast.makeText(getContext(), "Could not add any games. Check fab.log for more information.", Toast.LENGTH_LONG).show();
                    return;
                }

                for (File r : results) {
                    if (r != null) {
                        IFictionDbHelper.addGameMetaData(act, r, mDbHelper, ByteBuffer.allocateDirect(2000000), this);
                        nCopied++;
                    }
                }

                if (nCopied != results.length) {
                    String msg = "Could not add all games - added " + nCopied +
                            " of " + results.length + ". Check fab.log for more information.";
                    Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                }
                break;

            case MANUAL_COPY_TAG:
                if (results == null) {
                    Toast.makeText(getContext(), "Could not add any games. Check fab.log for more information.", Toast.LENGTH_LONG).show();
                    return;
                }

                for (File r : results) {
                    if (r != null) {
                        IFictionDbHelper.addGameMetaDataManually(r, this);
                        nCopied++;
                    }
                }

                if (nCopied != results.length) {
                    String msg = "Could not add all games - added " + nCopied +
                            " of " + results.length + ". Check fab.log for more information.";
                    Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                }
                break;

            case NORMAL_COPY_TAG:
                if (results == null) {
                    Toast.makeText(getContext(), "Could not copy any items. Check fab.log for more information.", Toast.LENGTH_LONG).show();
                    return;
                }

                for (File r : results) {
                    if (r != null) {
                        nCopied++;
                    }
                }

                String msg;
                if (nCopied != results.length) {
                    msg = "Could not copy all items - only copied " + nCopied +
                            " of " + results.length + ". Check fab.log for more information.";
                } else {
                    msg = "Copied " + nCopied + " item(s).";
                }
                Toast.makeText(act, msg, Toast.LENGTH_LONG).show();
                break;

            default:
                break;
        }

        // Refresh the game selector screen, if relevant
        if (tag.equals(NORMAL_COPY_TAG)) {
            if (mWorkingDir != null) {
                loadFileList(mWorkingDir);
            }
        } else if (tag.equals(AUTO_COPY_TAG) || tag.equals(MANUAL_COPY_TAG)) {
            if (act instanceof MainActivity) {
                Fragment f = ((MainActivity) act).getTab(MainActivity.POS_TAB_PLAY);
                if (f instanceof GameSelectorFragment) {
                    ((GameSelectorFragment) f).restartLoader();
                }
            }
        }
    }

    @Override
    public void onRenameFilesPostExecute(@Nullable File[] results) {
        int nRenamed = 0;

        FragmentActivity act = getActivity();
        if (act == null || act.isFinishing()) {
            FabLogger.error("FileSelectorFragment: cannot rename items - either results is null, or cannot retrieve activity, or the activity is finishing.");
            return;
        }
        if (results == null) {
            Toast.makeText(getContext(), "Could not rename any items. Check fab.log for more information.", Toast.LENGTH_LONG).show();
            return;
        }

        for (File r : results) {
            if (r != null) {
                nRenamed++;
            }
        }

        String msg;
        if (nRenamed != results.length) {
            msg = "Could not rename all items - only renamed " + nRenamed +
                    " of " + results.length + ". Check fab.log for more information.";
        } else {
            msg = "Renamed " + nRenamed + " item(s).";
        }
        Toast.makeText(act, msg, Toast.LENGTH_LONG).show();

        if (mWorkingDir != null) {
            loadFileList(mWorkingDir);
        }
    }

    @Override
    public void onUnzipFilesPostExecute(@Nullable File[] results) {
        int nUnzipped = 0;

        FragmentActivity act = getActivity();
        if (act == null || act.isFinishing()) {
            FabLogger.error("FileSelectorFragment: cannot unzip files - either results is null, or cannot retrieve activity, or the activity is finishing.");
            return;
        }
        if (results == null) {
            Toast.makeText(getContext(), "Could not unzip any files. Check fab.log for more information.", Toast.LENGTH_LONG).show();
            return;
        }

        for (File r : results) {
            if (r != null) {
                nUnzipped++;
            }
        }

        String msg;
        if (nUnzipped != results.length) {
            msg = "Could not unzip all files - only unzipped " + nUnzipped +
                    " of " + results.length + ". Check fab.log for more information.";
        } else {
            msg = "Unzipped " + nUnzipped + " file(s).";
        }
        Toast.makeText(act, msg, Toast.LENGTH_LONG).show();

        if (mWorkingDir != null) {
            loadFileList(mWorkingDir);
        }
    }

    @Override
    public void onDeleteFilesPostExecute(int nDeleted) {
        FragmentActivity act = getActivity();
        if (act == null || act.isFinishing()) {
            FabLogger.error("FileSelectorFragment: onDeleteFilesPostExecute - cannot retrieve activity or the activity is finishing.");
            return;
        }
        if (mWorkingDir != null) {
            loadFileList(mWorkingDir);
        }
        Toast.makeText(act, nDeleted + " item(s) deleted.", Toast.LENGTH_LONG).show();
    }

    private enum DISPLAY_TYPE {
        NORMAL, STORAGE, SHORTCUTS, ZIP
    }
}
