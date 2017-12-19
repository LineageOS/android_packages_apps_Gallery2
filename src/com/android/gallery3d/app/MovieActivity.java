/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.app;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.OnMenuVisibilityListener;
import android.app.KeyguardManager;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
//import android.drm.DrmHelper;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import org.codeaurora.gallery.R;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.Utils;
import org.codeaurora.gallery3d.ext.IActivityHooker;
import org.codeaurora.gallery3d.ext.IMovieItem;
import org.codeaurora.gallery3d.ext.MovieItem;
import org.codeaurora.gallery3d.ext.MovieUtils;
import org.codeaurora.gallery3d.video.ExtensionHelper;
import org.codeaurora.gallery3d.video.MovieTitleHelper;

/**
 * This activity plays a video from a specified URI.
 *
 * The client of this activity can pass a logo bitmap in the intent (KEY_LOGO_BITMAP)
 * to set the action bar logo so the playback process looks more seamlessly integrated with
 * the original activity.
 */
public class MovieActivity extends AbstractPermissionActivity {
    @SuppressWarnings("unused")
    private static final String  TAG = "MovieActivity";
    private static final boolean LOG = false;
    public  static final String  KEY_LOGO_BITMAP = "logo-bitmap";
    public  static final String  KEY_TREAT_UP_AS_BACK = "treat-up-as-back";
    private static final String  VIDEO_SDP_MIME_TYPE = "application/sdp";
    private static final String  VIDEO_SDP_TITLE = "rtsp://";
    private static final String  VIDEO_FILE_SCHEMA = "file";
    private static final String  VIDEO_MIME_TYPE = "video/*";
    private static final String  SHARE_HISTORY_FILE = "video_share_history_file";

    private MoviePlayer mPlayer;
    private boolean     mFinishOnCompletion;
    private Uri         mUri;
    private ImageView   mLiveImg;

    private IMovieItem          mMovieItem;
    private IActivityHooker     mMovieHooker;
    private KeyguardManager     mKeyguardManager;
    private Bundle mSavedInstanceState;

    private boolean mResumed        = false;
    private boolean mControlResumed = false;

    private Intent mShareIntent;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setSystemUiVisibility(View rootView) {
        if (ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_LAYOUT_STABLE) {
            rootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        setContentView(R.layout.movie_view);
        View rootView = findViewById(R.id.movie_view_root);

        setSystemUiVisibility(rootView);

        mLiveImg = (ImageView) findViewById(R.id.img_live);

        Intent intent = getIntent();

        initializeActionBar(intent);
        mFinishOnCompletion = intent.getBooleanExtra(
                MediaStore.EXTRA_FINISH_ON_COMPLETION, true);
        mSavedInstanceState = savedInstanceState;
        if (isPermissionGranted()) {
            init(intent, rootView, savedInstanceState);
        }
        registerScreenReceiver();
        // DRM validation
//        Uri original = intent.getData();
//        String mimeType = intent.getType();
//        String filepath = DrmHelper.getFilePath(this, original);
//        if (DrmHelper.isDrmFile(filepath)) {
//            if (!DrmHelper.validateLicense(this, filepath, mimeType)) {
//                finish();
//            }
//        }
    }

    private void init(Intent intent, View rootView, Bundle savedInstanceState) {
        initMovieInfo(intent);
        mPlayer = new MoviePlayer(rootView, this, mMovieItem, savedInstanceState,
                !mFinishOnCompletion) {
            @Override
            public void onCompletion() {
                if (mFinishOnCompletion) {
                    finishActivity();
                    mControlResumed = false;
                    Bookmarker mBookmarker = new Bookmarker(MovieActivity.this);
                    mBookmarker.setBookmark(mMovieItem.getUri(), 0, 1);
                }
            }
        };
        if (intent.hasExtra(MediaStore.EXTRA_SCREEN_ORIENTATION)) {
            int orientation = intent.getIntExtra(
                    MediaStore.EXTRA_SCREEN_ORIENTATION,
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            if (orientation != getRequestedOrientation()) {
                setRequestedOrientation(orientation);
            }
        }
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
        winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        win.setAttributes(winParams);

        // We set the background in the theme to have the launching animation.
        // But for the performance (and battery), we remove the background here.
        win.setBackgroundDrawable(null);
        initMovieHooker(intent, savedInstanceState);

        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mPlayer.onPrepared(mp);
            }
        });
    }

    private void initMovieHooker(Intent intent, Bundle savedInstanceState) {
        mMovieHooker = ExtensionHelper.getHooker(this);
        mMovieHooker.init(this, intent);
        mMovieHooker.setParameter(null, mPlayer.getMoviePlayerExt());
        mMovieHooker.setParameter(null, mMovieItem);
        mMovieHooker.setParameter(null, mPlayer.getVideoSurface());
        mMovieHooker.onCreate(savedInstanceState);
    }


    @Override
    protected void onGetPermissionsSuccess() {
        init(getIntent(), findViewById(R.id.movie_view_root), mSavedInstanceState);
        mPlayer.requestAudioFocus();
        mMovieHooker.onStart();
        registerScreenReceiver();
    }

    @Override
    protected void onGetPermissionsFailure() {
        finish();
    }

    private void setActionBarLogoFromIntent(Intent intent) {
        Bitmap logo = intent.getParcelableExtra(KEY_LOGO_BITMAP);
        if (logo != null) {
            getActionBar().setLogo(
                    new BitmapDrawable(getResources(), logo));
        }
    }

    public ImageView getLiveImage() {
        return mLiveImg;
    }

    private void initializeActionBar(Intent intent) {
        mUri = intent.getData();
        final ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            return;
        }
        setActionBarLogoFromIntent(intent);
        actionBar.setDisplayOptions(
                ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE,
                ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.argb(66, 0, 0, 0)));

        actionBar.addOnMenuVisibilityListener(new OnMenuVisibilityListener() {
            @Override
            public void onMenuVisibilityChanged(boolean isVisible) {
                if (mPlayer != null) {
                    if (isVisible) {
                        mPlayer.cancelHidingController();
                    } else {
                        mPlayer.restartHidingController();
                    }
                }
            }
        });
        String title = intent.getStringExtra(Intent.EXTRA_TITLE);
        if (title != null) {
            actionBar.setTitle(title);
        } else {
            // Displays the filename as title, reading the filename from the
            // interface: {@link android.provider.OpenableColumns#DISPLAY_NAME}.
            AsyncQueryHandler queryHandler =
                    new AsyncQueryHandler(getContentResolver()) {
                @Override
                protected void onQueryComplete(int token, Object cookie,
                        Cursor cursor) {
                    try {
                        if ((cursor != null) && cursor.moveToFirst()) {
                            String displayName = cursor.getString(0);

                            // Just show empty title if other apps don't set
                            // DISPLAY_NAME
                            actionBar.setTitle((displayName == null) ? "" :
                                    displayName);
                        }
                    } finally {
                        Utils.closeSilently(cursor);
                    }
                }
            };
            queryHandler.startQuery(0, null, mUri,
                    new String[] {OpenableColumns.DISPLAY_NAME}, null, null,
                    null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.movie, menu);
        MenuItem shareMenu = menu.findItem(R.id.action_share);
        shareMenu.setVisible(false);
        if (shareMenu != null) {
            shareMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (mShareIntent != null) {
                        Intent intent = Intent.createChooser(mShareIntent, null);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplicationContext().startActivity(mShareIntent);
                    }
                    return true;
                }
            });
        }

        if (isPermissionGranted()) {
            refreshShareProvider(mMovieItem);
            mMovieHooker.onCreateOptionsMenu(menu);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mMovieHooker != null) {
            mMovieHooker.onPrepareOptionsMenu(menu);
        }

