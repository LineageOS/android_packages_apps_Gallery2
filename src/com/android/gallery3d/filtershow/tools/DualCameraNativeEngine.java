/*
 * Copyright (c) 2015-2016, The Linux Foundation. All rights reserved.
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

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.Log;

public class DualCameraNativeEngine {
    private static final String TAG = "DualCameraNativeEngine";

    public static final float DEFAULT_BRIGHTNESS_INTENSITY = 1.0f;

    static {
        try {
            System.loadLibrary("jni_dualcamera");
            mLibLoaded = true;
            Log.v(TAG, "successfully loaded dual camera lib");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "failed to load dual camera lib");
            mLibLoaded = false;
        }
    }

    // Status of Depth Map loading for current image
    public static enum DdmStatus {
        DDM_IDLE,
        DDM_PARSING,
        DDM_LOADING,
        DDM_LOADED,
        DDM_FAILED
    }

    public static final String DEPTH_MAP_EXT = "dm";
    private static final String CALIBRATION_FILENAME = "ddm_calib_file.dat";
    private static boolean mLibLoaded;

    private static DualCameraNativeEngine mInstance;

    private DualCameraNativeEngine() {
    }

    public static void createInstance() {
        if (mInstance == null) {
            mInstance = new DualCameraNativeEngine();
        }
    }

    public static DualCameraNativeEngine getInstance() {
        createInstance();
        return mInstance;
    }

    public boolean isLibLoaded() {
        return mLibLoaded;
    }

    public String getCalibFilepath(Context context) {
        File calibFile = new File(context.getFilesDir(), CALIBRATION_FILENAME);
        return calibFile.getAbsolutePath();
    }

    native public boolean initDepthMap(Bitmap primaryRGBA, Bitmap auxiliaryRGBA, String mpoFilepath, String calibFilepath, float brIntensity);

    native public void releaseDepthMap();

    native public boolean getDepthMapSize(Point point);

    native public boolean getDepthMap(Bitmap dataBuffer);

    native private boolean get3DEffectImages(Bitmap bitmap, Bitmap depthMap);

    native public boolean applyFocus(int focusPointX, int focusPointY, float intensity, int[] roiRect, boolean isPreview, Bitmap outBm);

    native public boolean applyFocusStar(int focusPointX, int focusPointY, float intensity, int[] roiRect, boolean isPreview, Bitmap outBm);

    native public boolean applyFocusHexagon(int focusPointX, int focusPointY, float intensity, int[] roiRect, boolean isPreview, Bitmap outBm);

    native public boolean applyHalo(int focusPointX, int focusPointY, float intensity, int[] roiRect, boolean isPreview, Bitmap outBm);

    native public boolean applyMotion(int focusPointX, int focusPointY, float intensity, int[] roiRect, boolean isPreview, Bitmap outBm);

    native public boolean applySketch(int focusPointX, int focusPointY, int[] roiRect, boolean isPreview, Bitmap outBm);

    native public boolean applyZoom(int focusPointX, int focusPointY, int[] roiRect, boolean isPreview, Bitmap outBm);

    native public boolean applyBlackAndWhite(int focusPointX, int focusPointY, int[] roiRect, boolean isPreview, Bitmap outBm);

    native public boolean applyBlackBoard(int focusPointX, int focusPointY, int[] roiRect, boolean isPreview, Bitmap outBm);

    native public boolean applyWhiteBoard(int focusPointX, int focusPointY, int[] roiRect, boolean isPreview, Bitmap outBm);

    native public boolean applyPosterize(int focusPointX, int focusPointY, float intensity, int[] roiRect, boolean isPreview, Bitmap outBm);

    native public boolean applyNegative(int focusPointX, int focusPointY, int[] roiRect, boolean isPreview, Bitmap outBm);

    native public boolean getForegroundImg(int focusPointX, int focusPointY, int[] roiRect, boolean isPreview, Bitmap outBm);

    native public boolean getPrimaryImg(int focusPointX, int focusPointY, int[] roiRect, boolean isPreview, Bitmap outBm);

    public static class DepthMap3D {
        public char[] pixels;
        public int width;
        public int height;
    }

    public DepthMap3D getDepthMap3D(Bitmap bitmap) {
        int width = bitmap.getWidth(), height = bitmap.getHeight();
        Bitmap depthMap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
        boolean ok = get3DEffectImages(bitmap, depthMap);
        if (!ok) return null;

        DepthMap3D map = new DepthMap3D();
        map.width = width;
        map.height = height;
        map.pixels = new char[width * height];

        int[] pixels = new int[width * height];
        depthMap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = width * height - 1; i >= 0; --i) {
            map.pixels[i] = (char) (pixels[i] & 0xff);
        }
        depthMap.recycle();
        return map;
    }
}
