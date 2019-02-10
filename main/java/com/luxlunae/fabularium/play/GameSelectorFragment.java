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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.luxlunae.fabularium.AsyncLoaders;
import com.luxlunae.fabularium.AsyncProgressFragment;
import com.luxlunae.fabularium.FabLogger;
import com.luxlunae.fabularium.MainActivity;
import com.luxlunae.fabularium.PreferencesActivity;
import com.luxlunae.fabularium.R;
import com.luxlunae.fabularium.UrlDialogFragment;
import com.luxlunae.glk.GLKActivity;
import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKUtils;

import java.io.File;
import java.io.IOException;

/**
 * The main interface that appears under the PLAY tab and allows the user to browse and manage
 * their game library. Game library metadata is stored in the user's private Fabularium database
 * (see IFictionDbHelper).
 */
public class GameSelectorFragment extends Fragment
        implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
        LoaderManager.LoaderCallbacks<Cursor>, SimpleCursorAdapter.ViewBinder,
        AsyncProgressFragment.RefreshDbCallbacks, PopupMenu.OnMenuItemClickListener, AsyncProgressFragment.DownloadMetadataCallback {
    private final static String DB_LOADER_SORT_ORDER = "com.luxlunae.fabularium.GAME_SORT_ORDER";
    private final static String DB_LOADER_SORT_ASC = "com.luxlunae.fabularium.GAME_SORT_ASC";
    private final static String DB_REFRESH_TAG = "com.luxlunae.fabularium.GAME_REFRESH_DB_TAG";
    private final static String URL_DIALOG_TAG = "com.luxlunae.fabularium.GAME_URL_DIALOG_TAG";
    private final static String IFICTION_DIALOG_TAG = "com.luxlunae.fabularium.IFICTION_DIALOG_TAG";

    private final static int ICON_WIDTH = 48;      // in dp
    private final static int ICON_HEIGHT = 48;     // in dp

    /* Default server and arguments to download XML metadata, given IFID or TUID */
    /* If there is a cover image a link will be provided under tag <coverart> => <url> */
    private static final String METADATA_DEF_SERVER_IFID = "https://ifdb.tads.org/viewgame?ifiction&ifid=";

    private SimpleCursorAdapter mAdapter;
    @Nullable
    private IFictionDbHelper mDbHelper;
    @Nullable
    private Bitmap mDefaultIcon;
    private int mIconWidth;
    private int mIconHeight;
    private int mSortIndex = 4;
    private boolean mSortAscending = true;

    private GridView mGridView;
    private int mSelPosition;

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
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        FragmentActivity act = getActivity();
        if (act != null) {
            mDbHelper = new IFictionDbHelper(act);
        } else {
            // According to Android docs, onCreate is always called after onAttach, so a getActivity() call here shouldn't return NULL
            FabLogger.error("ANDROID ERROR: GameSelector: getActivity() returned NULL in onCreate! This should not happen! Expect failure.");
        }
        DisplayMetrics dm = getResources().getDisplayMetrics();
        mIconWidth = GLKUtils.dpToPx(ICON_WIDTH, dm);
        mIconHeight = GLKUtils.dpToPx(ICON_HEIGHT, dm);
        mDefaultIcon = GLKUtils.getBitmapFromResource(getResources(), R.mipmap.ic_launcher, mIconWidth, mIconHeight);
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
        FrameLayout fr = (FrameLayout) inflater.inflate(R.layout.game_selector, container, false);
        TextView empty = fr.findViewById(android.R.id.empty);
        empty.setText(R.string.empty_game_list);
        empty.setContentDescription(getString(R.string.empty_game_list));
        mGridView = fr.findViewById(R.id.gvGames);
        mGridView.setEmptyView(empty);
        return fr;
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
        super.onActivityCreated(savedInstanceState);
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
        super.onStart();
        loadGameList();
    }

    /**
     * Called when the Fragment is no longer started. This is generally tied to
     * Activity.onStop of the containing Activity's lifecycle.
     * <p>
     * If you override this method you must call through to the superclass implementation.
     */
    @Override
    public void onStop() {
        super.onStop();
        getLoaderManager().destroyLoader(0);
    }

    /**
     * Initialize the contents of the Activity's standard options menu. You should place your menu
     * items in to menu. For this method to be called, you must have first called setHasOptionsMenu(boolean).
     * See Activity.onCreateOptionsMenu for more information.
     *
     * @param menu     - The options menu in which you place your items.
     * @param inflater - The inflater.
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.gamesel, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * This hook is called whenever an item in your options menu is selected. The default
     * implementation simply returns false to have the normal processing happen (calling the
     * item's Runnable or sending a message to its Handler as appropriate). You can use
     * this method for any items for which you would like to do processing without those other facilities.
     * <p>
     * Derived classes should call through to the base class for it to perform the default menu handling.
     *
     * @param item - The menu item that was selected.
     * @return Return false to allow normal menu processing to proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final FragmentActivity act = getActivity();
        if (act == null || act.isFinishing()) {
            FabLogger.error("GameSelectorFragment: cannot retrieve parent activity or activity is finishing, onOptionsItemSelected aborted.");
            return false;
        }

        switch (item.getItemId()) {
            case R.id.action_settings: {
                // Show settings UI
                Intent i = new Intent(act, PreferencesActivity.class);
                startActivity(i);
                return true;
            }

            case R.id.action_sortOrder: {
                // Prompt the user to specify their preferred sort order
                AlertDialog.Builder builder = new AlertDialog.Builder(act);
                builder.setTitle(R.string.menu_sortBy);

                @SuppressLint("InflateParams") View dialogView = LayoutInflater.from(builder.getContext()).inflate(R.layout.glk_file_selector, null);
                final ListView lv = dialogView.findViewById(R.id.lvGlkFiles);
                final ArrayAdapter lst = ArrayAdapter.createFromResource(builder.getContext(),
                        R.array.sort_array, android.R.layout.simple_list_item_single_choice);
                final int[] sortIndices = getResources().getIntArray(R.array.sort_array_index);
                final int[] pos = new int[1];
                lv.setAdapter(lst);
                lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);      // note: this is necessary to enable the checkboxes
                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
                        pos[0] = position;
                    }
                });

                // Initialise the list to have the current sort order selected
                for (int i = 0, sz = sortIndices.length; i < sz; i++) {
                    if (sortIndices[i] == mSortIndex) {
                        lv.setItemChecked(i, true);
                        break;
                    }
                }
                builder.setView(dialogView);
                builder.setNegativeButton(R.string.dialog_sort_asc, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setSortOrder(sortIndices[pos[0]], true);
                    }
                });
                builder.setPositiveButton(R.string.dialog_sort_desc, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setSortOrder(sortIndices[pos[0]], false);
                    }
                });
                builder.setNeutralButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
                return true;
            }

            case R.id.action_refresh: {
                // Refresh the database
                AsyncProgressFragment frag = new AsyncProgressFragment();
                act.getSupportFragmentManager().beginTransaction().add(frag, DB_REFRESH_TAG).commit();
                try {
                    File gameDir = new File(GLKConstants.getDir(act, GLKConstants.SUBDIR.GAME));
                    frag.refreshDatabase(act.getResources(), act, mDbHelper, gameDir, this);
                } catch (@NonNull IOException | SecurityException e) {
                    FabLogger.error("  => Could not access game directory. Please check you have granted file read/write permissions and try again. If you have overridden the default paths in your settings, also check that Fabularium has read/write access to that path.");
                }
                return true;
            }

            case R.id.action_resetConfig: {
                // Reset fab.ini to the latest defaults
                AlertDialog.Builder builder = new AlertDialog.Builder(act);
                builder.setTitle(R.string.dialog_reset_config);
                builder.setMessage("About to reset Fabularium's config files to their default settings. " +
                        "If you want to preserve any custom settings, please back up those files before proceeding. Ready?");
                builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (act instanceof MainActivity) {
                            try {
                                String path = GLKConstants.getDir(act, null).concat("fab.ini");
                                File f = new File(path);
                                f.delete();
                                path = GLKConstants.getDir(act, null).concat("keyboards.ini");
                                f = new File(path);
                                f.delete();
                                ((MainActivity) act).copyCoreAssets();
                                Toast.makeText(act, "Config files reset.", Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                Toast.makeText(act, "Could not reset config files: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

            case R.id.action_resetAdrift: {
                // Reset the ADRIFT runtime library to the latest defaults
                AlertDialog.Builder builder = new AlertDialog.Builder(act);
                builder.setTitle(R.string.dialog_reset_adrift);
                builder.setMessage("About to reset the ADRIFT 5 library (StandardLibrary.amf) used by Bebek to the latest default. " +
                        "If you want to preserve any custom changes, please back up that file before proceeding. Ready?");
                builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (act instanceof MainActivity) {
                            try {
                                String path = GLKConstants.getDir(act, GLKConstants.SUBDIR.LIB_ADRIFT).concat("StandardLibrary.amf");
                                File f = new File(path);
                                f.delete();
                                ((MainActivity) act).copyCoreAssets();
                                Toast.makeText(act, "Standard ADRIFT 5 library reset.", Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                Toast.makeText(act, "Could not reset standard ADRIFT 5 library: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

            case R.id.action_licenses: {
                // Show information about the other components
                UrlDialogFragment dialog = UrlDialogFragment.newInstance();
                dialog.setUrl(getString(R.string.html_licences_path_EN)).setTitle(getString(R.string.dialog_licences_title));
                dialog.show(act.getSupportFragmentManager(), URL_DIALOG_TAG);
                return true;
            }

            case R.id.action_help: {
                UrlDialogFragment dialog = UrlDialogFragment.newInstance();
                dialog.setUrl(getString(R.string.html_help_path_EN)).setTitle(getString(R.string.dialog_help_title));
                dialog.show(act.getSupportFragmentManager(), URL_DIALOG_TAG);
                return true;
            }

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
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
        Cursor cursor = (Cursor) parent.getItemAtPosition(position);
        String ifid = cursor.getString(1);  // assume this is the IFID, based on the way we created the adaptor
        SQLiteDatabase dbReader = mDbHelper.getReadableDatabase();
        IFictionRecord ifr = IFictionDbHelper.get(ifid, dbReader);
        dbReader.close();
        if (ifr == null) {
            Toast.makeText(getActivity(), "Cannot find IFID " + ifid + " in the database.  You need to refresh it.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        File f = new File(ifr.file_path);
        if (!f.exists()) {
            Toast.makeText(getActivity(), "Cannot load game, file " + ifr.file_path + " doesn't exist.  You need to refresh the database.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(getActivity(), GLKActivity.class);
        intent.putExtra(MainActivity.GAME_PATH, ifr.file_path);
        intent.putExtra(MainActivity.GAME_FORMAT, ifr.format);
        intent.putExtra(MainActivity.GAME_IFID, ifr.ifids[0]);
        intent.putExtra(MainActivity.USER_PREFS, PreferencesActivity.getReadOnlyPrefs(getContext()));
        startActivity(intent);
    }

    /**
     * Callback method to be invoked when an item in this view has been clicked and held.
     * Implementers can call getItemAtPosition(position) if they need to access the
     * data associated with the selected item.
     *
     * @param parent   - The AdapterView where the click happened.
     * @param view     - The view within the AdapterView that was clicked
     * @param position - The position of the view in the list
     * @param id       - The row id of the item that was clicked
     * @return true if the callback consumed the long click, false otherwise
     */
    @Override
    public boolean onItemLongClick(@NonNull AdapterView<?> parent, @NonNull View view, int position, long id) {
        Context c = getContext();
        if (c != null) {
            mSelPosition = position;
            PopupMenu popup = new PopupMenu(c, view);
            popup.setOnMenuItemClickListener(this);
            popup.inflate(R.menu.gamesel_context);
            popup.show();
        }
        return true;
    }

    @Override
    public boolean onMenuItemClick(@NonNull MenuItem item) {
        // Popup menu callback
        final FragmentActivity act = getActivity();
        if (act == null || act.isFinishing()) {
            FabLogger.error("GameSelectorFragment: cannot retrieve parent activity or activity is finishing, popup menu command aborted.");
            return false;
        }

        switch (item.getItemId()) {
            case R.id.action_showMetadata: {
                Cursor cursor = (Cursor) mGridView.getItemAtPosition(mSelPosition);
                String ifid = cursor.getString(1);  // assume this is the IFID, based on the way we created the adaptor
                SQLiteDatabase dbReader = mDbHelper.getReadableDatabase();
                IFictionRecord ifr = IFictionDbHelper.get(ifid, dbReader);
                dbReader.close();
                if (ifr == null) {
                    Toast.makeText(act, "Cannot find IFID " + ifid + " in the database.  You need to refresh it.",
                            Toast.LENGTH_LONG).show();
                    return true;
                }
                IFictionDialogFragment d = new IFictionDialogFragment();
                Bundle b = new Bundle();
                b.putParcelable(MainActivity.DEFAULT_BITMAP, mDefaultIcon);
                d.setArguments(b);
                d.setIFictionRecord(ifr);
                d.show(act.getSupportFragmentManager(), IFICTION_DIALOG_TAG);
                return true;
            }
            case R.id.action_getMetadata: {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(act);
                String metadataUrl = sharedPref.getString(PreferencesActivity.KEY_PREF_SERVER_URL, METADATA_DEF_SERVER_IFID);
                if (metadataUrl.equals("")) {
                    Toast.makeText(act, "Metadata server url in the settings is blank. Please specify it, then rerun this command.",
                            Toast.LENGTH_LONG).show();
                    return true;
                }

                Cursor cursor = (Cursor) mGridView.getItemAtPosition(mSelPosition);
                String ifid = cursor.getString(1);  // assume this is the IFID, based on the way we created the adaptor
                SQLiteDatabase dbReader = mDbHelper.getReadableDatabase();
                IFictionRecord ifr = IFictionDbHelper.get(ifid, dbReader);
                dbReader.close();
                if (ifr == null) {
                    Toast.makeText(act, "Cannot find IFID " + ifid + " in the database.  You need to refresh it.",
                            Toast.LENGTH_LONG).show();
                    return true;
                }
                IFictionRecord[] ifrs = new IFictionRecord[1];
                ifrs[0] = ifr;

                // Run the task in a new fragment
                AsyncProgressFragment f = new AsyncProgressFragment();
                act.getSupportFragmentManager().beginTransaction().add(f, "frag_gamesel").commit();
                f.downloadMetadata(act.getResources(), this, act, metadataUrl, ifrs);
                return true;
            }
            case R.id.action_removeFromGameLibrary: {
                Cursor cursor = (Cursor) mGridView.getItemAtPosition(mSelPosition);
                final String ifid = cursor.getString(1);  // assume this is the IFID, based on the way we created the adaptor
                final String title = cursor.getString(2);   // assume this is the title, based on the way we created the adaptor

                AlertDialog.Builder builder = new AlertDialog.Builder(act);
                builder.setTitle(R.string.dialog_remove_game_title);
                builder.setMessage("About to remove '" + title +
                        "' from your game library. This will also delete any downloaded metadata.");
                builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        boolean ret = mDbHelper.deleteGameMetaData(ifid);
                        if (!act.isFinishing()) {
                            Toast.makeText(act,
                                    ret ? "Removed '" + title + "' from the game library." :
                                            "Could not remove '" + title + "' from the game library.",
                                    Toast.LENGTH_LONG).show();
                            restartLoader();
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
            default:
                // not recognised - do nothing
                return false;
        }
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param loaderID - The ID whose loader is to be created.
     * @param args     - Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Nullable
    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, @Nullable Bundle args) {
        final FragmentActivity act = getActivity();
        if (act == null || act.isFinishing()) {
            FabLogger.error("GameSelectorFragment: cannot retrieve parent activity or activity is finishing, create loader aborted.");
            return null;
        }

        switch (loaderID) {
            case MainActivity.DB_LOADER:
                // construct the sort statement based on args passed in
                // default to ascending sort by game titles
                int sortIndex = (args != null) ? args.getInt(DB_LOADER_SORT_ORDER, 4) : 4;
                boolean asc = (args == null) || args.getBoolean(DB_LOADER_SORT_ASC, true);
                String sortOrder;
                if (sortIndex < 0 || sortIndex > IFictionDbHelper.PROJ_ALL.length) {
                    FabLogger.warn("GameSelector: onCreateLoader: unrecognised sort order index");
                    sortOrder = null;
                } else {
                    sortOrder = IFictionDbHelper.PROJ_ALL[sortIndex];
                }
                sortOrder += (asc ? " COLLATE LOCALIZED ASC" : " COLLATE LOCALIZED DESC");
                return new GameSelectorLoader(act,          // Parent activity context
                        IFictionDbHelper.PROJ_SUMMARY,      // Projection to return
                        null,                       // No selection clause
                        null,                    // No selection arguments
                        sortOrder,
                        mDbHelper);
            default:
                // An invalid id was passed in
                FabLogger.error("GameSelector: invalid ID passed to onCreateLoader.");
                return null;
        }
    }

    /**
     * Called when a previously created loader has finished its load. Note that normally an
     * application is not allowed to commit fragment transactions while in this call, since it can
     * happen after an activity's state is saved. See FragmentManager.openTransaction() for further discussion on this.
     * <p>
     * This function is guaranteed to be called prior to the release of the last data that was
     * supplied for this Loader. At this point you should remove all use of the old data (since
     * it will be released soon), but should not do your own release of the data since its Loader
     * owns it and will take care of that. The Loader will take care of management of its
     * data so you don't have to. In particular:
     * <p>
     * (1) The Loader will monitor for changes to the data, and report them to you through new calls here.
     * You should not monitor the data yourself. For example, if the data is a Cursor and you place it
     * in a CursorAdapter, use the CursorAdapter(android.content.Context, android.database.Cursor, int)
     * constructor without passing in either FLAG_AUTO_REQUERY or FLAG_REGISTER_CONTENT_OBSERVER (that is,
     * use 0 for the flags argument). This prevents the CursorAdapter from doing its own observing of the
     * Cursor, which is not needed since when a change happens you will get a new Cursor throw another call here.
     * <p>
     * (2) The Loader will release the data once it knows the application is no longer using it. For
     * example, if the data is a Cursor from a CursorLoader, you should not call close() on it yourself.
     * If the Cursor is being placed in a CursorAdapter, you should use the swapCursor(android.database.Cursor)
     * method so that the old Cursor is not closed.
     *
     * @param loader - The Loader that has finished.
     * @param data   - The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, @NonNull Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
    }

    /**
     * Called when a previously created loader is being reset, and thus making its
     * data unavailable. The application should at this point remove any references
     * it has to the Loader's data.
     *
     * @param loader - The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    /**
     * Binds the Cursor column defined by the specified index to the specified view. When
     * binding is handled by this ViewBinder, this method must return true.
     * If this method returns false, SimpleCursorAdapter will attempts to
     * handle the binding on its own.
     *
     * @param view        - the view to bind the data to
     * @param cursor      -  the cursor to get the data from
     * @param columnIndex - the column at which the data can be found in the cursor
     * @return true if the data was bound to the view, false otherwise
     */
    @Override
    public boolean setViewValue(@NonNull View view, @NonNull Cursor cursor, int columnIndex) {
        if (columnIndex == 1) {
            Context c = getContext();
            if (c != null && mDefaultIcon != null) {
                // N.B. We assume that the second column of the cursor is
                // the ifid, based on how the mAdapter is constructed in the onCreate()
                // method above.  We could call cursor.getColumnName(columnIndex) to
                // check, but we're trying to avoid the performance overhead.
                // We also assume that the view we're binding to is a TextView, again
                // so we don't have the performance overheads of checking.
                String ifid = cursor.getString(columnIndex);
                AsyncLoaders.loadBitmap(ifid, (TextView) view, getContext(), mDefaultIcon, mIconWidth, mIconHeight);
            }
            return true;
        }
        return false;
    }

    /**
     * A callback from RefreshDbFragment when the database has finished refreshing.
     */
    @Override
    public void onPostExecute() {
        // Hide the progress dialog
        restartLoader();
    }

    @Override
    public void onDownloadMetadataPostExecute(@Nullable IFictionRecord[] ifrs, @Nullable String lastErrorMsg) {
        // Callback once metadata download has completed (or timed out)
        FragmentActivity act = getActivity();
        if (act == null || act.isFinishing()) {
            FabLogger.error("GameSelectorFragment: onDownloadMetadataPostExecute - cannot retrieve activity, or the activity is finishing.");
            return;
        }

        if (ifrs == null) {
            Toast.makeText(act, "Could not download any metadata: " + lastErrorMsg, Toast.LENGTH_LONG).show();
            return;
        }

        int nDone = 0;
        for (IFictionRecord ifr : ifrs) {
            if (ifr != null) {
                SQLiteDatabase dbWriter = mDbHelper.getWritableDatabase();
                IFictionDbHelper.put(ifr, dbWriter);
                dbWriter.close();
                nDone++;
            }
        }

        if (nDone > 0) {
            // Restart the loader to pull in the updated data
            act.getSupportLoaderManager().restartLoader(MainActivity.DB_LOADER, null, this);
        }

        if (nDone < ifrs.length) {
            FabLogger.error("Unable to download all the metadata: " + lastErrorMsg);
            Toast.makeText(act, "Unable to download all the metadata: " + lastErrorMsg, Toast.LENGTH_LONG).show();
        }
    }

    public void restartLoader() {
        FragmentActivity act = getActivity();
        if (act == null || act.isFinishing()) {
            FabLogger.error("GameSelector: Cannot restart loader as activity is null or finishing.");
            return;
        }

        // Restart the loader to do a new query and pull in the updated data.
        Bundle args = new Bundle();
        args.putInt(DB_LOADER_SORT_ORDER, mSortIndex);
        args.putBoolean(DB_LOADER_SORT_ASC, mSortAscending);
        act.getSupportLoaderManager().restartLoader(MainActivity.DB_LOADER, args, this);
    }

    private void loadGameList() {
        FragmentActivity act = getActivity();
        if (act == null || act.isFinishing()) {
            FabLogger.error("GameSelector: Cannot load game list as activity is null or finishing.");
            return;
        }

        // Set up the grid view
        mAdapter = new SimpleCursorAdapter(act,
                R.layout.game_selector_cell, null,
                new String[]{IFictionDbHelper.IFictionEntry.COLUMN_NAME_TITLE, IFictionDbHelper.IFictionEntry.COLUMN_NAME_ENTRY_ID},
                new int[]{R.id.tvGameInfo, R.id.tvGameInfo}, 0);
        mAdapter.setViewBinder(this);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(this);
        mGridView.setOnItemLongClickListener(this);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        Bundle args = new Bundle();
        args.putInt(DB_LOADER_SORT_ORDER, mSortIndex);
        args.putBoolean(DB_LOADER_SORT_ASC, mSortAscending);
        act.getSupportLoaderManager().initLoader(MainActivity.DB_LOADER, args, this);
    }

    private void setSortOrder(int colIndex, boolean isAscending) {
        // changes the sort order of the ifiction entries in the gridview
        mSortIndex = colIndex;
        mSortAscending = isAscending;
        restartLoader();
    }

    private static class GameSelectorLoader extends CursorLoader {
        @NonNull
        private final IFictionDbHelper mDbHelper;

        GameSelectorLoader(@NonNull Context context, @NonNull String[] projection,
                           @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder,
                           @NonNull IFictionDbHelper dbHelper) {
            // We don't need a URI because we also override loadInBackground() below
            super(context, Uri.EMPTY, projection, selection, selectionArgs, sortOrder);
            mDbHelper = dbHelper;
        }

        @Override
        public Cursor loadInBackground() {
            final SQLiteDatabase db = mDbHelper.getReadableDatabase();
            return db.query(IFictionDbHelper.IFictionEntry.TABLE_NAME, getProjection(),
                    getSelection(), getSelectionArgs(), null, null, getSortOrder(), null);
        }
    }
}
