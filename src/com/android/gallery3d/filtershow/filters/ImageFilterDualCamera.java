/*
 * Copyright (c) 2015-2017, The Linux Foundation. All rights reserved.
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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import org.codeaurora.gallery.R;
import com.android.gallery3d.filtershow.cache.BitmapCache;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils.GeometryHolder;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.FilterEnvironment;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import com.android.gallery3d.filtershow.tools.DualCameraEffect;

class ImageFilterDualCamera extends ImageFilter {
    private static final String TAG = ImageFilterDualCamera.class.getSimpleName();

    private FilterDualCamBasicRepresentation mParameters;
    private Paint mPaint = new Paint();

    public FilterRepresentation getDefaultRepresentation() {
        return null;
    }

    public void useRepresentation(FilterRepresentation representation) {
        mParameters = (FilterDualCamBasicRepresentation) representation;
    }

    boolean mSupportFusion = false;

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (mParameters == null) {
            return bitmap;
        }

        Point point = new Point(mParameters.getPoint());
        if (!point.equals(-1, -1)) {
            MasterImage image = MasterImage.getImage();

            Size size = getFilteredSize(image, quality);
            DualCameraEffect effect = image.getDualCameraEffect(size.getWidth(), size.getHeight());
            if (effect == null) {
                return bitmap;
            }
            size = effect.size();

            Bitmap filteredBitmap = image.getBitmapCache().getBitmap(size.getWidth(),
                    size.getHeight(), BitmapCache.FILTERS);
            Log.d(TAG, "filtered: " + size.getWidth() + "x" + size.getHeight());
            filteredBitmap.setHasAlpha(mSupportFusion);

            float intensity = getIntensity();
            effect.map(point);
            int effectType = getEffectType();
            boolean result = effect.render(effectType, point.x, point.y, filteredBitmap, intensity);
            if (!result) {
                showToast(R.string.dualcam_no_segment_toast, Toast.LENGTH_SHORT);
                return bitmap;
            }

            mPaint.reset();
            mPaint.setAntiAlias(true);
            if (quality == FilterEnvironment.QUALITY_FINAL) {
                mPaint.setFilterBitmap(true);
                mPaint.setDither(true);
            }

            Canvas canvas = new Canvas(bitmap);
            ImagePreset preset = getEnvironment().getImagePreset();
            int bmWidth = bitmap.getWidth();
            int bmHeight = bitmap.getHeight();
            GeometryHolder holder;
            if (preset.getDoApplyGeometry()) {
                holder = GeometryMathUtils.unpackGeometry(preset.getGeometryFilters());
            } else {
                holder = new GeometryHolder();
            }

            RectF roiRectF = new RectF();
            roiRectF.left = 0;
            roiRectF.top = 0;
            roiRectF.right = 1;
            roiRectF.bottom = 1;

            int zoomOrientation = image.getZoomOrientation();
            if (isRotated(zoomOrientation)) {
                Matrix mt = new Matrix();
                mt.preRotate(GeometryMathUtils.getRotationForOrientation(zoomOrientation),
                        0.5f, 0.5f);
                mt.mapRect(roiRectF);
            }

            // Check for ROI cropping
            if (!FilterCropRepresentation.getNil().equals(roiRectF)) {
                if (FilterCropRepresentation.getNil().equals(holder.crop)) {
                    // no crop filter, set crop to be roiRect
                    holder.crop.set(roiRectF);
                } else if (!roiRectF.contains(holder.crop)) {
                    // take smaller intersecting area between roiRect and crop rect
                    holder.crop.left = Math.max(holder.crop.left, roiRectF.left);
                    holder.crop.top = Math.max(holder.crop.top, roiRectF.top);
                    holder.crop.right = Math.min(holder.crop.right, roiRectF.right);
                    holder.crop.bottom = Math.min(holder.crop.bottom, roiRectF.bottom);
                }
            }

            RectF crop = new RectF();
            Matrix m = GeometryMathUtils.getOriginalToScreen(holder, crop, true,
                    size.getWidth(), size.getHeight(), bmWidth, bmHeight);

            if (mSupportFusion) canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            canvas.save();
            canvas.clipRect(crop);
            canvas.drawBitmap(filteredBitmap, m, mPaint);
            canvas.restore();

            image.getBitmapCache().cache(filteredBitmap);
        }
        return bitmap;
    }

    private int getEffectType() {
        switch (mParameters.getTextId()) {
            case R.string.focus: return DualCameraEffect.REFOCUS_CIRCLE;
            case R.string.halo: return DualCameraEffect.HALO;
            case R.string.motion: return DualCameraEffect.MOTION_BLUR;
            case R.string.posterize: return DualCameraEffect.POSTERIZE;
            case R.string.sketch: return DualCameraEffect.SKETCH;
            case R.string.zoom: return DualCameraEffect.ZOOM_BLUR;
            case R.string.bw: return DualCameraEffect.BLACK_WHITE;
            case R.string.blackboard: return DualCameraEffect.BLACKBOARD;
            case R.string.whiteboard: return DualCameraEffect.WHITEBOARD;
            case R.string.fusion: return DualCameraEffect.FUSION_FOREGROUND;
            case R.string.dc_negative: return DualCameraEffect.NEGATIVE;
            default: throw new IllegalArgumentException();
        }
    }

    private Size getFilteredSize(MasterImage image, int quality) {
        int width, height;
        if (quality == FilterEnvironment.QUALITY_FINAL) {
            Rect originalBounds = image.getOriginalBounds();
            width = originalBounds.width();
            height = originalBounds.height();
        } else {

            Bitmap originalBmp = image.getOriginalBitmapHighres();
            width = originalBmp.getWidth();
            height = originalBmp.getHeight();

            // image is rotated
            int orientation = image.getOrientation();
            if (isRotated(orientation)) {
                int tmp = width;
                width = height;
                height = tmp;
            }

            // non even width or height
            if (width % 2 != 0 || height % 2 != 0) {
                float aspect = (float) height / (float) width;
                if (width >= height) {
                    width = MasterImage.MAX_BITMAP_DIM;
                    height = (int) (width * aspect);
                } else {
                    height = MasterImage.MAX_BITMAP_DIM;
                    width = (int) (height / aspect);
                }
            }
        }
        return new Size(width, height);
    }

    private boolean isRotated(int orientation) {
        return orientation == ImageLoader.ORI_ROTATE_90 ||
                orientation == ImageLoader.ORI_ROTATE_270 ||
                orientation == ImageLoader.ORI_TRANSPOSE ||
                orientation == ImageLoader.ORI_TRANSVERSE;
    }

    private float getIntensity() {
        float value = (float) mParameters.getValue();
        float max = (float) mParameters.getMaximum();
        return max != 0 ? value / max : 0;
    }
}