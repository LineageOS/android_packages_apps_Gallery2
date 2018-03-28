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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.cursoradapter.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView.RecyclerListener;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import com.android.gallery3d.filtershow.mediapicker.imageloader.ImageLoaderStub;
import org.codeaurora.gallery.R;

/**
 * Adapter for display media item list.
 */
public class MediaAdapter extends CursorAdapter implements RecyclerListener {
    private ImageLoaderStub mMediaImageLoader;
    private Uri mMediaSelected;
    private int mItemHeight = 0;
    private int mNumColumns = 0;
    private RelativeLayout.LayoutParams mImageViewLayoutParams;
    private List<SelectedImageView> mImageViewSelected = new ArrayList<SelectedImageView>();

    public MediaAdapter(Context context, Cursor c, int flags,
                        ImageLoaderStub mediaImageLoader) {
        this(context, c, flags, null, mediaImageLoader);
    }

    public MediaAdapter(Context context, Cursor c, int flags,
                        Uri mediaSelected, ImageLoaderStub mediaImageLoader) {
        super(context, c, flags);
        mMediaSelected = mediaSelected;
        mMediaImageLoader = mediaImageLoader;
        mImageViewLayoutParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final SelectedImageView imageView = (SelectedImageView) view.getTag();
        final Uri uri;
        uri = getPhotoUri(cursor);

        boolean isSelected = isSelected(uri);
        imageView.setSelected(isSelected);
        if (isSelected) {
            mImageViewSelected.add(imageView);
        }
        mMediaImageLoader.displayImage(uri, imageView);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View root = View
                .inflate(context, R.layout.mediapicker_list_item, null);
        SelectedImageView imageView = (SelectedImageView) root.findViewById(R.id.thumbnail);

        imageView.setLayoutParams(mImageViewLayoutParams);
        // Check the height matches our calculated column width
        if (imageView.getLayoutParams().height != mItemHeight) {
            imageView.setLayoutParams(mImageViewLayoutParams);
        }
        root.setTag(imageView);
        return root;
    }

    public Uri getPhotoUri(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
        return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
    }

    /**
     * Check media uri is selected or not.
     *
     * @param uri Uri of media item (photo, video)
     * @return true if selected, false otherwise.
     */
    public boolean isSelected(Uri uri) {
        if (uri == null)
            return false;
        if (mMediaSelected != null) {
            if (mMediaSelected.equals(uri))
                return true;
        }
        return false;
    }

    /**
     * If item selected then change to unselected and unselected to selected.
     *
     * @param item Item to update.
     */
    public void updateMediaSelected(Uri item,
                                    SelectedImageView selImageView) {
        if (mMediaSelected == null || !mMediaSelected.equals(item)) {
            for (SelectedImageView picker : this.mImageViewSelected) {
                picker.setSelected(false);
            }
            this.mImageViewSelected.clear();

            mMediaSelected = item;
            selImageView.setSelected(true);
            this.mImageViewSelected.add(selImageView);
        }
    }

    public void setMediaSelected(Uri item) {
        mMediaSelected = item;
    }

    // set numcols
    public void setNumColumns(int numColumns) {
        mNumColumns = numColumns;
    }

    public int getNumColumns() {
        return mNumColumns;
    }

    // set photo item height
    public void setItemHeight(int height) {
        if (height == mItemHeight) {
            return;
        }
        mItemHeight = height;
        mImageViewLayoutParams.height = height;
        mImageViewLayoutParams.width = height;
        notifyDataSetChanged();
    }

    @Override
    public void onMovedToScrapHeap(View view) {
        SelectedImageView imageView = (SelectedImageView) view
                .findViewById(R.id.thumbnail);
        mImageViewSelected.remove(imageView);
    }

    public void onDestroyView() {
        mImageViewSelected.clear();
    }
}
