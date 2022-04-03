/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.filtershow.filters;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.renderscript.Allocation;
import android.widget.Toast;

import com.android.gallery3d.app.GalleryAppImpl;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;
import com.android.gallery3d.filtershow.imageshow.PrimaryImage;
import com.android.gallery3d.filtershow.pipeline.FilterEnvironment;

public abstract class ImageFilter implements Cloneable {
    private FilterEnvironment mEnvironment = null;

    protected String mName = "Original";
    private final String LOGTAG = "ImageFilter";
    protected static final boolean SIMPLE_ICONS = true;
    // TODO: Temporary, for dogfood note memory issues with toasts for better
    // feedback. Remove this when filters actually work in low memory
    // situations.
    protected static Activity sActivity = null;
    private static Toast sToast = null;
    private String lastMsg;

    public static void setActivityForMemoryToasts(Activity activity) {
        sActivity = activity;
    }

    public static void resetStatics() {
        sActivity = null;
        if (null != sToast) {
            sToast.cancel();
        }
        sToast = null;
    }

    void showToast(String msg, int duration) {
        if (sActivity != null) {
            sActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (sActivity != null && (!msg.equals(lastMsg) || sToast == null)) {
                        sToast = Toast.makeText(GalleryAppImpl.getContext(), msg, duration);
                        lastMsg = msg;
                    }
                    if (sToast != null) {
                        sToast.show();
                    }
                }
            });
        }
    }

    void showToast(int resID, int duration) {
        showToast(GalleryAppImpl.getContext().getString(resID), duration);
    }

    public void freeResources() {}

    public void displayLowMemoryToast() {
        showToast("Memory too low for filter " + getName() +
                ", please file a bug report", Toast.LENGTH_SHORT);
    }

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public boolean supportsAllocationInput() { return false; }

    public void apply(Allocation in, Allocation out) {
        setGeneralParameters();
    }

    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        // do nothing here, subclasses will implement filtering here
        setGeneralParameters();
        return bitmap;
    }

    public abstract void useRepresentation(FilterRepresentation representation);

    native protected void nativeApplyGradientFilter(Bitmap bitmap, int w, int h,
            int[] redGradient, int[] greenGradient, int[] blueGradient);

    public FilterRepresentation getDefaultRepresentation() {
        return null;
    }

    protected Matrix getOriginalToScreenMatrix(int w, int h) {
        return GeometryMathUtils.getImageToScreenMatrix(getEnvironment().getImagePreset()
                .getGeometryFilters(), true, PrimaryImage.getImage().getOriginalBounds(), w, h);
    }

    public void setEnvironment(FilterEnvironment environment) {
        mEnvironment = environment;
    }

    public FilterEnvironment getEnvironment() {
        return mEnvironment;
    }

    public void setGeneralParameters() {
        // should implement in subclass which like to transport
        // some information to other filters. (like the style setting from RetroLux
        // and Film to FixedFrame)
        mEnvironment.clearGeneralParameters();
    }
}
