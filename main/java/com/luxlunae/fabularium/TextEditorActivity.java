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

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.luxlunae.fabularium.create.ProjectDbHelper;
import com.luxlunae.glk.GLKActivity;
import com.luxlunae.glk.GLKConstants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TextEditorActivity extends AppCompatActivity {
    public final static String TEXTFILE = "com.luxlunae.fabularium.TEXTFILE";

    public final static String PROJECT_TITLE = "com.luxlunae.fabularium.PROJTITLE";
    public final static String SOURCEFILES = "com.luxlunae.fabularium.SOURCEFILES";
    public final static String COMPILER = "com.luxlunae.fabularium.COMPILER";
    public final static String COMPILER_ARGS = "com.luxlunae.fabularium.COMPILERARGS";
    public final static String PROJECT_TYPE = "com.luxlunae.fabularium.PROJTYPE";
    public final static String GAME_PATH = "com.luxlunae.fabularium.GAMEPATH";
    public final static String GAME_FORMAT = "com.luxlunae.fabularium.GAMEFORMAT";

    private static final boolean DEBUG_LIFECYCLE = false;

    private File mTextFile;

    private String mProjectTitle;
    private File[] mFiles;
    private TextEditorView mTextEditor;
    private boolean mIdeMode;
    private String mCompiler;
    private String mCompilerArgs;
    private String mGamePath;
    private String mGameFormat;
    private RunProgramResultReceiver mProgResultReceiver;
    private ViewPager mViewPager;
    @Nullable
    private PagerAdapter mPagerAdapter;

    /**
     * Called when the activity is starting. This is where most initialization should go:
     * calling setContentView(int) to inflate the activity's UI, using findViewById(int)
     * to programmatically interact with widgets in the UI, calling
     * managedQuery(android.net.Uri, String[], String, String[], String) to
     * retrieve cursors for data being displayed, etc.
     * <p>
     * You can call finish() from within this function, in which case onDestroy()
     * will be immediately called without any of the rest of the activity lifecycle (onStart(), onResume(), onPause(), etc) executing.
     * <p>
     * Derived classes must call through to the super class's implementation of this method. If they do not, an exception will be thrown.
     * <p>
     * This method must be called from the main thread of your app.
     * <p>
     * If you override this method you must call through to the superclass implementation.
     *
     * @param savedInstanceState -  If the activity is being re-initialized after previously being shut down
     *                           then this Bundle contains the data it most recently supplied in
     *                           onSaveInstanceState(Bundle). Note: Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /* This activity caters to two types of functions:
           (1) a stand-alone text editor for one file if TEXTFILE is specified;
           (2) a multi-file IDE otherwise.
         */
        // N.B. According to https://developer.android.com/topic/libraries/architecture/saving-states.html:
        //  "Additionally, when you open an activity from an intent, the bundle of extras is
        //   delivered to the activity both when the configuration changes and when the system
        //   restores the activity."
        // So we don't need to save anything special when restoring state.
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String fName = intent.getStringExtra(TEXTFILE);
        mIdeMode = (fName == null);

        if (mIdeMode) {
            // we need to show tabs
            setContentView(R.layout.ide_layout);

            String[] filePaths = intent.getStringExtra(SOURCEFILES).split(ProjectDbHelper.SOURCE_FILE_DELIMITER);
            mFiles = new File[filePaths.length];
            for (int i = 0; i < mFiles.length; i++) {
                mFiles[i] = new File(filePaths[i]);
            }
            mProjectTitle = intent.getStringExtra(PROJECT_TITLE);
            setTitle(mProjectTitle);
            mCompiler = intent.getStringExtra(COMPILER);
            mCompilerArgs = intent.getStringExtra(COMPILER_ARGS);
            mGamePath = intent.getStringExtra(GAME_PATH);
            mGameFormat = intent.getStringExtra(GAME_FORMAT);
            mViewPager = findViewById(R.id.IDEpager);
            TabLayout tabLayout = findViewById(R.id.IDEtabs);
            tabLayout.setupWithViewPager(mViewPager);

            // Set up the toolbar
            Toolbar myToolbar = findViewById(R.id.IDEtoolbar);
            setSupportActionBar(myToolbar);

            setupServiceReceiver(this);
        } else {
            // only one text file, no tabs
            setContentView(R.layout.text_editor_layout);
            mTextEditor = findViewById(R.id.SingleTextEditor);
            mTextFile = new File(intent.getStringExtra(TEXTFILE));
            setTitle(mTextFile.getName());

            // Set up the toolbar
            Toolbar myToolbar = findViewById(R.id.IDEtoolbar);
            setSupportActionBar(myToolbar);
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu. You should place your menu
     * items in to menu.
     * <p>
     * This is only called once, the first time the options menu is displayed. To update the
     * menu every time it is displayed, see onPrepareOptionsMenu(Menu).
     * <p>
     * The default implementation populates the menu with standard system menu items. These are
     * placed in the CATEGORY_SYSTEM group so that they will be correctly ordered with
     * application-defined menu items. Deriving classes should always call through to the base
     * implementation.
     * <p>
     * You can safely hold on to menu (and any items created from it), making modifications to it
     * as desired, until the next time onCreateOptionsMenu() is called.
     * <p>
     * When you add items to the menu, you can implement the Activity's
     * onOptionsItemSelected(MenuItem) method to handle them there.
     *
     * @param menu - The options menu in which you place your items.
     * @return You must return true for the menu to be displayed; if you return false
     * it will not be shown.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(mIdeMode ? R.menu.texteditor_ide : R.menu.texteditor, menu);
        return true;
    }

    /**
     * Called after onRestoreInstanceState(Bundle), onRestart(), or onPause(),
     * for your activity to start interacting with the user. This is a good place
     * to begin animations, open exclusive-access devices (such as the camera), etc.
     * <p>
     * Keep in mind that onResume is not the best indicator that your activity is
     * visible to the user; a system window such as the keyguard may be in front.
     * Use onWindowFocusChanged(boolean) to know for certain that your activity is
     * visible to the user (for example, to resume a game).
     * <p>
     * Derived classes must call through to the super class's implementation of this
     * method. If they do not, an exception will be thrown.
     * <p>
     * If you override this method you must call through to the superclass implementation.
     */
    @Override
    public void onResume() {
        // (Re-)load and display file(s)
        if (DEBUG_LIFECYCLE) {
            FabLogger.debug("TextEditorActivity: onResume()");
        }
        FabLogger.flush();
        if (mIdeMode) {
            mPagerAdapter = new TextEditorPagerAdapter(getSupportFragmentManager(), mFiles);
            mViewPager.setAdapter(mPagerAdapter);
        } else {
            AsyncProgressFragment frag = new AsyncProgressFragment();
            getSupportFragmentManager().beginTransaction().add(frag, "frag_loadtext").commit();
            frag.loadTextFile(getResources(), mTextEditor, mTextFile);
        }
        super.onResume();
    }

    /**
     * Called as part of the activity lifecycle when an activity is going into
     * the background, but has not (yet) been killed. The counterpart to onResume().
     * <p>
     * When activity B is launched in front of activity A, this callback will be invoked on A.
     * B will not be created until A's onPause() returns, so be sure to not do anything lengthy here.
     * <p>
     * This callback is mostly used for saving any persistent state the activity is editing,
     * to present a "edit in place" model to the user and making sure nothing is lost if
     * there are not enough resources to start the new activity without first killing
     * this one. This is also a good place to do things like stop animations and other
     * things that consume a noticeable amount of CPU in order to make the switch to the next
     * activity as fast as possible, or to close resources that are exclusive access such as the camera.
     * <p>
     * In situations where the system needs more memory it may kill paused processes to reclaim
     * resources. Because of this, you should be sure that all of your state is saved by the
     * time you return from this function. In general onSaveInstanceState(Bundle) is used to save
     * per-instance state in the activity and this method is used to store global persistent data
     * (in content providers, files, etc.)
     * <p>
     * After receiving this call you will usually receive a following call to onStop() (after the
     * next activity has been resumed and displayed), however in some cases there will be a
     * direct call back to onResume() without going through the stopped state.
     * <p>
     * Derived classes must call through to the super class's implementation of this method.
     * If they do not, an exception will be thrown.
     * <p>
     * If you override this method you must call through to the superclass implementation.
     */
    @Override
    public void onPause() {
        // Close all displayed files, to avoid TransactionTooLarge exception
        if (DEBUG_LIFECYCLE) {
            FabLogger.debug("TextEditorActivity: onPause()");
        }
        FabLogger.flush();
        if (mIdeMode) {
            mPagerAdapter = null;
            mViewPager.setAdapter(null);
        } else {
            mTextEditor.setText("");
        }
        super.onPause();
    }

    /**
     * This hook is called whenever an item in your options menu is selected. The default
     * implementation simply returns false to have the normal processing happen (calling the
     * item's Runnable or sending a message to its Handler as appropriate). You can use this
     * method for any items for which you would like to do processing without those other facilities.
     *
     * Derived classes should call through to the base class for it to perform the default menu handling.
     *
     * @param item - The menu item that was selected.
     *
     * @return Return false to allow normal menu processing to proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save_textfile: {
                if (mIdeMode) {
                    // save the current tab
                    int pos = mViewPager.getCurrentItem();
                    TextEditorFragment frag = (TextEditorFragment) mPagerAdapter.instantiateItem(mViewPager, pos);
                    frag.saveFile();
                } else {
                    // save the single text file
                    try {
                        FileWriter writer = new FileWriter(mTextFile);
                        writer.write(mTextEditor.getText().toString());
                        writer.flush();
                        writer.close();
                        Toast.makeText(this, "Saved " + mTextFile.getName(), Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(this, "Error: could not save " + mTextFile.getName(), Toast.LENGTH_LONG).show();
                        return false;
                    }
                }
                return true;
            }

            case R.id.action_compile_textfile: {
                if (mIdeMode) {
                    // save the files
                  /*  TextEditorFragment frag;
                    for (int i = 0; i < mFiles.length; i++) {
                        frag = (TextEditorFragment) mPagerAdapter.instantiateItem(mViewPager, i);
                        frag.saveFile();
                    } */

                    // now run the compile command
                    StringBuilder cmd = new StringBuilder();
                    cmd.append(mCompiler).append(" ");

                    // append the include / lib paths for system directories
                    try {
                        switch (mCompiler) {
                            case "inform":
                                cmd.append("+").append(GLKConstants.getDir(this, GLKConstants.SUBDIR.LIB_INFORM));
                                cmd.append(",").append(GLKConstants.getDir(this, GLKConstants.SUBDIR.INCLUDE_INFORM));
                                cmd.append(" ");
                                break;
                            case "t3make":
                                cmd.append("-FL ").append(GLKConstants.getDir(this, GLKConstants.SUBDIR.LIB_TADS3));
                                cmd.append(" -FI ").append(GLKConstants.getDir(this, GLKConstants.SUBDIR.INCLUDE_TADS3));
                                cmd.append(" -I ").append(GLKConstants.getDir(this, GLKConstants.SUBDIR.LIB_TADS3)).append("/adv3/");
                                cmd.append(" -I ").append(GLKConstants.getDir(this, GLKConstants.SUBDIR.INCLUDE_TADS3)).append("/adv3/en_us/");
                                cmd.append(" ");
                                break;
                            default:
                                // do nothing
                                break;
                        }
                    } catch (@NonNull IOException | SecurityException e) {
                        FabLogger.warn("TextEditorActivity: cannot access one or more of the compiler library / include directories. Check you have granted read/write permissions. If you have overridden the default paths, also check Fabularium has read/write access to the paths you have specified.");
                    }
                    cmd.append(mCompilerArgs);
                    Intent intent = new Intent(this, RunProgramService.class);
                    intent.putExtra(RunProgramService.CMD_ARGS, cmd.toString());
                    intent.putExtra(RunProgramService.CMD_RECEIVER, mProgResultReceiver);
                    startService(intent);
                }
                return true;
            }

            case R.id.action_run_project: {
                if (mIdeMode) {
                    File gameFile = new File(mGamePath);
                    if (!gameFile.exists()) {
                        Toast.makeText(this,
                                "The game file " + mGamePath + " doesn't exist. Try compiling the source code first, then run this command again.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Intent intent = new Intent(this, GLKActivity.class);
                        intent.putExtra(MainActivity.GAME_PATH, mGamePath);
                        intent.putExtra(MainActivity.GAME_FORMAT, mGameFormat);
                        intent.putExtra(MainActivity.GAME_IFID, "????");
                        intent.putExtra(MainActivity.USER_PREFS, PreferencesActivity.getReadOnlyPrefs(this));
                        startActivity(intent);
                    }
                }
                return true;
            }

            case R.id.action_show_line_numbers:
                item.setChecked(!item.isChecked());

                if (mIdeMode) {
                    // save the current tab
                    int pos = mViewPager.getCurrentItem();
                    TextEditorFragment frag = (TextEditorFragment) mPagerAdapter.instantiateItem(mViewPager, pos);
                    frag.toggleLineNumbers();
                } else {
                    mTextEditor.toggleLineNumbers();
                }
                return true;

            case R.id.action_goto_line:
                Toast.makeText(this, "TODO: goto line", Toast.LENGTH_LONG).show();

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupServiceReceiver(@NonNull final Activity act) {
        mProgResultReceiver = new RunProgramResultReceiver(new Handler());
        mProgResultReceiver.setReceiver(new RunProgramResultReceiver.Receiver() {
            @Override
            public void onReceiveResult(int resultCode, @NonNull Bundle resultData) {
                if (resultCode == Activity.RESULT_OK) {
                    int resultVal = resultData.getInt(RunProgramService.CMD_RESULT);
                    String resMsg = resultData.getString(RunProgramService.CMD_RESULT_MSG);

                    // TODO: Put a TICK or CROSS icon depending on whether
                    // command was successful or not (also consider making title
                    // green or red)
                    AlertDialog.Builder builder = new AlertDialog.Builder(act);
                    builder.setTitle(resultVal != 0 ? "Error" : "Success")
                            .setMessage(resMsg)
                            .setPositiveButton("GOT IT", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User closed the dialog
                                }
                            })
                            .show();
                }
            }
        });
    }

    public static class TextEditorFragment extends Fragment {
        public static final String ARG_TEXTFILE = "arg_text_file";
        @Nullable
        private File mTextfile;
        @Nullable
        private TextEditorView mTextEditor;

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState) {
            mTextEditor = new TextEditorView(getContext());
            Bundle args = getArguments();
            String filePath = args.getString(ARG_TEXTFILE);
            if (filePath != null) {
                mTextfile = new File(filePath);
                FragmentActivity act = getActivity();
                if (act != null) {
                    AsyncProgressFragment frag = new AsyncProgressFragment();
                    act.getSupportFragmentManager().beginTransaction().add(frag, "frag_loadtext").commit();
                    frag.loadTextFile(act.getResources(), mTextEditor, mTextfile);
                } else {
                    mTextEditor.setText("<Cannot load file>");
                }
            }
            return mTextEditor;
        }

        private boolean saveFile() {
            if (mTextfile == null) {
                Toast.makeText(getContext(), "Error: could not save", Toast.LENGTH_LONG).show();
                return false;
            }
            try {
                FileWriter writer = new FileWriter(mTextfile);
                writer.write(mTextEditor.getText().toString());
                writer.flush();
                writer.close();
                Toast.makeText(getContext(), "Saved " + mTextfile.getName(), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(getContext(), "Error: could not save " + mTextfile.getName(), Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        }

        private void toggleLineNumbers() {
            mTextEditor.toggleLineNumbers();
        }
    }

    private class TextEditorPagerAdapter extends FragmentStatePagerAdapter {
        private final File[] mFiles;

        TextEditorPagerAdapter(FragmentManager fm, File[] files) {
            super(fm);
            mFiles = files;
        }

        @NonNull
        @Override
        public Fragment getItem(int i) {
            Fragment fragment = new TextEditorFragment();
            Bundle args = new Bundle();
            args.putString(TextEditorFragment.ARG_TEXTFILE, mFiles[i].getAbsolutePath());
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            return mFiles.length;
        }

        @NonNull
        @Override
        public CharSequence getPageTitle(int position) {
            return mFiles[position].getName();
        }
    }
}
