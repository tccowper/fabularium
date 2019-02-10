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
package com.luxlunae.fabularium.create;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.luxlunae.fabularium.AsyncLoaders;
import com.luxlunae.fabularium.AsyncProgressFragment;
import com.luxlunae.fabularium.FabLogger;
import com.luxlunae.fabularium.MainActivity;
import com.luxlunae.fabularium.R;
import com.luxlunae.fabularium.TextEditorActivity;
import com.luxlunae.fabularium.UrlDialogFragment;
import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKUtils;

import java.io.File;
import java.io.IOException;

/**
 * The main interface that appears under the CREATE tab and allows the user to browse and manage
 * their project library. Project library metadata is stored in the user's private Fabularium database
 * (see ProjectDbHelper).
 */
public class ProjectSelectorFragment extends Fragment
        implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
        LoaderManager.LoaderCallbacks<Cursor>, SimpleCursorAdapter.ViewBinder,
        AsyncProgressFragment.RefreshDbCallbacks {
    private final static String DB_LOADER_SORT_ORDER = "com.luxlunae.fabularium.PROJECT_SORT_ORDER";
    private final static String DB_LOADER_SORT_ASC = "com.luxlunae.fabularium.PROJECT_SORT_ASC";
    private final static String DB_REFRESH_TAG = "com.luxlunae.fabularium.PROJECT_REFRESH_DB_TAG";
    private final static String URL_DIALOG_TAG = "com.luxlunae.fabularium.PROJECT_URL_DIALOG_TAG";
    private final static String PROJECT_DIALOG_TAG = "com.luxlunae.fabularium.PROJECT_DIALOG_TAG";

    private final static int ICON_WIDTH = 48;      // in dp
    private final static int ICON_HEIGHT = 48;     // in dp

    @Nullable
    private ProjectDbHelper mDbHelper;
    private SimpleCursorAdapter mAdapter;
    @Nullable
    private Bitmap mInformIcon;
    @Nullable
    private Bitmap mTADSIcon;
    private int mIconWidth;
    private int mIconHeight;
    private int mSortIndex = 1;
    private boolean mSortAscending = true;

    private GridView mGridView;

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
            mDbHelper = new ProjectDbHelper(act);
        } else {
            // According to Android docs, onCreate is always called after onAttach, so a getActivity() call here shouldn't return NULL
            FabLogger.error("ANDROID ERROR: ProjectSelector: getActivity() returned NULL in onCreate! This should not happen! Expect failure.");
        }
        DisplayMetrics dm = getResources().getDisplayMetrics();
        mIconWidth = GLKUtils.dpToPx(ICON_WIDTH, dm);
        mIconHeight = GLKUtils.dpToPx(ICON_HEIGHT, dm);
        mInformIcon = GLKUtils.getBitmapFromResource(getResources(), R.mipmap.inform7, mIconWidth, mIconHeight);
        mTADSIcon = GLKUtils.getBitmapFromResource(getResources(), R.mipmap.qtads, mIconWidth, mIconHeight);
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
        mGridView = fr.findViewById(R.id.gvGames);
        TextView empty = fr.findViewById(android.R.id.empty);
        empty.setText(R.string.empty_project_list);
        empty.setContentDescription(getString(R.string.empty_project_list));
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
     *
     * If you override this method you must call through to the superclass implementation.
     */
    @Override
    public void onStart() {
        super.onStart();
        loadProjectsList();
    }

    @Override
    public void onStop() {
        super.onStop();
        getLoaderManager().destroyLoader(0);
    }

    /**
     * Initialize the contents of the Activity's standard options menu. You should place
     * your menu items in to menu. For this method to be called, you must have first
     * called setHasOptionsMenu(boolean). See Activity.onCreateOptionsMenu for more information.
     *
     * @param menu - The options menu in which you place your items.
     * @param inflater - The inflater.
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.projsel, menu);
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
        final FragmentActivity act = getActivity();
        if (act == null || act.isFinishing()) {
            FabLogger.error("ProjectSelectorFragment: cannot retrieve parent activity or activity is finishing, onOptionsItemSelected aborted.");
            return false;
        }

        switch (item.getItemId()) {
            case R.id.projaction_refresh: {
                // Refresh the database
                AsyncProgressFragment frag = new AsyncProgressFragment();
                act.getSupportFragmentManager().beginTransaction().add(frag, DB_REFRESH_TAG).commit();
                try {
                    File projectsDir = new File(GLKConstants.getDir(act, GLKConstants.SUBDIR.PROJECTS));
                    frag.refreshDatabase(act.getResources(), act, mDbHelper, projectsDir, this);
                } catch (@NonNull IOException | SecurityException e) {
                    FabLogger.error("  => Could not access projects directory. Please check you have granted file read/write permissions and try again. If you have overridden the default paths in your settings, also check that Fabularium has read/write access to that path.");
                }
                return true;
            }

            case R.id.projaction_licenses: {
                // Show information about the other components
                UrlDialogFragment dialog = UrlDialogFragment.newInstance();
                dialog.setUrl(getString(R.string.html_licences_path_EN)).setTitle(getString(R.string.dialog_licences_title));
                dialog.show(act.getSupportFragmentManager(), URL_DIALOG_TAG);
                return true;
            }

            case R.id.projaction_help: {
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
        String projid = cursor.getString(1);        // assume this is the project ID, based on how we created the adapter
        SQLiteDatabase dbReader = mDbHelper.getReadableDatabase();
        ProjectDbHelper.ProjectRecord pr = ProjectDbHelper.get(projid, dbReader);
        dbReader.close();
        if (pr == null) {
            Toast.makeText(getActivity(),
                    "Cannot find project " + projid + " in the database.  You need to refresh it.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(getActivity(), TextEditorActivity.class);
        intent.putExtra(TextEditorActivity.PROJECT_TITLE, pr.title);
        intent.putExtra(TextEditorActivity.SOURCEFILES, pr.sourceFiles);
        intent.putExtra(TextEditorActivity.COMPILER, pr.compiler);
        intent.putExtra(TextEditorActivity.COMPILER_ARGS, pr.compilerArgs);
        intent.putExtra(TextEditorActivity.GAME_PATH, pr.outputFile);
        intent.putExtra(TextEditorActivity.GAME_FORMAT, pr.outputFormat);
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
        final FragmentActivity act = getActivity();
        if (act == null || act.isFinishing()) {
            FabLogger.error("ProjectSelectorFragment: cannot retrieve parent activity or activity is finishing, onItemLongClick aborted.");
            return false;
        }

        Cursor cursor = (Cursor) parent.getItemAtPosition(position);
        String projid = cursor.getString(1);        // assume this is the project ID, based on how we created the adapter
        SQLiteDatabase dbReader = mDbHelper.getReadableDatabase();
        ProjectDbHelper.ProjectRecord pr = ProjectDbHelper.get(projid, dbReader);
        dbReader.close();
        if (pr == null) {
            Toast.makeText(act,
                    "Cannot find project " + projid + " in the database.  You need to refresh it.",
                    Toast.LENGTH_LONG).show();
            return true;
        }
        ProjectDialogFragment d = new ProjectDialogFragment();
        d.setProjectRecord(pr);
        d.setTargetFragment(this, 0);
        d.show(act.getSupportFragmentManager(), PROJECT_DIALOG_TAG);
        return true;
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param loaderID - The ID whose loader is to be created.
     * @param args - Any arguments supplied by the caller.
     *
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Nullable
    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, @Nullable Bundle args) {
        final FragmentActivity act = getActivity();
        if (act == null || act.isFinishing()) {
            FabLogger.error("ProjectSelectorFragment: cannot retrieve parent activity or activity is finishing, create loader aborted.");
            return null;
        }

        switch (loaderID) {
            case MainActivity.DB_LOADER_PROJECTS:
                // construct the sort statement based on args passed in
                // default to ascending sort by project titles
                int sortIndex = (args != null) ? args.getInt(DB_LOADER_SORT_ORDER, 1) : 1;
                boolean asc = (args == null) || args.getBoolean(DB_LOADER_SORT_ASC, true);
                String sortOrder;
                if (sortIndex < 0 || sortIndex > ProjectDbHelper.PROJ_ALL.length) {
                    FabLogger.warn("ProjectSelector: onCreateLoader: unrecognised sort order index");
                    sortOrder = null;
                } else {
                    sortOrder = ProjectDbHelper.PROJ_ALL[sortIndex];
                }
                sortOrder += (asc ? " COLLATE LOCALIZED ASC" : " COLLATE LOCALIZED DESC");

                return new ProjectSelectorLoader(act,   // Parent activity context
                        ProjectDbHelper.PROJ_SUMMARY,   // Projection to return
                        null,                  // No selection clause
                        null,               // No selection arguments
                        sortOrder,
                        mDbHelper);
            default:
                // An invalid id was passed in
                FabLogger.error("ProjectSelector: invalid ID passed to onCreateLoader.");
                return null;
        }
    }

    /**
     * Called when a previously created loader has finished its load. Note that normally an
     * application is not allowed to commit fragment transactions while in this call, since it can
     * happen after an activity's state is saved. See FragmentManager.openTransaction() for further discussion on this.
     *
     * This function is guaranteed to be called prior to the release of the last data that was
     * supplied for this Loader. At this point you should remove all use of the old data (since
     * it will be released soon), but should not do your own release of the data since its Loader
     * owns it and will take care of that. The Loader will take care of management of its
     * data so you don't have to. In particular:
     *
     * (1) The Loader will monitor for changes to the data, and report them to you through new calls here.
     * You should not monitor the data yourself. For example, if the data is a Cursor and you place it
     * in a CursorAdapter, use the CursorAdapter(android.content.Context, android.database.Cursor, int)
     * constructor without passing in either FLAG_AUTO_REQUERY or FLAG_REGISTER_CONTENT_OBSERVER (that is,
     * use 0 for the flags argument). This prevents the CursorAdapter from doing its own observing of the
     * Cursor, which is not needed since when a change happens you will get a new Cursor throw another call here.
     *
     * (2) The Loader will release the data once it knows the application is no longer using it. For
     * example, if the data is a Cursor from a CursorLoader, you should not call close() on it yourself.
     * If the Cursor is being placed in a CursorAdapter, you should use the swapCursor(android.database.Cursor)
     * method so that the old Cursor is not closed.
     *
     * @param loader - The Loader that has finished.
     * @param data - The data generated by the Loader.
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
     * @param view - the view to bind the data to
     * @param cursor -  the cursor to get the data from
     * @param columnIndex - the column at which the data can be found in the cursor
     *
     * @return true if the data was bound to the view, false otherwise
     */
    @Override
    public boolean setViewValue(@NonNull View view, @NonNull Cursor cursor, int columnIndex) {
        if (columnIndex == 3) {
            Context c = getContext();
            if (c != null) {
                // N.B. We assume that the fourth column of the cursor is
                // the project type, based on how the mAdapter is constructed in the onCreate()
                // method above.  We could call cursor.getColumnName(columnIndex) to
                // check, but we're trying to avoid the performance overhead.
                // We also assume that the view we're binding to is a TextView, again
                // so we don't have the performance overheads of checking.
                String compiler = cursor.getString(columnIndex);
                switch (compiler) {
                    case "inform":
                        assert mInformIcon != null;
                        AsyncLoaders.loadBitmap((TextView) view, c, mInformIcon, mIconWidth, mIconHeight);
                        break;
                    case "t3make":
                        assert mTADSIcon != null;
                        AsyncLoaders.loadBitmap((TextView) view, c, mTADSIcon, mIconWidth, mIconHeight);
                        break;
                    default:
                        // Shouldn't get here
                        return false;
                }
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

    public void restartLoader() {
        FragmentActivity act = getActivity();
        if (act == null || act.isFinishing()) {
            FabLogger.error("ProjectSelector: Cannot restart loader as activity is null or finishing.");
            return;
        }

        // Restart the loader to do a new query and pull in the updated data.
        Bundle args = new Bundle();
        args.putInt(DB_LOADER_SORT_ORDER, mSortIndex);
        args.putBoolean(DB_LOADER_SORT_ASC, mSortAscending);
        act.getSupportLoaderManager().restartLoader(MainActivity.DB_LOADER_PROJECTS, args, this);
    }

    private void loadProjectsList() {
        FragmentActivity act = getActivity();
        if (act == null || act.isFinishing()) {
            FabLogger.error("ProjectSelector: Cannot load projects list as activity is null or finishing.");
            return;
        }

        // Set up the grid view
        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.game_selector_cell, null,
                new String[]{ProjectDbHelper.ProjectEntry.COLUMN_NAME_TITLE, ProjectDbHelper.ProjectEntry.COLUMN_NAME_COMPILER},
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
        getActivity().getSupportLoaderManager().initLoader(MainActivity.DB_LOADER_PROJECTS, args, this);
    }

    private static class ProjectSelectorLoader extends CursorLoader {
        @NonNull
        private final ProjectDbHelper mDbHelper;

        ProjectSelectorLoader(@NonNull Context context, @Nullable String[] projection,
                              @Nullable String selection, @Nullable String[] selectionArgs,
                              @NonNull String sortOrder, @NonNull ProjectDbHelper dbHelper) {
            // We don't need a URI because we also override loadInBackground() below
            super(context, Uri.EMPTY, projection, selection, selectionArgs, sortOrder);
            mDbHelper = dbHelper;
        }

        @Override
        public Cursor loadInBackground() {
            final SQLiteDatabase db = mDbHelper.getReadableDatabase();
            return db.query(ProjectDbHelper.ProjectEntry.TABLE_NAME, getProjection(),
                    getSelection(), getSelectionArgs(), null, null, getSortOrder(), null);
        }
    }
}
