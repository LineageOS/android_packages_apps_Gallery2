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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.widget.Toast;

import org.codeaurora.gallery.R;

import com.android.gallery3d.app.GalleryAppImpl;
import com.android.gallery3d.filtershow.cache.BitmapCache;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils.GeometryHolder;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.FilterEnvironment;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import com.android.gallery3d.filtershow.tools.TruePortraitNativeEngine;
import com.android.gallery3d.filtershow.tools.TruePortraitNativeEngine.EffectType;

public class ImageFilterTruePortrait extends ImageFilter {
    private static final String TAG = "ImageFilterTruePortrait";

    private FilterRepresentation mParameters;
    private Paint mPaint = new Paint();

    public void useRepresentation(FilterRepresentation representation) {
        mParameters = representation;
    }

    public FilterRepresentation getParameters() {
        return mParameters;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (getParameters() == null || quality == FilterEnvironment.QUALITY_ICON) {
            return bitmap;
        }

        Bitmap filteredBitmap = null;
        boolean result = false;
        int orientation = MasterImage.getImage().getOrientation();
        Rect originalBounds = MasterImage.getImage().getOriginalBounds();
        int filteredW;
        int filteredH;

        if(quality == FilterEnvironment.QUALITY_FINAL) {
            filteredW = originalBounds.width();
            filteredH = originalBounds.height();
        } else {
            Bitmap originalBmp = MasterImage.getImage().getOriginalBitmapHighres();
            filteredW = originalBmp.getWidth();
            filteredH = originalBmp.getHeight();

            // image is rotated
            if (orientation == ImageLoader.ORI_ROTATE_90 ||
                    orientation == ImageLoader.ORI_ROTATE_270 ||
                    orientation == ImageLoader.ORI_TRANSPOSE ||
                    orientation == ImageLoader.ORI_TRANSVERSE) {
                int tmp = filteredW;
                filteredW = filteredH;
                filteredH = tmp;
            }

            // non even width or height
            if(filteredW%2 != 0 || filteredH%2 != 0) {
                float aspect = (float)filteredH / (float)filteredW;
                if(filteredW >= filteredH) {
                    filteredW = MasterImage.MAX_BITMAP_DIM;
                    filteredH = (int)(filteredW * aspect);
                } else {
                    filteredH = MasterImage.MAX_BITMAP_DIM;
                    filteredW = (int)(filteredH / aspect);
                }
            }
        }

        filteredBitmap = MasterImage.getImage().getBitmapCache().getBitmap(filteredW, filteredH, BitmapCache.FILTERS);

        result = applyEffect(filteredBitmap);

        if(result == false) {
            Log.e(TAG, "Imagelib API failed");
            showToast(GalleryAppImpl.getContext().getString(R.string.no_faces_found),
                    Toast.LENGTH_SHORT);
            return bitmap;
        } else {

            mPaint.reset();
            mPaint.setAntiAlias(true);
            if(quality == FilterEnvironment.QUALITY_FINAL) {
                mPaint.setFilterBitmap(true);
                mPaint.setDither(true);
            }

            if(needsClear()) {
                bitmap.setHasAlpha(true);
                bitmap.eraseColor(Color.TRANSPARENT);
            }

            Canvas canvas = new Canvas(bitmap);
            ImagePreset preset = getEnvironment().getImagePreset();
            int bmWidth = bitmap.getWidth();
            int bmHeight = bitmap.getHeight();
            GeometryHolder holder;
            if(preset.getDoApplyGeometry()) {
                holder = GeometryMathUtils.unpackGeometry(preset.getGeometryFilters());
            } else {
                holder = new GeometryHolder();
            }

            RectF crop = new RectF();
            Matrix m = GeometryMathUtils.getOriginalToScreen(holder, crop, true,
                    filteredW, filteredH, bmWidth, bmHeight);

            canvas.save();
            canvas.clipRect(crop);
            canvas.drawBitmap(filteredBitmap, m, mPaint);
            canvas.restore();

            MasterImage.getImage().getBitmapCache().cache(filteredBitmap);
        }

        return bitmap;
    }

    protected boolean applyEffect(Bitmap filteredBitmap) {
        FilterBasicRepresentation basicRep = (FilterBasicRepresentation) getParameters();
        int value = basicRep.getValue();

        boolean result = false;
        switch(mParameters.getTextId()) {
        case R.string.blur:
            result = TruePortraitNativeEngine.getInstance().applyEffect(EffectType.BLUR, value, filteredBitmap);
            break;
        case R.string.motion_blur:
            result = TruePortraitNativeEngine.getInstance().applyEffect(EffectType.MOTION_BLUR, value, filteredBitmap);
            break;
        case R.string.halo:
            result = TruePortraitNativeEngine.getInstance().applyEffect(EffectType.HALO, value, filteredBitmap);
            break;
        case R.string.sketch:
            result = TruePortraitNativeEngine.getInstance().applyEffect(EffectType.SKETCH, value, filteredBitmap);
            break;
        }

        return result;
    }

    protected boolean needsClear() {
        return false;
    }
}
