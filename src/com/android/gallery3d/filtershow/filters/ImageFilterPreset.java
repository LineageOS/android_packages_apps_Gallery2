/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of The Linux Foundation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
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


package com.android.gallery3d.filtershow.filters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import com.android.gallery3d.app.Log;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.tools.FilterGeneratorNativeEngine;

public class ImageFilterPreset extends ImageFilter {

    private static final String LOGTAG = "ImageFilterPreset";
    private FilterPresetRepresentation mParameters = null;
    private Bitmap mPresetBitmap = null;
    private Resources mResources = null;
    private int mPresetBitmapId = 0;
    private Uri filterImageUri;

    public ImageFilterPreset() {    }

    @Override

    public void freeResources() {
        if (mPresetBitmap != null) mPresetBitmap.recycle();
        mPresetBitmap = null;
        }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        return null;
    }

    public void useRepresentation(FilterRepresentation representation) {
        FilterPresetRepresentation parameters = (FilterPresetRepresentation) representation;
        mParameters = parameters;
    }

    public FilterPresetRepresentation getParameters() {
        return mParameters;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (getParameters() == null || mResources == null) {
            return bitmap;
        }

        int bitmapResourceId = getParameters().getBitmapResource();
        if (bitmapResourceId == 0) { // null filter preset
            return bitmap;
        }

        int highresPreviewSize = Math.min(MasterImage.MAX_BITMAP_DIM, MasterImage.getImage().getActivity().getScreenImageSize());

        filterImageUri = getParameters().getUri();
        Bitmap filter = ImageLoader.loadOrientedConstrainedBitmap(filterImageUri,
                MasterImage.getImage().getActivity(), highresPreviewSize,
                MasterImage.getImage().getOrientation(), new Rect());

        FilterGeneratorNativeEngine.getInstance().filterGeneratorProcess(bitmap,filter,bitmap);
        return bitmap;
    }

    public void setResources(Resources resources) {
        mResources = resources;
    }
}