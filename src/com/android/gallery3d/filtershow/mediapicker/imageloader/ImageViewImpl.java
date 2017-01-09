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

package com.android.gallery3d.filtershow.mediapicker.imageloader;

import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;


public class ImageViewImpl{

    private final static String TAG = "ImageViewImpl";
    protected Reference<View> viewRef;


    public ImageViewImpl(ImageView imageView) {
        if (imageView == null) throw new IllegalArgumentException("view must not be null");

        this.viewRef = new WeakReference<View>(imageView);
    }

    public int getWidth() {
        int width = 0;
        View view = viewRef.get();
        if (view != null) {
            final ViewGroup.LayoutParams params = view.getLayoutParams();

            if (width <= 0 && params != null) width = params.width; // Get layout width parameter
        }
        if (width <= 0) {
            ImageView imageView = (ImageView) viewRef.get();
            if (imageView != null) {
                width = getImageViewFieldValue(imageView, "mMaxWidth"); // Check maxWidth parameter
            }
        }
        return width;
    }

    public int getHeight() {
        int height = 0;
        View view = viewRef.get();
        if (view != null) {
            final ViewGroup.LayoutParams params = view.getLayoutParams();
            if (height <= 0 && params != null) height = params.height; // Get layout height parameter
            return height;
        }
        if (height <= 0) {
            ImageView imageView = (ImageView) viewRef.get();
            if (imageView != null) {
                height = getImageViewFieldValue(imageView, "mMaxHeight"); // Check maxHeight parameter
            }
        }
        return height;
    }

    public boolean setImageDrawable(Drawable drawable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            View view = viewRef.get();
            if (view != null) {
                ((ImageView) view).setImageDrawable(drawable);
                if (drawable instanceof AnimationDrawable) {
                    ((AnimationDrawable)drawable).start();
                }
                return true;
            }
        } else {
            Log.w(TAG, "Can't set a bitmap into view. You should call ImageLoader on UI thread for it.");
        }
        return false;
    }

    public int getId() {
        View view = viewRef.get();
        return view == null ? super.hashCode() : view.hashCode();
    }

    private static int getImageViewFieldValue(Object object, String fieldName) {
        int value = 0;
        try {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            int fieldValue = (Integer) field.get(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
                value = fieldValue;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return value;
    }

    public ViewScaleType getScaleType() {
        ImageView imageView = (ImageView) viewRef.get();
        if (imageView != null) {
            return ViewScaleType.fromImageView(imageView);
        }
        return ViewScaleType.CROP;
    }

    public boolean setImageBitmap(Bitmap bitmap) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            View view = viewRef.get();
            if (view != null) {
                ((ImageView) view).setImageBitmap(bitmap);
                return true;
            }
        } else {
            Log.w(TAG, "Can't set a bitmap into view. You should call ImageLoader on UI thread for it.");
        }
        return false;
    }

    public boolean isRecyled() {
        return viewRef.get() == null;
    }
}

