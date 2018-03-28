/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of The Linux Foundation nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.gallery3d.filtershow.mediapicker;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.MediaColumns;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;

import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.mediapicker.imageloader.ImageLoaderStub;
import com.android.photos.views.HeaderGridView;

import org.codeaurora.gallery.R;

/**
 * Display list of videos, photos from {@link MediaStore} and select one or many
 * item from list depends on {@link MediaOptions} that passed when open media
 * picker.
 */
public class MediaPickerFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {
    private static final String LOADER_EXTRA_URI = "loader_extra_uri";
    private static final String LOADER_EXTRA_PROJECT = "loader_extra_project";
    private static final String KEY_GRID_STATE = "grid_state";
    private static final String KEY_MEDIA_SELECTED = "media_selected";
    private HeaderGridView mGridView;

    protected ImageLoaderStub mMediaImageLoader;
    protected Context mContext;
    protected static Context appContext;
    protected FilterShowActivity mActivity;
    protected FilterRepresentation mCntFp, mOldFp;

    private MediaAdapter mMediaAdapter;

    private Bundle mSavedInstanceState;
    private Uri mMediaSelected;
    private ImageButton mSelDone, mSelCancel;

    private int mPhotoSize, mPhotoSpacing;
    private int mIndexPreset;
    private MediaPicker mMediaPicker;

    public MediaPickerFragment() {
        mSavedInstanceState = new Bundle();
    }

    public static MediaPickerFragment newInstance(Context context) {
        MediaPickerFragment fragment = new MediaPickerFragment();
        appContext = context;
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mMediaImageLoader = new ImageLoaderStub(appContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mMediaSelected = savedInstanceState
                    .getParcelable(KEY_MEDIA_SELECTED);
            mSavedInstanceState = savedInstanceState;
        }

        // get the photo size and spacing
        mPhotoSize = getResources().getDimensionPixelSize(
                R.dimen.picker_photo_size);
        mPhotoSpacing = getResources().getDimensionPixelSize(
                R.dimen.picker_photo_spacing);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mMediaPicker = (MediaPicker) inflater.inflate(
                R.layout.mediapicker_panel,
                container,
                false);
        mSelDone = (ImageButton) mMediaPicker.findViewById(R.id.btn_yes);
        mSelCancel = (ImageButton) mMediaPicker.findViewById(R.id.btn_no);
        mSelDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaSelected != null) {
                    mActivity.onMediaPickerResult(mMediaSelected);
                } else {
                    mActivity.useFilterRepresentation(mOldFp);
                    mActivity.onBackPressed();
                }
            }
        });
        mSelCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOldFp != null)
                    mActivity.useFilterRepresentation(mOldFp);
                else
                    mActivity.setDefaultPreset();
                mActivity.onBackPressed();
            }
        });

        initView(mMediaPicker);
        mMediaPicker.setupFullScreen(false, true);
        return mMediaPicker;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContext = getActivity();
        mActivity = (FilterShowActivity) mContext;
        mOldFp = mActivity.getCurrentPresentation();
        mIndexPreset = 100;
        requestPic();
    }

    public void requestPic() {
        requestMedia(Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaColumns._ID});
    }

    private void requestMedia(Uri uri, String[] projects) {
        Bundle bundle = new Bundle();
        bundle.putStringArray(LOADER_EXTRA_PROJECT, projects);
        bundle.putString(LOADER_EXTRA_URI, uri.toString());
        getLoaderManager().initLoader(0, bundle, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mGridView != null) {
            mSavedInstanceState.putParcelable(KEY_GRID_STATE,
                    mGridView.onSaveInstanceState());
        }
        mSavedInstanceState.putParcelable(KEY_MEDIA_SELECTED,
                mMediaSelected);
        outState.putAll(mSavedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        Uri uri = Uri.parse(bundle.getString(LOADER_EXTRA_URI));
        String[] projects = bundle.getStringArray(LOADER_EXTRA_PROJECT);
        String order = MediaColumns.DATE_ADDED + " DESC";
        return new CursorLoader(mContext, uri, projects, null, null, order);
    }

    private void attachData(Cursor cursor) {
        if (cursor == null || cursor.getCount() <= 0) {
            switchToError();
            return;
        }
        mGridView.setVisibility(View.VISIBLE);
        if (mMediaSelected != null) {
            mMediaSelected = null;
            mMediaAdapter.setMediaSelected(mMediaSelected);
        }
        if (mMediaAdapter == null) {
            mMediaAdapter = new MediaAdapter(mContext, cursor, 0,
                     mMediaImageLoader);
        } else {
            mMediaAdapter.swapCursor(cursor);
        }
        if (mGridView.getAdapter() == null) {
            mGridView.setAdapter(mMediaAdapter);
            mGridView.setRecyclerListener(mMediaAdapter);
        }
        Parcelable state = mSavedInstanceState.getParcelable(KEY_GRID_STATE);
        if (state != null) {
            mGridView.onRestoreInstanceState(state);
        }
        mMediaAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        attachData(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Preference:http://developer.android.com/guide/components/loaders.html#callback
        if (mMediaAdapter != null)
            mMediaAdapter.swapCursor(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        Object object = parent.getAdapter().getItem(position);
        if (object instanceof Cursor) {
            Uri uri;
            uri = getPicUri((Cursor) object);

            if (!uri.equals(mMediaSelected)) {
                mIndexPreset++;
                mCntFp = mActivity.createUserPresentaion(uri, mIndexPreset);
                mActivity.applyCustomFilterRepresentation(mCntFp, mOldFp);
            }

            SelectedImageView selImageView = (SelectedImageView) view
                    .findViewById(R.id.thumbnail);
            mMediaSelected = uri;
            mMediaAdapter.updateMediaSelected(uri, selImageView);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mGridView != null) {
            mSavedInstanceState.putParcelable(KEY_GRID_STATE,
                    mGridView.onSaveInstanceState());
            mGridView = null;
        }
        if (mMediaAdapter != null) {
            mMediaAdapter.onDestroyView();
        }
    }


    private Uri getPicUri(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndex(MediaColumns._ID));
        return Uri.withAppendedPath(Images.Media.EXTERNAL_CONTENT_URI, id);
    }

    private void switchToError() {
        mGridView.setVisibility(View.GONE);
        mSelDone.setVisibility(View.GONE);
        mSelCancel.setVisibility(View.GONE);
    }

    private void initView(MediaPicker view) {
        mGridView = (HeaderGridView) view.findViewById(R.id.grid);
        mGridView.setOnItemClickListener(this);

        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mMediaAdapter != null
                                && mMediaAdapter.getNumColumns() == 0) {
                            final int numColumns = (int) Math.floor(mGridView
                                    .getWidth() / (mPhotoSize + mPhotoSpacing));
                            if (numColumns > 0) {
                                final int columnWidth = (mGridView.getWidth() / numColumns)
                                        - mPhotoSpacing;
                                mMediaAdapter.setNumColumns(numColumns);
                                mMediaAdapter.setItemHeight(columnWidth);
                            }
                        }
                    }
                });
    }

}
