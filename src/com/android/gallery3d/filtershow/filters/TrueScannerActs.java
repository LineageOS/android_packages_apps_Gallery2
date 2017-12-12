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

package com.android.gallery3d.filtershow.filters;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import org.codeaurora.gallery.R;
import com.android.gallery3d.app.GalleryActivity;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.controller.Parameter;
import com.android.gallery3d.filtershow.editors.TrueScannerEditor;
import com.android.gallery3d.filtershow.imageshow.ImageTrueScanner;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.ui.MenuExecutor;
import static com.android.gallery3d.filtershow.imageshow.ImageTrueScanner.*;

public class TrueScannerActs extends SimpleImageFilter {
    public static final String SERIALIZATION_NAME = "TrueScannerActs";
    //The minimum resolution that TrueScanner library supports is VGA, i.e. 640x480.
    public static final int MIN_WIDTH = 640;
    public static final int MIN_HEIGHT = 480;
    private static boolean rotating = false;
    private static final boolean DEBUG = false;
    private static boolean isTrueScannerEnabled = true;
    private static boolean isPointsAcquired;
    private final static int POINTS_LEN = 8;
    private static int[] mAcquiredPoints = new int[POINTS_LEN+2];
    protected boolean isWhiteBoard = false;
    private boolean mLocked = false;
    private Bitmap rectifiedImage = null;
    private int[] oldPts = new int[POINTS_LEN];
    private ProgressDialog mProgressDialog;

    private void printDebug(String str) {
        if(DEBUG)
            android.util.Log.d("TrueScanner", str);
    }

    public static boolean isTrueScannerEnabled() {
        return isTrueScannerEnabled;
    }

    public static boolean setRotating(boolean isRotating) {
        return rotating = isRotating;
    }

    public TrueScannerActs() {
        mName = "TrueScannerActs";
        isPointsAcquired = false;
    }

    public FilterRepresentation getDefaultRepresentation() {
        FilterBasicRepresentation representation = (FilterBasicRepresentation) super.getDefaultRepresentation();
        representation.setName("TrueScanner");
        representation.setSerializationName(SERIALIZATION_NAME);
        representation.setFilterClass(TrueScannerActs.class);
        representation.setTextId(R.string.truescanner_normal);
        representation.setMinimum(0);
        representation.setMaximum(10);
        representation.setValue(0);
        representation.setDefaultValue(0);
        representation.setSupportsPartialRendering(false);
        representation.setEditorId(TrueScannerEditor.ID);

        return representation;
    }

    private native int[] processImage(Bitmap orgBitmap, Bitmap rectifiedBitmap, int[] cornerPts);
    private native int enhanceImage(Bitmap orgBitmap, Bitmap rectifiedBitmap);

    private synchronized boolean acquireLock(boolean isAcquiring) {
        if(mLocked != isAcquiring) {
            mLocked = isAcquiring;
            return true;
        }
        return false;
    }

    private void showProgressDialog() {
        if (null != sActivity) {
            sActivity.runOnUiThread(new Runnable() {
                public void run() {
                    if (!sActivity.isFinishing()) {
                        mProgressDialog = ProgressDialog.show(sActivity, "", "Processing...", true, false);
                        mProgressDialog.show();
                    }
                }
            });
        }
    }

    private void dismissProgressDialog() {
        if (null != sActivity) {
            sActivity.runOnUiThread(new Runnable() {
                public void run() {
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }
                }
            });
        }
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float not_use, int quality) {
        if(bitmap == null)
            return null;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w < h) {
            w = h;
            h = bitmap.getWidth();
        }
        if(w <= MIN_WIDTH || h <= MIN_HEIGHT)
            return bitmap;
        if(ImageTrueScanner.getCordsUIState()) {
            return bitmap;
        }
        if(!acquireLock(true)) {
            showToast("Still processing the previous request... ", Toast.LENGTH_LONG);
            return bitmap;
        }
        new Throwable().printStackTrace();
        int[] pts = ImageTrueScanner.getDeterminedPoints();
        int[] resultPts = new int[POINTS_LEN];
        float xScale = ((float)bitmap.getWidth())/pts[POINTS_LEN];
        float yScale = ((float)bitmap.getHeight())/pts[POINTS_LEN+1];
        for(int i=0; i < POINTS_LEN; i++) {
            if(i%2 == 0)
                resultPts[i] = (int)((pts[i] - pts[POINTS_LEN+2])*xScale);
            else
                resultPts[i] = (int)((pts[i] - pts[POINTS_LEN+3])*yScale);
        }
        if (rotating && rectifiedImage != null) {
            acquireLock(false);
            rotating = false;
            return rectifiedImage;
        }
        rectifiedImage = Bitmap.createBitmap(
                bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        int[] outputSize = processImage(bitmap, rectifiedImage, resultPts);
        rectifiedImage = Bitmap.createBitmap(
                rectifiedImage, 0, 0, outputSize[0], outputSize[1]);
        if(ImageTrueScanner.getRemoveGlareButtonStatus()) {
            Bitmap enhancedImage = Bitmap.createBitmap(
                    outputSize[0], outputSize[1], Bitmap.Config.ARGB_8888);
            showProgressDialog();
            enhanceImage(rectifiedImage, enhancedImage);
            dismissProgressDialog();
            rectifiedImage = enhancedImage;
        }
        acquireLock(false);

        return rectifiedImage;
    }

    static {
        try {
            System.loadLibrary("jni_truescanner_v2");
            isTrueScannerEnabled = true;
        } catch(UnsatisfiedLinkError e) {
            isTrueScannerEnabled = false;
        }
    }
}