//        if (mMovieItem != null
//                && !DrmHelper.isShareableDrmFile(DrmHelper.getFilePath(this,
//                        mMovieItem.getUri()))) {
//            menu.removeItem(R.id.action_share);
//        }

        return true;
    }

    private Intent createShareIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_STREAM, mMovieItem.getUri());
        return intent;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
         // If click back up button, we will always finish current activity and
         // back to previous one.
            finish();
            return true;
        } else if (id == R.id.action_share) {
            startActivity(Intent.createChooser(createShareIntent(),
                    getString(R.string.share)));
            return true;
        }
        return mMovieHooker.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        if (!isPermissionGranted()) {
            super.onStart();
            return;
        }
        mPlayer.requestAudioFocus();
        super.onStart();
        mMovieHooker.onStart();
    }

    @Override
    protected void onStop() {
        if (!isPermissionGranted()) {
            super.onStop();
            return;
        }
        mPlayer.abandonAudioFocus();
        super.onStop();
        if (mControlResumed && mPlayer != null) {
            mPlayer.onStop();
            mControlResumed = false;
        }
        mMovieHooker.onStop();
    }

    @Override
    public void onPause() {
        if (!isPermissionGranted()) {
            super.onPause();
            return;
        }
        mResumed = false;
        if (mControlResumed && mPlayer != null) {
            mControlResumed = !mPlayer.onPause();
        }
        super.onPause();
        mMovieHooker.onPause();
    }

    @Override
    public void onResume() {
        mResumed = true;
        if (isPermissionGranted()) {
            invalidateOptionsMenu();
            if (!isKeyguardLocked() && !mControlResumed && mPlayer != null) {
                mPlayer.onResume();
                mControlResumed = true;
            }
            enhanceActionBar();
            super.onResume();
            mMovieHooker.onResume();
        } else {
            super.onResume();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ||
            this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            if(mPlayer != null) {
                mPlayer.setDefaultScreenMode();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mPlayer.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        if (!isPermissionGranted()) {
            super.onDestroy();
            return;
        }
        mPlayer.onDestroy();
        super.onDestroy();
        mMovieHooker.onDestroy();
        unregisterScreenReceiver();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (LOG) {
            Log.v(TAG, "onWindowFocusChanged(" + hasFocus + ") isKeyguardLocked="
                    + isKeyguardLocked()
                    + ", mResumed=" + mResumed + ", mControlResumed=" + mControlResumed);
        }
        if (hasFocus && !isKeyguardLocked() && mResumed && !mControlResumed && mPlayer != null) {
            mPlayer.onResume();
            mControlResumed = true;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mPlayer.onKeyDown(keyCode, event)
                || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mPlayer.onKeyUp(keyCode, event)
                || super.onKeyUp(keyCode, event);
    }

    private boolean isSharable() {
        String scheme = mUri.getScheme();
        return ContentResolver.SCHEME_FILE.equals(scheme)
                || (ContentResolver.SCHEME_CONTENT.equals(scheme) && MediaStore.AUTHORITY
                        .equals(mUri.getAuthority()));
    }
    private void initMovieInfo(Intent intent) {
        Uri original = intent.getData();
        String mimeType = intent.getType();
        if (VIDEO_SDP_MIME_TYPE.equalsIgnoreCase(mimeType)
                && VIDEO_FILE_SCHEMA.equalsIgnoreCase(original.getScheme())) {
            mMovieItem = new MovieItem(VIDEO_SDP_TITLE + original, mimeType, null);
        } else {
            mMovieItem = new MovieItem(original, mimeType, null);
        }
        mMovieItem.setOriginalUri(original);
    }

    // we do not stop live streaming when other dialog overlays it.
    private BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (LOG) {
                Log.v(TAG, "onReceive(" + intent.getAction() + ") mControlResumed="
                        + mControlResumed);
            }
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                // Only stop video.
                if (mControlResumed && !mResumed) {
                    mPlayer.onStop();
                    mControlResumed = false;
                }
            } else if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                if (!mControlResumed && mResumed) {
                    mPlayer.onResume();
                    mControlResumed = true;
                }
            }
        }

    };

    private void registerScreenReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mScreenReceiver, filter);
    }

    private void unregisterScreenReceiver() {
        unregisterReceiver(mScreenReceiver);
    }

    private boolean isKeyguardLocked() {
        if (mKeyguardManager == null) {
            mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        }
        // isKeyguardSecure excludes the slide lock case.
        boolean locked = (mKeyguardManager != null)
                && mKeyguardManager.inKeyguardRestrictedInputMode();
        if (LOG) {
            Log.v(TAG, "isKeyguardLocked() locked=" + locked + ", mKeyguardManager="
                    + mKeyguardManager);
        }
        return locked;
    }

    public void refreshMovieInfo(IMovieItem info) {
        mMovieItem = info;
        setActionBarTitle(info.getTitle());
        refreshShareProvider(info);
        mMovieHooker.setParameter(null, mMovieItem);
    }

    private void refreshShareProvider(IMovieItem info) {
        // we only share the video if it's "content:".
        mShareIntent = new Intent(Intent.ACTION_SEND);
        if (MovieUtils.isLocalFile(info.getUri(), info.getMimeType())) {
            mShareIntent.setType("video/*");
            mShareIntent.putExtra(Intent.EXTRA_STREAM, info.getUri());
        } else {
            mShareIntent.setType("text/plain");
            mShareIntent.putExtra(Intent.EXTRA_TEXT, String.valueOf(info.getUri()));
        }
    }

    private void enhanceActionBar() {
        final IMovieItem movieItem = mMovieItem;// remember original item
        final Uri uri = mMovieItem.getUri();
        final String scheme = mMovieItem.getUri().getScheme();
        final String authority = mMovieItem.getUri().getAuthority();
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String title = null;
                if (ContentResolver.SCHEME_FILE.equals(scheme)) {
                    title = MovieTitleHelper.getTitleFromMediaData(MovieActivity.this, uri);
                } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
                    title = MovieTitleHelper.getTitleFromDisplayName(MovieActivity.this, uri);
                    if (title == null) {
                        title = MovieTitleHelper.getTitleFromData(MovieActivity.this, uri);
                    }
                }
                if (title == null) {
                    title = MovieTitleHelper.getTitleFromUri(uri);
                }
                if (LOG) {
                    Log.v(TAG, "enhanceActionBar() task return " + title);
                }
                return title;
            }

            @Override
            protected void onPostExecute(String result) {
                if (LOG) {
                    Log.v(TAG, "onPostExecute(" + result + ") movieItem=" + movieItem
                            + ", mMovieItem=" + mMovieItem);
                }
                movieItem.setTitle(result);
                if (movieItem == mMovieItem) {
                    setActionBarTitle(result);
                }
            };
        }.execute();
        if (LOG) {
            Log.v(TAG, "enhanceActionBar() " + mMovieItem);
        }
    }

    public void setActionBarTitle(String title) {
        if (LOG) {
            Log.v(TAG, "setActionBarTitle(" + title + ")");
        }
        ActionBar actionBar = getActionBar();
        if (title != null && actionBar != null) {
            actionBar.setTitle(title);
        }
    }
    @Override
    public void onBackPressed() {
        finishActivity();
    }
    private void finishActivity(){
        MovieActivity.this.finish();
        overridePendingTransition(0,0);
        return;
    }
}
