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

package com.android.gallery3d.filtershow.tools;

import java.util.Stack;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import org.codeaurora.gallery.R;
import com.android.gallery3d.filtershow.filters.FilterDrawRepresentation.StrokeData;
import com.android.gallery3d.filtershow.ui.AlertMsgDialog;

public class TruePortraitNativeEngine {
    private static final String TAG = "TruePortraitNativeEngine";
    static {
        try {
            System.loadLibrary("jni_trueportrait");
            mLibLoaded = true;
            Log.v(TAG, "successfully loaded true portrait lib");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "failed to load true portrait lib");
            mLibLoaded = false;
        }
    }

    public static enum EffectType {
        BLUR,
        MOTION_BLUR,
        HALO,
        SKETCH
    };

    private static final int MASK_UPDATE_FOREGROUND = 0x7F000000;
    private static final int MASK_UPDATE_BACKGROUND = 0x81000000;

    private static boolean mLibLoaded;
    private static TruePortraitNativeEngine mInstance;

    private Bitmap mSketchBm;
    private boolean mFacesDetected;
    private Point mPreviewSize = new Point();
    private Bitmap mUpdateBm;

    private TruePortraitNativeEngine() {}

    public static void createInstance() {
        if(mInstance == null) {
            mInstance = new TruePortraitNativeEngine();
        }
    }

    public static TruePortraitNativeEngine getInstance() {
        createInstance();
        return mInstance;
    }

    public boolean isLibLoaded() {
        return mLibLoaded;
    }

    public boolean init(Context context, Bitmap src, Rect[] faces) {
        boolean result = false; 
        if(nativeInit(src, faces.length, faces)) {
            result = nativeGetPreviewSize(mPreviewSize);
        }
        if (result) {
            mSketchBm = BitmapFactory.decodeResource(context.getResources(), R.raw.sketch_bm);
        } else {
            mSketchBm = null;
        }
        setFacesDetected(result);   
        return result;
    }

    public void release() {
        if(mUpdateBm != null) {
            mUpdateBm.recycle();
            mUpdateBm = null;
        }

        if(mSketchBm != null) {
            mSketchBm.recycle();
            mSketchBm = null;
        }

        nativeRelease();
    }

    public void setFacesDetected(boolean detected) {
        mFacesDetected = detected;
    }

    public boolean facesDetected() {
        return mFacesDetected;
    }

    public void showNoFaceDetectedDialog(FragmentManager fragmentManager) {
        AlertMsgDialog dialog = new AlertMsgDialog(R.string.trueportrait, R.string.trueportrait_no_face);
        dialog.show(fragmentManager, "tp_no_face");
    }

    public boolean applyEffect(EffectType type, int intensity, Bitmap outBm) {
        boolean result = false;
        switch(type) {
        case BLUR:
            result = nativeApplyBlur(intensity, outBm);
            break;
        case MOTION_BLUR:
            result = nativeApplyMotionBlur(intensity, outBm);
            break;
        case HALO:
            result = nativeApplyHalo(intensity, outBm);
            break;
        case SKETCH:
            result = nativeApplySketch(outBm, mSketchBm);
            break;
        }

        return result;
    }

    public Bitmap getMask() {
        Bitmap mask = Bitmap.createBitmap(mPreviewSize.x, mPreviewSize.y, Config.ALPHA_8);
        if(mask != null) {
            nativeGetPreviewMask(mask);
        }

        return mask;
    }

    public Bitmap getPreview() {
        Bitmap previewBm = Bitmap.createBitmap(mPreviewSize.x, mPreviewSize.y, Config.ARGB_8888);
        if(previewBm != null) {
            nativeGetPreviewImage(previewBm);
        }

        return previewBm;
    }

    public boolean updateMask(Stack<StrokeData> edits) {
        if(mUpdateBm == null) {
            mUpdateBm = Bitmap.createBitmap(mPreviewSize.x, mPreviewSize.y, Config.ALPHA_8);
        }

        mUpdateBm.eraseColor(Color.TRANSPARENT);
        Canvas canvas = new Canvas(mUpdateBm);
        for(StrokeData sd:edits) {
            drawEdit(canvas, sd);
        }
        return nativeUpdatePreviewMask(mUpdateBm);
    }

    private void drawEdit(Canvas canvas, StrokeData sd) {
        if (sd == null) {
            return;
        }
        if (sd.mPath == null) {
            return;
        }
        //  USER_INPUT_MASK_UNCHANGED_VALUE   = 0
        //  USER_INPUT_MASK_BACKGROUND_VALUE  = 129
        //  USER_INPUT_MASK_FOREGROUND_VALUE  = 127
        Paint paint = new Paint();
        paint.setAntiAlias(false);
        paint.setStyle(Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        if(sd.mColor == Color.TRANSPARENT)
            paint.setColor(MASK_UPDATE_BACKGROUND);
        else
            paint.setColor(MASK_UPDATE_FOREGROUND);
        paint.setStrokeWidth(sd.mRadius);

        canvas.drawPath(sd.mPath, paint);
    }

    native public boolean nativeInit(Bitmap srcRGBA, int numFaces, Rect[] faces);

    native public void nativeRelease();

    native public boolean nativeGetPreviewSize(Point point);

    native public boolean nativeGetPreviewImage(Bitmap outBm);

    native public boolean nativeGetPreviewMask(Bitmap outBm);

    native public boolean nativeUpdatePreviewMask(Bitmap mask);

    native public boolean nativeApplyBlur(int intensity, Bitmap outBm);

    native public boolean nativeApplyMotionBlur(int intensity, Bitmap outBm);

    native public boolean nativeApplyHalo(int intensity, Bitmap outBm);

    native public boolean nativeApplySketch(Bitmap outBm, Bitmap sketchBm);

    native public boolean nativeGetForegroundImg(Bitmap outBm);
}
