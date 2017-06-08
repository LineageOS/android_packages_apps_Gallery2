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

package com.android.gallery3d.filtershow.tools;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.Log;
import android.util.Size;

public class DualCameraEffect {
    private static final String TAG = "DualCameraEffect";

    // following constants should be consistent with jni code
    public static final int INPUT = 0;
    public static final int REFOCUS_CIRCLE = 1;
    public static final int REFOCUS_HEXAGON = 2;
    public static final int REFOCUS_STAR = 3;
    public static final int SKETCH = 4;
    public static final int HALO = 5;
    public static final int MOTION_BLUR = 6;
    public static final int FUSION_FOREGROUND = 7;
    public static final int ZOOM_BLUR = 8;
    public static final int BLACK_WHITE = 9;
    public static final int WHITEBOARD = 10;
    public static final int BLACKBOARD = 11;
    public static final int POSTERIZE = 12;
    public static final int NEGATIVE = 13;

    public static final float DEFAULT_BRIGHTNESS_INTENSITY = 1.0f;

    static {
        try {
            System.loadLibrary("jni_dualcamera");
            loaded = true;
            Log.v(TAG, "successfully loaded dual camera lib");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "failed to load dual camera lib");
            loaded = false;
        }
    }

    private static boolean loaded;
    private static DualCameraEffect instance;

    private int[] mRoi;
    private int mWidth;
    private int mHeight;

    private DualCameraEffect() {}

    public static boolean isSupported() {
        return loaded;
    }

    public static synchronized DualCameraEffect getInstance() {
        if (instance == null) {
            instance = new DualCameraEffect();
        }
        return instance;
    }

    public boolean initialize(Bitmap primary, Bitmap depthMap, int[] roi, int width, int height,
            float brIntensity) {
        boolean ok = init(primary, depthMap, roi, width, height, brIntensity);
        if (ok) {
            mRoi = roi;
            mWidth = width;
            mHeight = height;
        }
        return ok;
    }

    public void map(Point point) {
        point.x = (point.x - mRoi[0]) * mWidth / mRoi[2];
        point.y = (point.y - mRoi[1]) * mHeight / mRoi[3];
    }

    public Size size() {
        return new Size(mWidth, mHeight);
    }

    native private boolean init(Bitmap primary, Bitmap depthMap, int[] roi, int width, int height,
            float brIntensity);

    native public void release();

    native public boolean render(int effect, int focusPointX, int focusPointY, Bitmap out,
            float intensity);

    public boolean render(int effect, int focusPointX, int focusPointY, Bitmap out) {
        return render(effect, focusPointX, focusPointY, out, 0);
    }
}