/*
 * Copyright (c) 2016-2017, The Linux Foundation. All rights reserved.
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

package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.util.AttributeSet;

import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.editors.EditorTruePortraitFusion;
import com.android.gallery3d.filtershow.filters.FilterTruePortraitFusionRepresentation;

public class ImageTruePortraitFusion extends ImageShow {
    private static final String LOGTAG = "ImageTruePortraitFusion";
    protected EditorTruePortraitFusion mEditor;
    protected FilterTruePortraitFusionRepresentation mRepresentation;
    private Bitmap mUnderlay;

    public ImageTruePortraitFusion(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAllowScaleAndTranslate = true;
    }

    public ImageTruePortraitFusion(Context context) {
        super(context);
        mAllowScaleAndTranslate = true;
    }

    public void setUnderlay(Uri uri) {
        mRepresentation.setUnderlay(uri);

        if(mUnderlay != null) {
            mUnderlay.recycle();
            mUnderlay = null;
            MasterImage.getImage().setFusionUnderlay(null);
        }

        mUnderlay = ImageLoader.loadConstrainedBitmap(uri, getContext(), MasterImage.MAX_BITMAP_DIM, new Rect(), false);
        int ori = ImageLoader.getMetadataOrientation(getContext(), uri);
        if (ori != ImageLoader.ORI_NORMAL) {
            mUnderlay = ImageLoader.orientBitmap(mUnderlay, ori);
        }
        MasterImage.getImage().setFusionUnderlay(mUnderlay);
        invalidate();
    }

    public void setEditor(EditorTruePortraitFusion editor) {
        mEditor = editor;
    }

    public void setRepresentation(FilterTruePortraitFusionRepresentation representation) {
        mRepresentation = representation;
    }

    @Override
    public boolean enableComparison() {
        return false;
    }
}
